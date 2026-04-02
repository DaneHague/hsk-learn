using HskLearn.Api.Services;

namespace HskLearn.Api.Endpoints;

public static class ProgressEndpoints
{
    public static RouteGroupBuilder MapProgressEndpoints(this RouteGroupBuilder group)
    {
        var progress = group.MapGroup("/progress").WithTags("Progress");

        progress.MapPost("/record", (ProgressService svc, RecordRequest request) =>
        {
            if (string.IsNullOrWhiteSpace(request.Type) ||
                string.IsNullOrWhiteSpace(request.ItemId))
            {
                return Results.Problem(
                    detail: "type and itemId are required.",
                    statusCode: StatusCodes.Status400BadRequest);
            }

            svc.RecordAttempt(request.Type, request.ItemId, request.Score, request.Details);
            return TypedResults.Ok(new { recorded = true });
        })
        .WithName("RecordProgress")
        .WithSummary("Record a practice attempt");

        progress.MapGet("/summary", (ProgressService svc) =>
        {
            return TypedResults.Ok(svc.GetSummary());
        })
        .WithName("GetProgressSummary")
        .WithSummary("Get overall progress summary");

        progress.MapGet("/weak-words", (ProgressService svc, int count = 5) =>
        {
            return TypedResults.Ok(svc.GetWeakWords(count));
        })
        .WithName("GetWeakWords")
        .WithSummary("Get lowest-scoring words");

        return group;
    }
}

public record RecordRequest(
    string Type,
    string ItemId,
    double Score,
    Dictionary<string, object>? Details = null
);
