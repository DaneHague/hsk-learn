using System.Text.Json;
using HskLearn.Api.Services;

namespace HskLearn.Api.Endpoints;

public static class ReadingEndpoints
{
    public static RouteGroupBuilder MapReadingEndpoints(this RouteGroupBuilder group)
    {
        var reading = group.MapGroup("/reading").WithTags("Reading");

        reading.MapGet("/passage", (ReadingTaskService svc, string? topic = null, int level = 4) =>
        {
            var passage = topic is not null
                ? svc.GetPassageByTopic(topic, level)
                : svc.GetRandomPassage(level);

            return passage is not null
                ? TypedResults.Ok(passage)
                : Results.Problem(
                    detail: $"No reading passages available for HSK {level}.",
                    statusCode: StatusCodes.Status404NotFound);
        })
        .WithName("GetReadingPassage")
        .WithSummary("Get a static reading passage");

        reading.MapGet("/passage/{id:int}", (ReadingTaskService svc, int id) =>
        {
            var passage = svc.GetPassageById(id);
            return passage is not null
                ? TypedResults.Ok(passage)
                : Results.Problem(detail: $"Passage {id} not found.", statusCode: StatusCodes.Status404NotFound);
        })
        .WithName("GetReadingPassageById")
        .WithSummary("Get a specific reading passage by id");

        reading.MapGet("/topics", (ReadingTaskService svc, int level = 4) =>
            TypedResults.Ok(svc.GetTopics(level)))
        .WithName("GetReadingTopics");

        reading.MapGet("/levels", (ReadingTaskService svc) =>
            TypedResults.Ok(svc.GetAvailableLevels()))
        .WithName("GetReadingLevels");

        // --- AI-generated reading ---

        reading.MapPost("/generate", async (ReadingTaskService svc, HttpRequest request) =>
        {
            int level = 4;
            if (request.HasJsonContentType())
            {
                using var doc = await JsonDocument.ParseAsync(request.Body);
                if (doc.RootElement.TryGetProperty("level", out var lv))
                    level = lv.GetInt32();
            }
            else if (request.Query.ContainsKey("level"))
            {
                int.TryParse(request.Query["level"], out level);
            }

            try
            {
                var passage = await svc.GeneratePassage(level);
                return TypedResults.Ok(passage);
            }
            catch (Exception ex)
            {
                return Results.Problem(
                    detail: $"Failed to generate passage: {ex.Message}",
                    statusCode: StatusCodes.Status502BadGateway);
            }
        })
        .WithName("GeneratePassage")
        .WithSummary("AI-generate a reading passage at the given HSK level")
        .DisableAntiforgery();

        reading.MapPost("/translate", async (ReadingTaskService svc, HttpRequest request) =>
        {
            if (!request.HasJsonContentType())
                return Results.Problem(detail: "JSON body required.", statusCode: StatusCodes.Status400BadRequest);

            using var doc = await JsonDocument.ParseAsync(request.Body);
            var root = doc.RootElement;
            var passage = root.TryGetProperty("passage", out var p) ? p.GetString() ?? "" : "";
            var pinyin = root.TryGetProperty("passagePinyin", out var py) ? py.GetString() ?? "" : "";
            var topic = root.TryGetProperty("topic", out var t) ? t.GetString() ?? "" : "";

            if (string.IsNullOrWhiteSpace(passage))
                return Results.Problem(detail: "passage is required.", statusCode: StatusCodes.Status400BadRequest);

            try
            {
                var result = await svc.TranslatePassage(passage, pinyin, topic);
                return TypedResults.Ok(result);
            }
            catch (Exception ex)
            {
                return Results.Problem(
                    detail: $"Translation failed: {ex.Message}",
                    statusCode: StatusCodes.Status502BadGateway);
            }
        })
        .WithName("TranslatePassage")
        .WithSummary("Get English translation for a Chinese passage")
        .DisableAntiforgery();

        // --- Spoken Q&A ---

        reading.MapGet("/question", (ReadingTaskService svc, int level = 4) =>
        {
            var question = svc.GetRandomQuestion(level);
            return question is not null
                ? TypedResults.Ok(question)
                : Results.Problem(
                    detail: $"No questions available for HSK {level}.",
                    statusCode: StatusCodes.Status404NotFound);
        })
        .WithName("GetDiscussionQuestion")
        .WithSummary("Get a random discussion question for spoken Q&A");

        reading.MapPost("/evaluate-answer", async (ReadingTaskService svc, HttpRequest request) =>
        {
            if (!request.HasJsonContentType())
                return Results.Problem(detail: "JSON body required.", statusCode: StatusCodes.Status400BadRequest);

            using var doc = await JsonDocument.ParseAsync(request.Body);
            var root = doc.RootElement;
            var question = root.TryGetProperty("question", out var q) ? q.GetString() ?? "" : "";
            var recognisedText = root.TryGetProperty("recognisedText", out var rt) ? rt.GetString() ?? "" : "";
            var level = root.TryGetProperty("level", out var lv) ? lv.GetInt32() : 4;

            if (string.IsNullOrWhiteSpace(question) || string.IsNullOrWhiteSpace(recognisedText))
                return Results.Problem(detail: "question and recognisedText are required.", statusCode: StatusCodes.Status400BadRequest);

            try
            {
                var result = await svc.EvaluateSpokenAnswer(question, recognisedText, level);
                return TypedResults.Ok(result);
            }
            catch (Exception ex)
            {
                return Results.Problem(
                    detail: $"Evaluation failed: {ex.Message}",
                    statusCode: StatusCodes.Status502BadGateway);
            }
        })
        .WithName("EvaluateSpokenAnswer")
        .WithSummary("Evaluate a spoken answer to a discussion question using AI")
        .DisableAntiforgery();

        return group;
    }
}
