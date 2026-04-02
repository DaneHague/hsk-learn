namespace HskLearn.Api.Models;

public record SpeechAssessmentResult(
    double OverallScore,
    double AccuracyScore,
    double FluencyScore,
    double CompletenessScore,
    string RecognisedText,
    List<WordResult> Words
);

public record WordResult(
    string Word,
    double AccuracyScore,
    string ErrorType,
    List<PhonemeResult>? Phonemes
);

public record PhonemeResult(
    string Phoneme,
    double AccuracyScore
);

public record PracticeSentence(
    string Sentence,
    string Pinyin,
    string Translation
);
