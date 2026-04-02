using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using HskLearn.Api.Models;
using Microsoft.Extensions.Options;

namespace HskLearn.Api.Services;

public class WritingEvaluationService
{
    private readonly AzureAIFoundryOptions _options;
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly ILogger<WritingEvaluationService> _logger;
    private static readonly TimeSpan RequestTimeout = TimeSpan.FromSeconds(30);

    private static string CompositionSystemPrompt(int level) =>
        "You are an expert Chinese language teacher evaluating a student's handwritten composition. The student is at HSK " + level + " level. They were given a topic prompt and wrote a response in Chinese by hand. First read and transcribe their handwriting, then evaluate the composition for grammar, vocabulary usage, structure, and content relevance. Evaluate based on HSK " + level + " standards — only expect vocabulary and grammar appropriate for this level. Give all feedback in BOTH Chinese and English (Chinese first, then English). Respond ONLY in valid JSON, no markdown fences, with this structure:\n" +
        """{"overallScore": <0-100>, "transcription": "<the Chinese text you read from the handwriting>", "grammar": "<bilingual comment on grammar accuracy>", "vocabulary": "<bilingual comment on vocabulary range and usage>", "structure": "<bilingual comment on essay structure and flow>", "content": "<bilingual comment on relevance to prompt and ideas>", "corrections": ["<specific correction in bilingual>", "<another correction>"], "suggestions": ["<bilingual improvement tip>", "<another tip>"], "encouragement": "<motivating message in Chinese and English>"}""" + "\n" +
        """Example field value: "语法使用基本正确，但有几处小错误。Grammar is mostly correct with a few minor errors.""" + "\"";

    private const string SystemPrompt =
        """
        You are an expert Chinese calligraphy and handwriting teacher evaluating a student's handwritten Chinese character. Evaluate the handwriting against the target character. Also provide the English meaning of the target character. Give all feedback in BOTH Chinese and English (Chinese first, then English). Respond ONLY in valid JSON, no markdown fences, with this structure:
        {"overallScore": <0-100>, "strokeOrder": "<bilingual comment on stroke order>", "proportion": "<bilingual comment on balance and structure>", "similarity": "<bilingual comment on how close to the target>", "recognisedMeaning": "<English meaning/translation of the character, e.g. 'love; to love'>", "suggestions": ["<bilingual tip1>", "<bilingual tip2>"], "encouragement": "<motivating message in Chinese and English>"}
        Example recognisedMeaning: "to accumulate; to build up"
        """;

    private const string TopicSystemPrompt =
        """
        You are a Chinese language teacher creating a writing prompt for a student. Generate a short, specific topic appropriate for their HSK level. The topic should encourage the student to write 3-5 sentences in Chinese. Give the prompt in both Chinese and English. Respond ONLY in valid JSON, no markdown fences, with this structure:
        {"promptChinese": "<the writing prompt in Chinese>", "promptEnglish": "<the same prompt in English>", "topic": "<1-3 word topic tag in English>"}
        Keep prompts practical and relatable (daily life, hobbies, school, travel, food, culture, etc.). Vary topics — do not repeat common ones like "self-introduction".
        """;

    public WritingEvaluationService(
        IOptions<AzureAIFoundryOptions> options,
        IHttpClientFactory httpClientFactory,
        ILogger<WritingEvaluationService> logger)
    {
        _options = options.Value;
        _httpClientFactory = httpClientFactory;
        _logger = logger;
    }

