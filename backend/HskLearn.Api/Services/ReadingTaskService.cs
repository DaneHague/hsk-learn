using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using HskLearn.Api.Models;
using Microsoft.Extensions.Options;

namespace HskLearn.Api.Services;

public class ReadingTaskService
{
    private readonly Dictionary<int, List<ReadingPassage>> _passagesByLevel = [];
    private readonly Dictionary<int, List<DiscussionQuestion>> _questionsByLevel = [];
    private readonly AzureAIFoundryOptions _aiOptions;
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly ILogger<ReadingTaskService> _logger;
    private static readonly TimeSpan RequestTimeout = TimeSpan.FromSeconds(45);
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true,
    };

    public ReadingTaskService(
        ILogger<ReadingTaskService> logger,
        IOptions<AzureAIFoundryOptions> aiOptions,
        IHttpClientFactory httpClientFactory)
    {
        _logger = logger;
        _aiOptions = aiOptions.Value;
        _httpClientFactory = httpClientFactory;

        for (int level = 1; level <= 4; level++)
        {
            LoadPassages(level);
            LoadQuestions(level);
        }
    }

    private void LoadPassages(int level)
    {
        var path = $"Data/reading_passages_hsk{level}.json";
        if (!File.Exists(path)) return;
        var json = File.ReadAllText(path);
        var passages = JsonSerializer.Deserialize<List<ReadingPassage>>(json, JsonOptions);
        if (passages is { Count: > 0 })
        {
            _passagesByLevel[level] = passages;
            _logger.LogInformation("Loaded {Count} reading passages for HSK {Level}", passages.Count, level);
        }
    }

    private void LoadQuestions(int level)
    {
        var path = $"Data/discussion_questions_hsk{level}.json";
        if (!File.Exists(path)) return;
        var json = File.ReadAllText(path);
        var questions = JsonSerializer.Deserialize<List<DiscussionQuestion>>(json, JsonOptions);
        if (questions is { Count: > 0 })
        {
            _questionsByLevel[level] = questions;
        }
    }

    // --- Static passage methods ---

    public ReadingPassage? GetRandomPassage(int level = 4)
    {
        var passages = GetPassagesForLevel(level);
        if (passages.Count == 0) return null;
        return passages[Random.Shared.Next(passages.Count)];
    }

    public ReadingPassage? GetPassageByTopic(string topic, int level = 4)
    {
        var matching = GetPassagesForLevel(level)
            .Where(p => p.Topic.Equals(topic, StringComparison.OrdinalIgnoreCase))
            .ToList();
        if (matching.Count == 0) return null;
        return matching[Random.Shared.Next(matching.Count)];
    }

    public ReadingPassage? GetPassageById(int id, int? level = null)
    {
        if (level.HasValue)
            return GetPassagesForLevel(level.Value).FirstOrDefault(p => p.Id == id);
        return _passagesByLevel.Values.SelectMany(p => p).FirstOrDefault(p => p.Id == id);
    }

    public List<string> GetTopics(int level = 4)
    {
        return GetPassagesForLevel(level).Select(p => p.Topic).Distinct().ToList();
    }

    public List<int> GetAvailableLevels()
    {
        return _passagesByLevel.Keys.OrderBy(k => k).ToList();
    }

    private List<ReadingPassage> GetPassagesForLevel(int level)
    {
        return _passagesByLevel.TryGetValue(level, out var passages) ? passages : [];
    }

    // --- AI-generated passage ---

    public async Task<GeneratedPassage> GeneratePassage(int level)
    {
        var prompt = "Generate a short Chinese reading passage (3-5 sentences) appropriate for HSK level " + level + " students. " +
            "Use only grammar and vocabulary from HSK level " + level + " or below. " +
            "Choose a random everyday topic. " +
            "Respond ONLY in valid JSON, no markdown fences: " +
            "{\"passage\": \"<Chinese passage>\", \"passagePinyin\": \"<full pinyin with tone mark diacritics, e.g. Wǒ hěn xǐhuan chī zhōngguó cài.>\", \"topic\": \"<topic in Chinese>\"} " +
            "The pinyin MUST use tone mark diacritics (ā á ǎ à, ē é ě è, etc.), NOT tone numbers.";

        var content = await CallAI(prompt);
        return ParseJson<GeneratedPassage>(content, new GeneratedPassage(
            Passage: "无法生成。Unable to generate.",
            PassagePinyin: "",
            Topic: ""));
    }

    public async Task<PassageTranslation> TranslatePassage(string passage, string passagePinyin, string topic)
    {
        var prompt = "Translate the following Chinese passage into natural English. " +
            "Respond ONLY in valid JSON, no markdown fences: " +
            "{\"passage\": \"" + EscapeJson(passage) + "\", \"passagePinyin\": \"" + EscapeJson(passagePinyin) +
            "\", \"translation\": \"<English translation>\", \"topic\": \"" + EscapeJson(topic) + "\"} " +
            "Chinese passage: " + passage;

        var content = await CallAI(prompt);
        return ParseJson<PassageTranslation>(content, new PassageTranslation(
            Passage: passage,
            PassagePinyin: passagePinyin,
            Translation: "Unable to translate.",
            Topic: topic));
    }

    // --- Discussion questions ---

    public DiscussionQuestion? GetRandomQuestion(int level = 4)
    {
        if (_questionsByLevel.TryGetValue(level, out var questions) && questions.Count > 0)
            return questions[Random.Shared.Next(questions.Count)];
        return null;
    }

    public async Task<SpokenAnswerEvaluation> EvaluateSpokenAnswer(
        string question, string recognisedText, int level)
    {
        var prompt = "You are a Chinese language teacher evaluating a student's spoken answer. " +
            "The student is at HSK level " + level + ". Evaluate for grammar, content relevance, and overall quality. " +
            "Give all feedback in BOTH Chinese and English (Chinese first, then English). " +
            "Respond ONLY in valid JSON, no markdown fences: " +
            "{\"overallScore\": <0-100>, \"recognisedText\": \"" + EscapeJson(recognisedText) +
            "\", \"grammar\": \"<bilingual grammar feedback>\", \"content\": \"<bilingual content feedback>\"," +
            " \"pronunciation\": \"<bilingual pronunciation note>\", \"suggestions\": [\"<bilingual tip>\"]," +
            " \"encouragement\": \"<bilingual encouragement>\"} " +
            "Question: " + question + " Student's answer: " + recognisedText;

        var content = await CallAI(prompt);
        return ParseJson<SpokenAnswerEvaluation>(content, new SpokenAnswerEvaluation(
            OverallScore: 50,
            RecognisedText: recognisedText,
            Grammar: "无法评估。Unable to evaluate.",
            Content: "",
            Pronunciation: "",
            Suggestions: [],
            Encouragement: "继续加油！Keep practicing!"));
    }

    // --- AI helper ---

    private async Task<string> CallAI(string userPrompt)
    {
        var endpoint = _aiOptions.Endpoint.TrimEnd('/');
        string url;
        bool isFoundry;

        if (endpoint.Contains(".services.ai.azure.com"))
        {
            url = $"{endpoint}/openai/v1/chat/completions";
            isFoundry = true;
        }
        else
        {
            url = $"{endpoint}/openai/deployments/{_aiOptions.DeploymentName}" +
                  "/chat/completions?api-version=2024-12-01-preview";
            isFoundry = false;
        }

        var messages = new object[]
        {
            new { role = "user", content = userPrompt },
        };

        object requestBody = isFoundry
            ? new { model = _aiOptions.DeploymentName, messages, max_completion_tokens = 600, temperature = 0.7 }
            : new { messages, max_completion_tokens = 600, temperature = 0.7 };

        var client = _httpClientFactory.CreateClient();
        client.Timeout = RequestTimeout;

        using var request = new HttpRequestMessage(HttpMethod.Post, url);
        request.Headers.Add("api-key", _aiOptions.ApiKey);
        request.Content = new StringContent(
            JsonSerializer.Serialize(requestBody),
            Encoding.UTF8,
            new MediaTypeHeaderValue("application/json"));

        using var response = await client.SendAsync(request);
        if (!response.IsSuccessStatusCode)
        {
            var errorBody = await response.Content.ReadAsStringAsync();
            _logger.LogError("AI Foundry returned {Status}: {Body}", response.StatusCode, errorBody);
            throw new HttpRequestException($"AI service returned {(int)response.StatusCode}");
        }

        var responseJson = await response.Content.ReadAsStringAsync();
        using var doc = JsonDocument.Parse(responseJson);
        var content = doc.RootElement
            .GetProperty("choices")[0]
            .GetProperty("message")
            .GetProperty("content")
            .GetString() ?? "";

        // Strip markdown fences
        content = content.Trim();
        if (content.StartsWith("```"))
        {
            var nl = content.IndexOf('\n');
            if (nl >= 0) content = content[(nl + 1)..];
            if (content.EndsWith("```")) content = content[..^3];
            content = content.Trim();
        }

        return content;
    }

    private T ParseJson<T>(string json, T fallback)
    {
        try
        {
            return JsonSerializer.Deserialize<T>(json, JsonOptions) ?? fallback;
        }
        catch (JsonException ex)
        {
            _logger.LogWarning(ex, "Failed to parse AI JSON: {Content}", json);
            return fallback;
        }
    }

    private static string EscapeJson(string s) => s.Replace("\\", "\\\\").Replace("\"", "\\\"").Replace("\n", "\\n");
}
