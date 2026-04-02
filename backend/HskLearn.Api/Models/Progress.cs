namespace HskLearn.Api.Models;

public record PracticeAttempt(
    string Type,
    string ItemId,
    double Score,
    Dictionary<string, object>? Details,
    DateTime Timestamp
);

public record ProgressSummary(
    int TotalWordsLearned,
    int TotalWords,
    TaskSummary Reading,
    TaskSummary Speaking,
    TaskSummary Writing,
    List<WeakWord> WeakWords
);

public record TaskSummary(
    int ItemsCompleted,
    double AverageScore
);

public record WeakWord(
    string Word,
    string Pinyin,
    double AverageScore
);
