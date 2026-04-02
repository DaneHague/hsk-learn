using System.Text.Json;
using HskLearn.Api.Models;

namespace HskLearn.Api.Services;

public class VocabularyService
{
    private readonly List<HskWord> _words = [];
    private readonly ILogger<VocabularyService> _logger;
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true,
    };

    public VocabularyService(ILogger<VocabularyService> logger)
    {
        _logger = logger;

        for (int level = 1; level <= 4; level++)
        {
            var path = $"Data/hsk{level}_vocab.json";
            if (File.Exists(path))
            {
                var json = File.ReadAllText(path);
                var words = JsonSerializer.Deserialize<List<HskWord>>(json, JsonOptions);
                if (words is not null)
                {
                    // Ensure level is set correctly
                    var withLevel = words.Select(w => w with { HskLevel = level }).ToList();
                    _words.AddRange(withLevel);
                }
                _logger.LogInformation("Loaded {Count} words for HSK {Level}", words?.Count ?? 0, level);
            }
        }

        _logger.LogInformation("Total vocabulary: {Count} words across all levels", _words.Count);
    }

    public List<HskWord> GetAll(int page = 1, int pageSize = 20,
        string? partOfSpeech = null, int? level = null)
    {
        var query = FilterByLevel(_words, level);

        if (!string.IsNullOrWhiteSpace(partOfSpeech))
        {
            query = query.Where(w =>
                w.PartOfSpeech.Equals(partOfSpeech, StringComparison.OrdinalIgnoreCase));
        }

        return query
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToList();
    }

    public int GetTotalCount(string? partOfSpeech = null, int? level = null)
    {
        var query = FilterByLevel(_words, level);

        if (!string.IsNullOrWhiteSpace(partOfSpeech))
        {
            query = query.Where(w =>
                w.PartOfSpeech.Equals(partOfSpeech, StringComparison.OrdinalIgnoreCase));
        }

        return query.Count();
    }

    public List<HskWord> GetRandom(int count = 5, int? level = null)
    {
        return FilterByLevel(_words, level)
            .OrderBy(_ => Random.Shared.Next())
            .Take(count)
            .ToList();
    }

    public HskWord? GetById(int id)
    {
        return _words.FirstOrDefault(w => w.Id == id);
    }

    public List<HskWord> Search(string query, int? level = null)
    {
        if (string.IsNullOrWhiteSpace(query))
            return [];

        var q = query.Trim();

        return FilterByLevel(_words, level)
            .Where(w =>
                w.Word.Contains(q, StringComparison.OrdinalIgnoreCase) ||
                w.Pinyin.Contains(q, StringComparison.OrdinalIgnoreCase) ||
                w.Translation.Contains(q, StringComparison.OrdinalIgnoreCase))
            .ToList();
    }

    public List<int> GetAvailableLevels()
    {
        return _words.Select(w => w.HskLevel).Distinct().OrderBy(l => l).ToList();
    }

    private static IEnumerable<HskWord> FilterByLevel(List<HskWord> words, int? level)
    {
        return level.HasValue
            ? words.Where(w => w.HskLevel == level.Value)
            : words.AsEnumerable();
    }
}