    public async Task<WritingEvaluationResult> EvaluateHandwriting(
        byte[] imageBytes, string targetCharacter, string pinyin)
    {
        var base64Image = Convert.ToBase64String(imageBytes);
        var dataUri = $"data:image/png;base64,{base64Image}";

        var endpoint = _options.Endpoint.TrimEnd('/');

        string url;
        if (endpoint.Contains(".services.ai.azure.com"))
        {
            // AI Foundry project-scoped endpoint
            url = $"{endpoint}/openai/v1/chat/completions";
        }
        else
        {
            // Classic Azure OpenAI endpoint
            url = $"{endpoint}/openai/deployments/{_options.DeploymentName}" +
                  "/chat/completions?api-version=2024-12-01-preview";
        }

        var isFoundry = !url.Contains("api-version");

        var messages = new object[]
        {
            new { role = "system", content = SystemPrompt },
            new
            {
                role = "user",
                content = new object[]
                {
                    new
                    {
                        type = "image_url",
                        image_url = new { url = dataUri, detail = "high" },
                    },
                    new
                    {
                        type = "text",
                        text = $"Target: {targetCharacter} ({pinyin}). Evaluate this handwriting attempt.",
                    },
                },
            },
        };

        // Foundry format needs model in body; classic format uses the deployment name in URL
        object requestBody = isFoundry
            ? new { model = _options.DeploymentName, messages, max_completion_tokens = 500, temperature = 0.3 }
            : new { messages, max_completion_tokens = 500, temperature = 0.3 };

        var client = _httpClientFactory.CreateClient();
        client.Timeout = RequestTimeout;

        using var request = new HttpRequestMessage(HttpMethod.Post, url);
        request.Headers.Add("api-key", _options.ApiKey);
        request.Content = new StringContent(
            JsonSerializer.Serialize(requestBody),
            Encoding.UTF8,
            new MediaTypeHeaderValue("application/json"));

        using var response = await client.SendAsync(request);

        if (!response.IsSuccessStatusCode)
        {
            var errorBody = await response.Content.ReadAsStringAsync();
            _logger.LogError("Azure AI Foundry returned {Status}: {Body}",
                response.StatusCode, errorBody);
            throw new HttpRequestException(
                $"AI evaluation service returned {(int)response.StatusCode}");
        }

        var responseJson = await response.Content.ReadAsStringAsync();
        return ParseResponse(responseJson);
    }

    public async Task<CompositionEvaluationResult> EvaluateComposition(
        byte[] imageBytes, string prompt, int level = 4)
    {
        var base64Image = Convert.ToBase64String(imageBytes);
        var dataUri = $"data:image/png;base64,{base64Image}";

        var endpoint = _options.Endpoint.TrimEnd('/');

        string url;
        if (endpoint.Contains(".services.ai.azure.com"))
        {
            url = $"{endpoint}/openai/v1/chat/completions";
        }
        else
        {
            url = $"{endpoint}/openai/deployments/{_options.DeploymentName}" +
                  "/chat/completions?api-version=2024-12-01-preview";
        }

        var isFoundry = !url.Contains("api-version");

        var messages = new object[]
        {
            new { role = "system", content = CompositionSystemPrompt(level) },
            new
            {
                role = "user",
                content = new object[]
                {
                    new
                    {
                        type = "image_url",
                        image_url = new { url = dataUri, detail = "high" },
                    },
                    new
                    {
                        type = "text",
                        text = $"Topic prompt: {prompt}\n\nRead the student's handwritten Chinese composition in the image and evaluate it.",
                    },
                },
            },
        };

        object requestBody = isFoundry
            ? new { model = _options.DeploymentName, messages, max_completion_tokens = 800, temperature = 0.3 }
            : new { messages, max_completion_tokens = 800, temperature = 0.3 };

        var client = _httpClientFactory.CreateClient();
        client.Timeout = RequestTimeout;

        using var request = new HttpRequestMessage(HttpMethod.Post, url);
        request.Headers.Add("api-key", _options.ApiKey);
        request.Content = new StringContent(
            JsonSerializer.Serialize(requestBody),
            Encoding.UTF8,
            new MediaTypeHeaderValue("application/json"));

        using var response = await client.SendAsync(request);

        if (!response.IsSuccessStatusCode)
        {
            var errorBody = await response.Content.ReadAsStringAsync();
            _logger.LogError("Azure AI Foundry returned {Status}: {Body}",
                response.StatusCode, errorBody);
            throw new HttpRequestException(
                $"AI evaluation service returned {(int)response.StatusCode}");
        }

        var responseJson = await response.Content.ReadAsStringAsync();
        return ParseCompositionResponse(responseJson);
    }

