namespace HskLearn.Api.Models;

public record ReadingPassage(
    int Id,
    string Topic,
    string Title,
    string TitlePinyin,
    string Passage,
    string PassagePinyin,
    List<string> TargetWords,
    List<ComprehensionQuestion> Questions
);

public record ComprehensionQuestion(
    string Question,
    List<string> Options,
    int CorrectIndex,
    string Explanation
);
