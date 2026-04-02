using HskLearn.Api.Services;

namespace HskLearn.Api.Endpoints;

public static class VocabularyEndpoints
{
    public static RouteGroupBuilder MapVocabularyEndpoints(this RouteGroupBuilder group)
    {
        var vocab = group.MapGroup("/vocabulary").WithTags("Vocabulary");

        vocab.MapGet("/", (VocabularyService svc, int page = 1, int pageSize = 20,
            string? pos = null, int? level = null) =>
        {
            var words = svc.GetAll(page, pageSize, pos, level);
            var totalCount = svc.GetTotalCount(pos, level);

            return TypedResults.Ok(new
            {
                words,
                page,
                pageSize,
                totalCount,
            });
        })
        .WithName("GetVocabulary")
        .WithSummary("List HSK words (paged, filterable by part of speech and level)");

        vocab.MapGet("/random", (VocabularyService svc, int count = 5, int? level = null) =>
        {
            var words = svc.GetRandom(count, level);
            return TypedResults.Ok(words);
        })
        .WithName("GetRandomVocabulary")
        .WithSummary("Get N random HSK words");

        vocab.MapGet("/search", (VocabularyService svc, string q, int? level = null) =>
        {
            var words = svc.Search(q, level);
            return TypedResults.Ok(words);
        })
        .WithName("SearchVocabulary")
        .WithSummary("Search words by character, pinyin, or translation");

        vocab.MapGet("/levels", (VocabularyService svc) =>
        {
            return TypedResults.Ok(svc.GetAvailableLevels());
        })
        .WithName("GetVocabularyLevels")
        .WithSummary("Get available HSK levels");

        vocab.MapGet("/{id:int}", (VocabularyService svc, int id) =>
        {
            var word = svc.GetById(id);

            return word is not null
                ? TypedResults.Ok(word)
                : Results.Problem(
                    detail: $"Word with id {id} not found.",
                    statusCode: StatusCodes.Status404NotFound);
        })
        .WithName("GetWordById")
        .WithSummary("Get a single HSK word by id");

        return group;
    }
}