    public async Task<WritingPrompt> GenerateTopic(int level)
    {
        var endpoint = _options.Endpoint.TrimEnd('/');

        string url;
        if (endpoint.Contains(".services.ai.azure.com"))
        {
            url = $"{endpoint}/openai/v1/chat/completions";
        }
        else
        {
            url = $"{endpoint}/openai/deployments/{_options.DeploymentName}" +
                  "/chat/completions?api-version=2024-12-01-preview";
        }

        var isFoundry = !url.Contains("api-version");

        var messages = new object[]
        {
            new { role = "system", content = TopicSystemPrompt },
            new
            {
                role = "user",
                content = $"Generate a writing prompt for an HSK {level} student. Use vocabulary and grammar appropriate for HSK level {level}.",
            },
        };

        object requestBody = isFoundry
            ? new { model = _options.DeploymentName, messages, max_completion_tokens = 200, temperature = 0.9 }
            : new { messages, max_completion_tokens = 200, temperature = 0.9 };

        var client = _httpClientFactory.CreateClient();
        client.Timeout = RequestTimeout;

        using var request = new HttpRequestMessage(HttpMethod.Post, url);
        request.Headers.Add("api-key", _options.ApiKey);
        request.Content = new StringContent(
            JsonSerializer.Serialize(requestBody),
            Encoding.UTF8,
            new MediaTypeHeaderValue("application/json"));

        using var response = await client.SendAsync(request);

        if (!response.IsSuccessStatusCode)
        {
            var errorBody = await response.Content.ReadAsStringAsync();
            _logger.LogError("Azure AI Foundry returned {Status}: {Body}",
                response.StatusCode, errorBody);
            throw new HttpRequestException(
                $"AI service returned {(int)response.StatusCode}");
        }

        var responseJson = await response.Content.ReadAsStringAsync();
        return ParseTopicResponse(responseJson);
    }

    private WritingPrompt ParseTopicResponse(string responseJson)
    {
        using var doc = JsonDocument.Parse(responseJson);
        var root = doc.RootElement;

        var content = root
            .GetProperty("choices")[0]
            .GetProperty("message")
            .GetProperty("content")
            .GetString() ?? "";

        content = content.Trim();
        if (content.StartsWith("```"))
        {
            var firstNewline = content.IndexOf('\n');
            if (firstNewline >= 0) content = content[(firstNewline + 1)..];
            if (content.EndsWith("```"))
                content = content[..^3];
            content = content.Trim();
        }

        try
        {
            using var evalDoc = JsonDocument.Parse(content);
            var eval = evalDoc.RootElement;

            return new WritingPrompt(
                PromptChinese: eval.TryGetProperty("promptChinese", out var pc)
                    ? pc.GetString() ?? "" : "",
                PromptEnglish: eval.TryGetProperty("promptEnglish", out var pe)
                    ? pe.GetString() ?? "" : "",
                Topic: eval.TryGetProperty("topic", out var t)
                    ? t.GetString() ?? "" : ""
            );
        }
        catch (JsonException ex)
        {
            _logger.LogWarning(ex, "Failed to parse topic JSON: {Content}", content);
            return new WritingPrompt(
                PromptChinese: "请写一写你今天做了什么。",
                PromptEnglish: "Write about what you did today.",
                Topic: "daily life"
            );
        }
    }

