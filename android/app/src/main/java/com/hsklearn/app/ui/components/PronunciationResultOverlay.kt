package com.hsklearn.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hsklearn.app.data.model.SpeechAssessmentResult
import com.hsklearn.app.data.model.WordResult

private val ScoreGreen = Color(0xFF4CAF50)
private val ScoreAmber = Color(0xFFFFA726)
private val ScoreRed = Color(0xFFE53935)

fun scoreColor(score: Double): Color = when {
    score >= 80 -> ScoreGreen
    score >= 60 -> ScoreAmber
    else -> ScoreRed
}

@Composable
fun PronunciationScoreRing(
    score: Double,
    modifier: Modifier = Modifier,
    size: Int = 100,
) {
    val color = scoreColor(score)

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size.dp)) {
        Canvas(modifier = Modifier.size(size.dp)) {
            val strokeWidth = 8.dp.toPx()
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = (score / 100.0 * 360).toFloat(),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toInt().toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "分 pts",
                fontSize = 12.sp,
                color = color.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
fun PronunciationScoreBar(
    label: String,
    score: Double,
    modifier: Modifier = Modifier,
) {
    val color = scoreColor(score)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${score.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (score / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
    }
}

@Composable
fun ColourCodedPassageText(
    passageText: String,
    words: List<WordResult>,
    modifier: Modifier = Modifier,
) {
    // Build a map of word -> accuracy from the results
    // The passage text is plain Chinese; words from the API are segmented
    val annotated = buildAnnotatedString {
        if (words.isEmpty()) {
            append(passageText)
            return@buildAnnotatedString
        }

        // Walk through the passage, matching word results sequentially
        var passageIndex = 0
        var wordIndex = 0

        while (passageIndex < passageText.length && wordIndex < words.size) {
            val word = words[wordIndex]
            val matchStart = passageText.indexOf(word.word, passageIndex)

            if (matchStart == passageIndex || (matchStart > passageIndex && matchStart - passageIndex <= 2)) {
                // Append any skipped characters (punctuation, etc.) in default colour
                if (matchStart > passageIndex) {
                    withStyle(SpanStyle(color = Color.White.copy(alpha = 0.9f))) {
                        append(passageText.substring(passageIndex, matchStart))
                    }
                }

                // Append the word with colour based on accuracy
                val color = scoreColor(word.accuracyScore)
                withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                    append(word.word)
                }
                passageIndex = matchStart + word.word.length
                wordIndex++
            } else {
                // No match found nearby — append current char and move on
                append(passageText[passageIndex])
                passageIndex++
            }
        }

        // Append remaining passage text
        if (passageIndex < passageText.length) {
            append(passageText.substring(passageIndex))
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 22.sp,
            lineHeight = 38.sp,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
fun PronunciationResultSummary(
    result: SpeechAssessmentResult,
    passageText: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PronunciationScoreRing(score = result.overallScore)

        Spacer(Modifier.height(12.dp))

        PronunciationScoreBar(label = "准确度 Accuracy", score = result.accuracyScore)
        Spacer(Modifier.height(8.dp))
        PronunciationScoreBar(label = "流利度 Fluency", score = result.fluencyScore)
        Spacer(Modifier.height(8.dp))
        PronunciationScoreBar(label = "完整度 Completeness", score = result.completenessScore)

        if (passageText != null && result.words.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            ColourCodedPassageText(
                passageText = passageText,
                words = result.words,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
