using HskLearn.Api.Services;

namespace HskLearn.Api.Endpoints;

public static class WritingEndpoints
{

    public static RouteGroupBuilder MapWritingEndpoints(this RouteGroupBuilder group)
    {
        var writing = group.MapGroup("/writing").WithTags("Writing");

        writing.MapPost("/evaluate", async (HttpRequest request, WritingEvaluationService svc) =>
        {
            if (!request.HasFormContentType)
            {
                return Results.Problem(
                    detail: "Request must be multipart/form-data.",
                    statusCode: StatusCodes.Status400BadRequest);
            }

            var form = await request.ReadFormAsync();
            var imageFile = form.Files.GetFile("image");

            if (imageFile is null || imageFile.Length == 0)
            {
                return Results.Problem(
                    detail: "Image file is required and must not be empty.",
                    statusCode: StatusCodes.Status400BadRequest);
            }

            var targetCharacter = form.TryGetValue("targetCharacter", out var charValues)
                ? charValues.FirstOrDefault() ?? ""
                : "";

            var pinyin = form.TryGetValue("pinyin", out var pyValues)
                ? pyValues.FirstOrDefault() ?? ""
                : "";

            if (string.IsNullOrWhiteSpace(targetCharacter))
            {
                return Results.Problem(
                    detail: "targetCharacter is required.",
                    statusCode: StatusCodes.Status400BadRequest);
            }

            byte[] imageBytes;
            using (var ms = new MemoryStream())
            {
                await imageFile.CopyToAsync(ms);
                imageBytes = ms.ToArray();
            }

            try
            {
                var result = await svc.EvaluateHandwriting(imageBytes, targetCharacter, pinyin);
                return TypedResults.Ok(result);
            }
            catch (OperationCanceledException)
            {
                return Results.Problem(
                    detail: "Evaluation timed out. Please try again.",
                    statusCode: StatusCodes.Status504GatewayTimeout);
            }
            catch (Exception ex)
            {
                return Results.Problem(
                    detail: $"Evaluation service error: {ex.Message}",
                    statusCode: StatusCodes.Status502BadGateway);
            }
        })
        .WithName("EvaluateWriting")
        .WithSummary("Evaluate a handwritten Chinese character image")
        .DisableAntiforgery();

        writing.MapPost("/evaluate-composition", async (HttpRequest request, WritingEvaluationService svc) =>
        {
            if (!request.HasFormContentType)
            {
                return Results.Problem(
                    detail: "Request must be multipart/form-data.",
                    statusCode: StatusCodes.Status400BadRequest);
            }

            var form = await request.ReadFormAsync();
            var imageFile = form.Files.GetFile("image");

            if (imageFile is null || imageFile.Length == 0)
            {
                return Results.Problem(
                    detail: "Image file is required and must not be empty.",
                    statusCode: StatusCodes.Status400BadRequest);
            }

            var prompt = form.TryGetValue("prompt", out var pv) ? pv.FirstOrDefault() : null;
            var levelStr = form.TryGetValue("level", out var lv) ? lv.FirstOrDefault() : "4";
            var level = int.TryParse(levelStr, out var l) ? l : 4;

            if (string.IsNullOrWhiteSpace(prompt))
            {
                return Results.Problem(
                    detail: "prompt is required.",
                    statusCode: StatusCodes.Status400BadRequest);
            }

            byte[] imageBytes;
            using (var ms = new MemoryStream())
            {
                await imageFile.CopyToAsync(ms);
                imageBytes = ms.ToArray();
            }

            try
            {
                var result = await svc.EvaluateComposition(imageBytes, prompt, level);
                return TypedResults.Ok(result);
            }
            catch (OperationCanceledException)
            {
                return Results.Problem(
                    detail: "Evaluation timed out. Please try again.",
                    statusCode: StatusCodes.Status504GatewayTimeout);
            }
            catch (Exception ex)
            {
                return Results.Problem(
                    detail: $"Evaluation service error: {ex.Message}",
                    statusCode: StatusCodes.Status502BadGateway);
            }
        })
        .WithName("EvaluateComposition")
        .WithSummary("Evaluate a handwritten Chinese composition image on a given topic")
        .DisableAntiforgery();

        writing.MapGet("/prompts", async (int level, WritingEvaluationService svc) =>
        {
            try
            {
                var prompt = await svc.GenerateTopic(level);
                return TypedResults.Ok(prompt);
            }
            catch (OperationCanceledException)
            {
                return Results.Problem(
                    detail: "Topic generation timed out. Please try again.",
                    statusCode: StatusCodes.Status504GatewayTimeout);
            }
            catch (Exception ex)
            {
                return Results.Problem(
                    detail: $"Topic generation error: {ex.Message}",
                    statusCode: StatusCodes.Status502BadGateway);
            }
        })
        .WithName("GetWritingPrompt")
        .WithSummary("Generate an AI writing topic for a given HSK level");

        return group;
    }
}
