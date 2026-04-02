namespace HskLearn.Api.Models;

public record GeneratedPassage(
    string Passage,
    string PassagePinyin,
    string Topic
);

public record PassageTranslation(
    string Passage,
    string PassagePinyin,
    string Translation,
    string Topic
);

public record DiscussionQuestion(
    string QuestionChinese,
    string QuestionPinyin,
    string QuestionEnglish,
    string Topic
);

public record SpokenAnswerEvaluation(
    int OverallScore,
    string RecognisedText,
    string Grammar,
    string Content,
    string Pronunciation,
    List<string> Suggestions,
    string Encouragement
);
