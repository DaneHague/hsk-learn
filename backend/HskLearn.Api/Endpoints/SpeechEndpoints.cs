using System.Text.Json;
using HskLearn.Api.Models;
using HskLearn.Api.Services;

namespace HskLearn.Api.Endpoints;

public static class SpeechEndpoints
{
    private static readonly Dictionary<int, List<PracticeSentence>> _sentencesByLevel = [];
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true,
    };

    public static RouteGroupBuilder MapSpeechEndpoints(this RouteGroupBuilder group)
    {
        var speech = group.MapGroup("/speech").WithTags("Speech");

        speech.MapPost("/assess", async (HttpRequest request, SpeechAssessmentService svc) =>
        {
            if (!request.HasFormContentType)
            {
                return Results.Problem(
                    detail: "Request must be multipart/form-data.",
                    statusCode: StatusCodes.Status400BadRequest);
            }

            var form = await request.ReadFormAsync();
            var audioFile = form.Files.GetFile("audio");

            if (audioFile is null || audioFile.Length == 0)
            {
                return Results.Problem(
                    detail: "Audio file is required and must not be empty.",
                    statusCode: StatusCodes.Status400BadRequest);
            }

            byte[] audioBytes;
            using (var ms = new MemoryStream())
            {
                await audioFile.CopyToAsync(ms);
                audioBytes = ms.ToArray();
            }

            var referenceText = form.TryGetValue("referenceText", out var refValues)
                ? refValues.FirstOrDefault()
                : null;

            try
            {
                var result = string.IsNullOrWhiteSpace(referenceText)
                    ? await svc.AssessFreeSpeak(audioBytes)
                    : await svc.AssessScriptedReading(audioBytes, referenceText);

                return TypedResults.Ok(result);
            }
            catch (OperationCanceledException)
            {
                return Results.Problem(
                    detail: "Speech assessment timed out. Please try again with a shorter recording.",
                    statusCode: StatusCodes.Status504GatewayTimeout);
            }
            catch (Exception ex)
            {
                return Results.Problem(
                    detail: $"Speech service error: {ex.Message}",
                    statusCode: StatusCodes.Status502BadGateway);
            }
        })
        .WithName("AssessSpeech")
        .WithSummary("Assess pronunciation from audio (scripted or free-speak)")
        .DisableAntiforgery();

        speech.MapPost("/assess-passage", async (HttpRequest request, SpeechAssessmentService svc) =>
        {
            if (!request.HasFormContentType)
            {
                return Results.Problem(
                    detail: "Request must be multipart/form-data.",
                    statusCode: StatusCodes.Status400BadRequest);
            }

            var form = await request.ReadFormAsync();
            var audioFile = form.Files.GetFile("audio");

            if (audioFile is null || audioFile.Length == 0)
            {
                return Results.Problem(
                    detail: "Audio file is required and must not be empty.",
                    statusCode: StatusCodes.Status400BadRequest);
            }

            var referenceText = form.TryGetValue("referenceText", out var refValues)
                ? refValues.FirstOrDefault() ?? ""
                : "";

            if (string.IsNullOrWhiteSpace(referenceText))
            {
                return Results.Problem(
                    detail: "referenceText is required for passage assessment.",
                    statusCode: StatusCodes.Status400BadRequest);
            }

            byte[] audioBytes;
            using (var ms = new MemoryStream())
            {
                await audioFile.CopyToAsync(ms);
                audioBytes = ms.ToArray();
            }

            try
            {
                var result = await svc.AssessPassage(audioBytes, referenceText);
                return TypedResults.Ok(result);
            }
            catch (OperationCanceledException)
            {
                return Results.Problem(
                    detail: "Passage assessment timed out.",
                    statusCode: StatusCodes.Status504GatewayTimeout);
            }
            catch (Exception ex)
            {
                return Results.Problem(
                    detail: $"Speech service error: {ex.Message}",
                    statusCode: StatusCodes.Status502BadGateway);
            }
        })
        .WithName("AssessPassage")
        .WithSummary("Assess pronunciation of a full reading passage (continuous mode)")
        .DisableAntiforgery();

        speech.MapPost("/synthesize", async (HttpRequest request, SpeechAssessmentService svc) =>
        {
            string? text = null;
            string voice = "zh-CN-XiaoxiaoNeural";

            if (request.HasFormContentType)
            {
                var form = await request.ReadFormAsync();
                text = form.TryGetValue("text", out var textValues)
                    ? textValues.FirstOrDefault() : null;
                if (form.TryGetValue("voice", out var voiceValues))
                {
                    var v = voiceValues.FirstOrDefault();
                    if (!string.IsNullOrWhiteSpace(v)) voice = v;
                }
            }
            else if (request.HasJsonContentType())
            {
                using var doc = await JsonDocument.ParseAsync(request.Body);
                text = doc.RootElement.TryGetProperty("text", out var t)
                    ? t.GetString() : null;
                if (doc.RootElement.TryGetProperty("voice", out var v))
                {
                    var val2 = v.GetString();
                    if (!string.IsNullOrWhiteSpace(val2)) voice = val2;
                }
            }

            if (string.IsNullOrWhiteSpace(text))
            {
                return Results.Problem(
                    detail: "text is required.",
                    statusCode: StatusCodes.Status400BadRequest);
            }

            try
            {
                var audioData = await svc.SynthesizeSpeech(text, voice);
                return Results.File(audioData, "audio/wav", "speech.wav");
            }
            catch (OperationCanceledException)
            {
                return Results.Problem(
                    detail: "Speech synthesis timed out.",
                    statusCode: StatusCodes.Status504GatewayTimeout);
            }
            catch (Exception ex)
            {
                return Results.Problem(
                    detail: $"Speech synthesis error: {ex.Message}",
                    statusCode: StatusCodes.Status502BadGateway);
            }
        })
        .WithName("SynthesizeSpeech")
        .WithSummary("Synthesize Chinese text to speech audio (WAV)")
        .DisableAntiforgery();

        speech.MapGet("/sentences", (int level = 4) =>
        {
            var sentences = LoadSentences(level);
            if (sentences.Count == 0)
            {
                return Results.Problem(
                    detail: $"No practice sentences available for HSK {level}.",
                    statusCode: StatusCodes.Status404NotFound);
            }

            var sentence = sentences[Random.Shared.Next(sentences.Count)];
            return TypedResults.Ok(sentence);
        })
        .WithName("GetPracticeSentence")
        .WithSummary("Get a random practice sentence for a given HSK level");

        return group;
    }

    private static List<PracticeSentence> LoadSentences(int level)
    {
        if (_sentencesByLevel.TryGetValue(level, out var cached)) return cached;

        var path = $"Data/practice_sentences_hsk{level}.json";
        if (!File.Exists(path))
        {
            _sentencesByLevel[level] = [];
            return _sentencesByLevel[level];
        }

        var json = File.ReadAllText(path);
        var sentences = JsonSerializer.Deserialize<List<PracticeSentence>>(json, JsonOptions) ?? [];
        _sentencesByLevel[level] = sentences;
        return sentences;
    }
}