    private CompositionEvaluationResult ParseCompositionResponse(string responseJson)
    {
        using var doc = JsonDocument.Parse(responseJson);
        var root = doc.RootElement;

        var content = root
            .GetProperty("choices")[0]
            .GetProperty("message")
            .GetProperty("content")
            .GetString() ?? "";

        content = content.Trim();
        if (content.StartsWith("```"))
        {
            var firstNewline = content.IndexOf('\n');
            if (firstNewline >= 0) content = content[(firstNewline + 1)..];
            if (content.EndsWith("```"))
                content = content[..^3];
            content = content.Trim();
        }

        try
        {
            using var evalDoc = JsonDocument.Parse(content);
            var eval = evalDoc.RootElement;

            List<string> GetStringList(string prop)
            {
                var list = new List<string>();
                if (eval.TryGetProperty(prop, out var arr))
                {
                    foreach (var s in arr.EnumerateArray())
                    {
                        var text = s.GetString();
                        if (!string.IsNullOrEmpty(text)) list.Add(text);
                    }
                }
                return list;
            }

            return new CompositionEvaluationResult(
                OverallScore: eval.TryGetProperty("overallScore", out var score)
                    ? score.GetInt32() : 50,
                Transcription: eval.TryGetProperty("transcription", out var tr)
                    ? tr.GetString() ?? "" : "",
                Grammar: eval.TryGetProperty("grammar", out var g)
                    ? g.GetString() ?? "" : "",
                Vocabulary: eval.TryGetProperty("vocabulary", out var v)
                    ? v.GetString() ?? "" : "",
                Structure: eval.TryGetProperty("structure", out var st)
                    ? st.GetString() ?? "" : "",
                Content: eval.TryGetProperty("content", out var c)
                    ? c.GetString() ?? "" : "",
                Corrections: GetStringList("corrections"),
                Suggestions: GetStringList("suggestions"),
                Encouragement: eval.TryGetProperty("encouragement", out var enc)
                    ? enc.GetString() ?? "" : ""
            );
        }
        catch (JsonException ex)
        {
            _logger.LogWarning(ex, "Failed to parse composition evaluation JSON: {Content}", content);
            return new CompositionEvaluationResult(
                OverallScore: 50,
                Transcription: "",
                Grammar: "无法评估。Unable to evaluate.",
                Vocabulary: "",
                Structure: "",
                Content: content,
                Corrections: [],
                Suggestions: ["请再试一次。Please try again."],
                Encouragement: "继续加油！Keep practicing!"
            );
        }
    }

    private WritingEvaluationResult ParseResponse(string responseJson)
    {
        using var doc = JsonDocument.Parse(responseJson);
        var root = doc.RootElement;

        var content = root
            .GetProperty("choices")[0]
            .GetProperty("message")
            .GetProperty("content")
            .GetString() ?? "";

        // Strip markdown fences if the model adds them despite instructions
        content = content.Trim();
        if (content.StartsWith("```"))
        {
            var firstNewline = content.IndexOf('\n');
            if (firstNewline >= 0) content = content[(firstNewline + 1)..];
            if (content.EndsWith("```"))
                content = content[..^3];
            content = content.Trim();
        }

        try
        {
            using var evalDoc = JsonDocument.Parse(content);
            var eval = evalDoc.RootElement;

            var suggestions = new List<string>();
            if (eval.TryGetProperty("suggestions", out var sugArr))
            {
                foreach (var s in sugArr.EnumerateArray())
                {
                    var text = s.GetString();
                    if (!string.IsNullOrEmpty(text))
                        suggestions.Add(text);
                }
            }

            return new WritingEvaluationResult(
                OverallScore: eval.TryGetProperty("overallScore", out var score)
                    ? score.GetInt32() : 50,
                StrokeOrder: eval.TryGetProperty("strokeOrder", out var so)
                    ? so.GetString() ?? "" : "",
                Proportion: eval.TryGetProperty("proportion", out var prop)
                    ? prop.GetString() ?? "" : "",
                Similarity: eval.TryGetProperty("similarity", out var sim)
                    ? sim.GetString() ?? "" : "",
                RecognisedMeaning: eval.TryGetProperty("recognisedMeaning", out var rm)
                    ? rm.GetString() ?? "" : "",
                Suggestions: suggestions,
                Encouragement: eval.TryGetProperty("encouragement", out var enc)
                    ? enc.GetString() ?? "" : ""
            );
        }
        catch (JsonException ex)
        {
            _logger.LogWarning(ex, "Failed to parse AI evaluation JSON: {Content}", content);
            return new WritingEvaluationResult(
                OverallScore: 50,
                StrokeOrder: "Unable to evaluate stroke order.",
                Proportion: "Unable to evaluate proportions.",
                Similarity: content,
                RecognisedMeaning: "",
                Suggestions: ["Try writing the character again with clearer strokes."],
                Encouragement: "继续加油！Keep practicing!"
            );
        }
    }
}
