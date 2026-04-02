using HskLearn.Api.Models;

namespace HskLearn.Api.Services;

public class ProgressService
{
    private readonly VocabularyService _vocabularyService;
    private readonly List<PracticeAttempt> _attempts = [];
    private readonly object _lock = new();

    public ProgressService(VocabularyService vocabularyService)
    {
        _vocabularyService = vocabularyService;
    }

    public void RecordAttempt(string type, string itemId, double score,
        Dictionary<string, object>? details = null)
    {
        lock (_lock)
        {
            _attempts.Add(new PracticeAttempt(
                Type: type,
                ItemId: itemId,
                Score: score,
                Details: details,
                Timestamp: DateTime.UtcNow));
        }
    }

    public ProgressSummary GetSummary()
    {
        lock (_lock)
        {
            var reading = GetTaskSummary("reading");
            var speaking = GetTaskSummary("speaking");
            var writing = GetTaskSummary("writing");

            var uniqueItems = _attempts
                .Select(a => a.ItemId)
                .Distinct()
                .Count();

            var totalWords = _vocabularyService.GetTotalCount();

            return new ProgressSummary(
                TotalWordsLearned: uniqueItems,
                TotalWords: totalWords,
                Reading: reading,
                Speaking: speaking,
                Writing: writing,
                WeakWords: GetWeakWords(5));
        }
    }

    public List<WeakWord> GetWeakWords(int count)
    {
        lock (_lock)
        {
            if (_attempts.Count == 0)
                return [];

            // Group by itemId, compute average score, return lowest
            var wordScores = _attempts
                .GroupBy(a => a.ItemId)
                .Select(g => new
                {
                    ItemId = g.Key,
                    AverageScore = g.Average(a => a.Score),
                })
                .OrderBy(w => w.AverageScore)
                .Take(count)
                .ToList();

            return wordScores.Select(w =>
            {
                // Try to resolve word details from vocabulary
                var vocab = _vocabularyService.GetById(0); // won't match
                // Search by word text
                var matches = _vocabularyService.Search(w.ItemId);
                var match = matches.FirstOrDefault();

                return new WeakWord(
                    Word: match?.Word ?? w.ItemId,
                    Pinyin: match?.Pinyin ?? "",
                    AverageScore: Math.Round(w.AverageScore, 1));
            }).ToList();
        }
    }

    private TaskSummary GetTaskSummary(string type)
    {
        var ofType = _attempts.Where(a =>
            a.Type.Equals(type, StringComparison.OrdinalIgnoreCase)).ToList();

        if (ofType.Count == 0)
            return new TaskSummary(ItemsCompleted: 0, AverageScore: 0);

        var uniqueItems = ofType.Select(a => a.ItemId).Distinct().Count();
        var avg = Math.Round(ofType.Average(a => a.Score), 1);

        return new TaskSummary(ItemsCompleted: uniqueItems, AverageScore: avg);
    }
}
