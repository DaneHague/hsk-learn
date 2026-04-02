namespace HskLearn.Api.Models;

public record HskWord(
    int Id,
    string Word,
    string Pinyin,
    string PartOfSpeech,
    string Translation,
    int HskLevel = 4,
    string? ExampleSentence = null,
    string? ExamplePinyin = null,
    string? ExampleTranslation = null
);
