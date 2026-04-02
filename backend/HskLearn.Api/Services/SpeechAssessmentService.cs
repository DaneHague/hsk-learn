using System.Text.Json;
using HskLearn.Api.Models;
using Microsoft.CognitiveServices.Speech;
using Microsoft.CognitiveServices.Speech.Audio;
using Microsoft.CognitiveServices.Speech.PronunciationAssessment;
using Microsoft.Extensions.Options;

namespace HskLearn.Api.Services;

public class SpeechAssessmentService
{
    private readonly AzureSpeechOptions _options;
    private readonly ILogger<SpeechAssessmentService> _logger;
    private static readonly TimeSpan RecognitionTimeout = TimeSpan.FromSeconds(30);
    private static readonly TimeSpan PassageTimeout = TimeSpan.FromSeconds(120);

    public SpeechAssessmentService(
        IOptions<AzureSpeechOptions> options,
        ILogger<SpeechAssessmentService> logger)
    {
        _options = options.Value;
        _logger = logger;
    }

    public async Task<SpeechAssessmentResult> AssessScriptedReading(byte[] audioData, string referenceText)
    {
        var speechConfig = CreateSpeechConfig();
        using var audioConfig = CreateAudioConfig(audioData);

        var pronConfig = new PronunciationAssessmentConfig(
            referenceText: referenceText,
            gradingSystem: GradingSystem.HundredMark,
            granularity: Granularity.Phoneme,
            enableMiscue: true);
        pronConfig.PhonemeAlphabet = "SAPI";

        using var recognizer = new SpeechRecognizer(speechConfig, audioConfig);
        pronConfig.ApplyTo(recognizer);

        return await RecognizeAndParse(recognizer);
    }

    public async Task<SpeechAssessmentResult> AssessFreeSpeak(byte[] audioData)
    {
        var speechConfig = CreateSpeechConfig();
        using var audioConfig = CreateAudioConfig(audioData);

        var pronConfig = new PronunciationAssessmentConfig(
            referenceText: "",
            gradingSystem: GradingSystem.HundredMark,
            granularity: Granularity.Phoneme,
            enableMiscue: false);
        pronConfig.PhonemeAlphabet = "SAPI";

        using var recognizer = new SpeechRecognizer(speechConfig, audioConfig);
        pronConfig.ApplyTo(recognizer);

        return await RecognizeAndParse(recognizer);
    }

    public async Task<SpeechAssessmentResult> AssessPassage(byte[] audioData, string referenceText)
    {
        var speechConfig = CreateSpeechConfig();
        using var audioConfig = CreateAudioConfig(audioData);

        var pronConfig = new PronunciationAssessmentConfig(
            referenceText: referenceText,
            gradingSystem: GradingSystem.HundredMark,
            granularity: Granularity.Phoneme,
            enableMiscue: true);
        pronConfig.PhonemeAlphabet = "SAPI";

        using var recognizer = new SpeechRecognizer(speechConfig, audioConfig);
        pronConfig.ApplyTo(recognizer);

        var allWords = new List<WordResult>();
        var recognisedParts = new List<string>();
        double totalAccuracy = 0, totalFluency = 0, totalCompleteness = 0, totalPron = 0;
        int segmentCount = 0;

        var done = new TaskCompletionSource<bool>();
        using var cts = new CancellationTokenSource(PassageTimeout);
        cts.Token.Register(() => done.TrySetResult(false));

        recognizer.Recognized += (_, e) =>
        {
            if (e.Result.Reason == ResultReason.RecognizedSpeech)
            {
                recognisedParts.Add(e.Result.Text);

                var pronResult = PronunciationAssessmentResult.FromResult(e.Result);
                totalAccuracy += pronResult.AccuracyScore;
                totalFluency += pronResult.FluencyScore;
                totalCompleteness += pronResult.CompletenessScore;
                totalPron += pronResult.PronunciationScore;
                segmentCount++;

                var words = ParseWordResults(e.Result);
                allWords.AddRange(words);
            }
        };

        recognizer.Canceled += (_, e) =>
        {
            if (e.Reason == CancellationReason.EndOfStream)
            {
                done.TrySetResult(true);
            }
            else
            {
                _logger.LogError("Continuous recognition canceled: {Reason} — {Details}",
                    e.Reason, e.ErrorDetails);
                done.TrySetResult(false);
            }
        };

        recognizer.SessionStopped += (_, _) =>
        {
            done.TrySetResult(true);
        };

        await recognizer.StartContinuousRecognitionAsync();
        await done.Task;
        await recognizer.StopContinuousRecognitionAsync();

        if (segmentCount == 0)
        {
            return new SpeechAssessmentResult(
                OverallScore: 0, AccuracyScore: 0, FluencyScore: 0,
                CompletenessScore: 0, RecognisedText: "", Words: []);
        }

        return new SpeechAssessmentResult(
            OverallScore: totalPron / segmentCount,
            AccuracyScore: totalAccuracy / segmentCount,
            FluencyScore: totalFluency / segmentCount,
            CompletenessScore: totalCompleteness / segmentCount,
            RecognisedText: string.Join("", recognisedParts),
            Words: allWords);
    }

