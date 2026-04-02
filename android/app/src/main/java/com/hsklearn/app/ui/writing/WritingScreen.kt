package com.hsklearn.app.ui.writing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.hsklearn.app.data.model.CompositionEvaluationResult
import com.hsklearn.app.data.model.WritingPrompt
import com.hsklearn.app.ui.theme.GoldAccent

private val ScoreGreen = Color(0xFF4CAF50)
private val ScoreAmber = Color(0xFFFFA726)
private val ScoreRed = Color(0xFFE53935)

@Composable
fun WritingScreen(viewModel: WritingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var canvasView by remember { mutableStateOf<HandwritingCanvasView?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Fixed dark header with the topic prompt
        val prompt = state.prompt
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            if (prompt != null) {
                PromptCard(prompt = prompt)
            } else if (state.isLoading) {
                CircularProgressIndicator(
                    color = GoldAccent,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
            } else {
                Text(
                    text = "加载中… Loading...",
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
        }

        // Scrollable content below
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isLoading && prompt == null) return@Column

            // Handwriting canvas area
            if (state.compositionResult == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp),
                ) {
                    AndroidView(
                        factory = { context ->
                            HandwritingCanvasView(context).also { view ->
                                view.setLinedMode(true)
                                canvasView = view
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Canvas controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { canvasView?.undoLastStroke() },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(
                        onClick = { canvasView?.clearCanvas() },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = { canvasView?.let { viewModel.submitComposition(it.exportAsPng()) } },
                        enabled = !state.isLoading,
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("提交评价 Submit")
                    }
                }
            }

            // Error
            state.error?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            // Results
            state.compositionResult?.let { result ->
                Spacer(Modifier.height(16.dp))
                CompositionResults(
                    result = result,
                    onTryAgain = {
                        viewModel.clearResult()
                        canvasView?.clearCanvas()
                    },
                    onNext = {
                        viewModel.clearResult()
                        viewModel.loadPrompt()
                        canvasView?.clearCanvas()
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PromptCard(prompt: WritingPrompt) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = GoldAccent.copy(alpha = 0.25f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "写作题目 Topic",
                style = MaterialTheme.typography.labelLarge,
                color = GoldAccent,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = prompt.promptChinese,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = prompt.promptEnglish,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun CompositionResults(
    result: CompositionEvaluationResult,
    onTryAgain: () -> Unit,
    onNext: () -> Unit,
) {
    // Score ring
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ScoreRing(score = result.overallScore)
    }

    Spacer(Modifier.height(12.dp))

    // Transcription card
    if (result.transcription.isNotBlank()) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "识别内容 Transcription",
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldAccent,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = result.transcription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    // Feedback cards
    FeedbackCard(title = "语法 Grammar", content = result.grammar)
    Spacer(Modifier.height(8.dp))
    FeedbackCard(title = "词汇 Vocabulary", content = result.vocabulary)
    Spacer(Modifier.height(8.dp))
    FeedbackCard(title = "结构 Structure", content = result.structure)
    Spacer(Modifier.height(8.dp))
    FeedbackCard(title = "内容 Content", content = result.content)

    // Corrections
    if (result.corrections.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "纠正 Corrections",
                    style = MaterialTheme.typography.labelLarge,
                    color = ScoreRed,
                )
                Spacer(Modifier.height(4.dp))
                result.corrections.forEach { correction ->
                    Text(
                        text = "• $correction",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }

    // Suggestions
    if (result.suggestions.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "建议 Suggestions",
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldAccent,
                )
                Spacer(Modifier.height(4.dp))
                result.suggestions.forEach { suggestion ->
                    Text(
                        text = "• $suggestion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }

    // Encouragement
    if (result.encouragement.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = result.encouragement,
            style = MaterialTheme.typography.bodyMedium,
            color = GoldAccent,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Spacer(Modifier.height(12.dp))

    // Action buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        OutlinedButton(onClick = onTryAgain) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("再试 Retry")
        }
        Button(onClick = onNext) {
            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("下一题 Next")
        }
    }
}

// ==================== SHARED COMPOSABLES ====================

@Composable
private fun FeedbackCard(title: String, content: String) {
    if (content.isBlank()) return
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = GoldAccent)
            Spacer(Modifier.height(2.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ScoreRing(score: Int) {
    val color = when {
        score >= 80 -> ScoreGreen
        score >= 60 -> ScoreAmber
        else -> ScoreRed
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
        Canvas(modifier = Modifier.size(100.dp)) {
            val strokeWidth = 8.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            drawArc(
                color = color.copy(alpha = 0.15f), startAngle = -90f, sweepAngle = 360f,
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = color, startAngle = -90f, sweepAngle = (score / 100f * 360f),
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$score", style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Text(text = "分 pts", fontSize = 12.sp, color = color.copy(alpha = 0.7f))
        }
    }
}
