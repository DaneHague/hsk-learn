namespace HskLearn.Api.Models;

public record WritingEvaluationResult(
    int OverallScore,
    string StrokeOrder,
    string Proportion,
    string Similarity,
    string RecognisedMeaning,
    List<string> Suggestions,
    string Encouragement
);

public record CompositionEvaluationResult(
    int OverallScore,
    string Transcription,
    string Grammar,
    string Vocabulary,
    string Structure,
    string Content,
    List<string> Corrections,
    List<string> Suggestions,
    string Encouragement
);

public record WritingPrompt(
    string PromptChinese,
    string PromptEnglish,
    string Topic
);