    public async Task<byte[]> SynthesizeSpeech(string text, string voice = "zh-CN-XiaoxiaoNeural")
    {
        var speechConfig = CreateSpeechConfig();
        speechConfig.SpeechSynthesisVoiceName = voice;

        using var synthesizer = new SpeechSynthesizer(speechConfig, null);
        using var cts = new CancellationTokenSource(RecognitionTimeout);

        var result = await synthesizer.SpeakTextAsync(text).WaitAsync(cts.Token);

        if (result.Reason == ResultReason.Canceled)
        {
            var details = SpeechSynthesisCancellationDetails.FromResult(result);
            _logger.LogError("Speech synthesis canceled: {Reason} — {Details}",
                details.Reason, details.ErrorDetails);
            throw new InvalidOperationException(
                $"Speech synthesis failed: {details.Reason}");
        }

        return result.AudioData;
    }

    private SpeechConfig CreateSpeechConfig()
    {
        var config = SpeechConfig.FromSubscription(_options.SubscriptionKey, _options.Region);
        config.SpeechRecognitionLanguage = "zh-CN";
        config.SetSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm);
        return config;
    }

    private static AudioConfig CreateAudioConfig(byte[] audioData)
    {
        var format = AudioStreamFormat.GetWaveFormatPCM(16000, 16, 1);
        var pushStream = AudioInputStream.CreatePushStream(format);

        int offset = FindPcmDataOffset(audioData);
        var pcmData = audioData.AsSpan(offset).ToArray();
        pushStream.Write(pcmData);
        pushStream.Close();

        return AudioConfig.FromStreamInput(pushStream);
    }

    private static int FindPcmDataOffset(byte[] wavData)
    {
        if (wavData.Length < 44)
            return 0;

        for (int i = 12; i < Math.Min(wavData.Length - 8, 200); i++)
        {
            if (wavData[i] == 'd' && wavData[i + 1] == 'a' &&
                wavData[i + 2] == 't' && wavData[i + 3] == 'a')
            {
                return i + 8;
            }
        }

        return 44;
    }

    private async Task<SpeechAssessmentResult> RecognizeAndParse(SpeechRecognizer recognizer)
    {
        using var cts = new CancellationTokenSource(RecognitionTimeout);

        var result = await recognizer.RecognizeOnceAsync().WaitAsync(cts.Token);

        if (result.Reason == ResultReason.Canceled)
        {
            var cancellation = CancellationDetails.FromResult(result);
            _logger.LogError("Speech recognition canceled: {Reason} — {Details}",
                cancellation.Reason, cancellation.ErrorDetails);
            throw new InvalidOperationException(
                $"Speech recognition failed: {cancellation.Reason}");
        }

        if (result.Reason == ResultReason.NoMatch)
        {
            _logger.LogWarning("No speech could be recognised from the audio");
            return new SpeechAssessmentResult(
                OverallScore: 0, AccuracyScore: 0, FluencyScore: 0,
                CompletenessScore: 0, RecognisedText: "", Words: []);
        }

        var pronResult = PronunciationAssessmentResult.FromResult(result);
        var words = ParseWordResults(result);

        return new SpeechAssessmentResult(
            OverallScore: pronResult.PronunciationScore,
            AccuracyScore: pronResult.AccuracyScore,
            FluencyScore: pronResult.FluencyScore,
            CompletenessScore: pronResult.CompletenessScore,
            RecognisedText: result.Text,
            Words: words);
    }

    private List<WordResult> ParseWordResults(SpeechRecognitionResult result)
    {
        var words = new List<WordResult>();

        try
        {
            var json = result.Properties.GetProperty(
                PropertyId.SpeechServiceResponse_JsonResult);

            if (string.IsNullOrEmpty(json))
                return words;

            using var doc = JsonDocument.Parse(json);
            var root = doc.RootElement;

            if (!root.TryGetProperty("NBest", out var nBest) ||
                nBest.GetArrayLength() == 0)
                return words;

            var best = nBest[0];
            if (!best.TryGetProperty("Words", out var wordsArray))
                return words;

            foreach (var wordElem in wordsArray.EnumerateArray())
            {
                var word = wordElem.GetProperty("Word").GetString() ?? "";
                double accuracy = 0;
                string errorType = "None";
                List<PhonemeResult>? phonemes = null;

                if (wordElem.TryGetProperty("PronunciationAssessment", out var pa))
                {
                    accuracy = pa.TryGetProperty("AccuracyScore", out var acc)
                        ? acc.GetDouble() : 0;
                    errorType = pa.TryGetProperty("ErrorType", out var err)
                        ? err.GetString() ?? "None" : "None";
                }

                if (wordElem.TryGetProperty("Phonemes", out var phonemesArray))
                {
                    phonemes = [];
                    foreach (var ph in phonemesArray.EnumerateArray())
                    {
                        var phoneme = ph.GetProperty("Phoneme").GetString() ?? "";
                        double phAccuracy = 0;
                        if (ph.TryGetProperty("PronunciationAssessment", out var phPa) &&
                            phPa.TryGetProperty("AccuracyScore", out var phAcc))
                        {
                            phAccuracy = phAcc.GetDouble();
                        }
                        phonemes.Add(new PhonemeResult(phoneme, phAccuracy));
                    }
                }

                words.Add(new WordResult(word, accuracy, errorType, phonemes));
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to parse word-level pronunciation results");
        }

        return words;
    }
}
