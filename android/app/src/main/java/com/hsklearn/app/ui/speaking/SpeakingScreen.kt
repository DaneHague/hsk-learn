package com.hsklearn.app.ui.speaking

import android.Manifest
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.hsklearn.app.data.model.SpeechAssessmentResult
import com.hsklearn.app.data.model.SpokenAnswerEvaluation
import com.hsklearn.app.data.model.WordResult
import com.hsklearn.app.ui.theme.GoldAccent

private val RecordRed = Color(0xFFE53935)
private val ScoreGreen = Color(0xFF4CAF50)
private val ScoreAmber = Color(0xFFFFA726)
private val ScoreRed = Color(0xFFE53935)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SpeakingScreen(viewModel: SpeakingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    Column(modifier = Modifier.fillMaxSize()) {
        val tabs = listOf("朗读 Read Aloud", "自由说 Free Talk")
        val selectedIndex = if (state.selectedTab == SpeakingTab.READ_ALOUD) 0 else 1

        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = GoldAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = GoldAccent,
                )
            },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = {
                        viewModel.selectTab(
                            if (index == 0) SpeakingTab.READ_ALOUD else SpeakingTab.FREE_TALK,
                        )
                    },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedIndex == index) GoldAccent
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    },
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.selectedTab == SpeakingTab.READ_ALOUD) {
                ReadAloudContent(state = state, onTogglePinyin = viewModel::togglePinyin)
            } else {
                FreeTalkContent(state = state)
            }

            Spacer(Modifier.height(24.dp))

            // Record button
            RecordButton(
                isRecording = state.isRecording,
                isLoading = state.isLoading,
                onTap = {
                    if (!micPermission.status.isGranted) {
                        micPermission.launchPermissionRequest()
                    } else if (state.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording()
                    }
                },
            )

            if (state.isLoading && !state.isRecording) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(color = GoldAccent, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "正在评估… Assessing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            state.error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }

            // Results
            state.result?.let { result ->
                Spacer(Modifier.height(24.dp))
                if (state.selectedTab == SpeakingTab.READ_ALOUD) {
                    ScriptedResults(
                        result = result,
                        onTryAgain = viewModel::clearResult,
                        onNext = { viewModel.clearResult(); viewModel.loadSentence() },
                    )
                } else {
                    FreeTalkResults(
                        speechResult = result,
                        aiResult = state.aiEvaluation,
                        onTryAgain = viewModel::clearResult,
                        onNext = { viewModel.clearResult(); viewModel.loadFreeQuestion() },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReadAloudContent(state: SpeakingUiState, onTogglePinyin: () -> Unit) {
    val sentence = state.sentence
    if (sentence == null) {
        if (!state.isLoading) {
            Text("未加载句子 No sentence loaded",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        return
    }

    Text(sentence.sentence,
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
        color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth())

    if (state.showPinyin) {
        Spacer(Modifier.height(8.dp))
        Text(sentence.pinyin, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }

    Spacer(Modifier.height(8.dp))
    Text(sentence.translation, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

    Spacer(Modifier.height(4.dp))
    TextButton(onClick = onTogglePinyin) {
        Text(if (state.showPinyin) "隐藏拼音 Hide Pinyin" else "显示拼音 Show Pinyin")
    }
}

@Composable
private fun FreeTalkContent(state: SpeakingUiState) {
    val question = state.freeQuestion
    if (question == null) {
        if (!state.isLoading) {
            Text("加载中… Loading...",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        return
    }

    // Question in Chinese (large)
    Text(question.questionChinese,
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

    // Pinyin
    if (question.questionPinyin.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(question.questionPinyin,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }

    // English
    Spacer(Modifier.height(8.dp))
    Text(question.questionEnglish,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

    Spacer(Modifier.height(8.dp))
    Text("用中文回答这个问题 Answer this question in Chinese",
        style = MaterialTheme.typography.bodySmall,
        color = GoldAccent.copy(alpha = 0.7f),
        textAlign = TextAlign.Center)
}

@Composable
private fun RecordButton(isRecording: Boolean, isLoading: Boolean, onTap: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulseScale")
    val scale = if (isRecording) pulseScale else 1f

    FloatingActionButton(
        onClick = onTap,
        modifier = Modifier.size(80.dp).scale(scale),
        shape = CircleShape,
        containerColor = if (isRecording) RecordRed else RecordRed.copy(alpha = 0.85f),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = if (isRecording) 8.dp else 4.dp),
    ) {
        if (isLoading && !isRecording) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
        } else {
            Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop" else "Record",
                tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}

// ==================== READ ALOUD RESULTS ====================

@Composable
private fun ScriptedResults(
    result: SpeechAssessmentResult,
    onTryAgain: () -> Unit,
    onNext: () -> Unit,
) {
    ScoreRing(score = result.overallScore)
    Spacer(Modifier.height(16.dp))
    ScoreBar(label = "准确度 Accuracy", score = result.accuracyScore)
    Spacer(Modifier.height(8.dp))
    ScoreBar(label = "流利度 Fluency", score = result.fluencyScore)
    Spacer(Modifier.height(8.dp))
    ScoreBar(label = "完整度 Completeness", score = result.completenessScore)

    if (result.words.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        WordResults(words = result.words)
    }

    Spacer(Modifier.height(16.dp))
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        OutlinedButton(onClick = onTryAgain) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp)); Text("再试一次 Try Again")
        }
        Button(onClick = onNext) {
            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp)); Text("下一句 Next")
        }
    }
}

// ==================== FREE TALK RESULTS ====================

@Composable
private fun FreeTalkResults(
    speechResult: SpeechAssessmentResult,
    aiResult: SpokenAnswerEvaluation?,
    onTryAgain: () -> Unit,
    onNext: () -> Unit,
) {
    // Show AI overall score if available, otherwise speech score
    val mainScore = aiResult?.overallScore?.toDouble() ?: speechResult.overallScore
    ScoreRing(score = mainScore)
    Spacer(Modifier.height(16.dp))

    // Recognised text
    if (speechResult.recognisedText.isNotBlank()) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("识别结果 What you said", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))
                Text(speechResult.recognisedText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // Pronunciation scores
    ScoreBar(label = "准确度 Accuracy", score = speechResult.accuracyScore)
    Spacer(Modifier.height(8.dp))
    ScoreBar(label = "流利度 Fluency", score = speechResult.fluencyScore)

    // AI evaluation feedback
    aiResult?.let { eval ->
        Spacer(Modifier.height(16.dp))
        Text("AI 评估 AI Evaluation", style = MaterialTheme.typography.titleLarge,
            color = GoldAccent)
        Spacer(Modifier.height(8.dp))

        FeedbackCard("语法 Grammar", eval.grammar)
        Spacer(Modifier.height(8.dp))
        FeedbackCard("内容 Content", eval.content)
        Spacer(Modifier.height(8.dp))
        FeedbackCard("发音 Pronunciation", eval.pronunciation)

        if (eval.suggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("建议 Suggestions", style = MaterialTheme.typography.labelLarge, color = GoldAccent)
                    Spacer(Modifier.height(4.dp))
                    eval.suggestions.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                }
            }
        }

        if (eval.encouragement.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(eval.encouragement, style = MaterialTheme.typography.bodyMedium,
                color = GoldAccent, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }

    Spacer(Modifier.height(16.dp))
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        OutlinedButton(onClick = onTryAgain) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp)); Text("再试一次 Try Again")
        }
        Button(onClick = onNext) {
            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp)); Text("下一题 Next")
        }
    }
}

// ==================== SHARED COMPOSABLES ====================

@Composable
private fun ScoreRing(score: Double) {
    val color = scoreColor(score)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val sw = 10.dp.toPx()
            val arcSize = Size(size.width - sw, size.height - sw)
            val tl = Offset(sw / 2, sw / 2)
            drawArc(color.copy(alpha = 0.15f), -90f, 360f, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(color, -90f, (score / 100.0 * 360).toFloat(), false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.toInt().toString(), style = MaterialTheme.typography.headlineLarge,
                color = color, fontWeight = FontWeight.Bold)
            Text("分 pts", style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun ScoreBar(label: String, score: Double) {
    val color = scoreColor(score)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("${score.toInt()}", style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (score / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color, trackColor = color.copy(alpha = 0.15f))
    }
}

@Composable
private fun WordResults(words: List<WordResult>) {
    var expandedWord by remember { mutableStateOf<WordResult?>(null) }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("逐字评分 Word Scores", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            words.chunked(6).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                    row.forEach { word ->
                        TextButton(onClick = { expandedWord = if (expandedWord == word) null else word }) {
                            Text(word.word, color = scoreColor(word.accuracyScore),
                                fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                }
            }
            expandedWord?.let { word ->
                Spacer(Modifier.height(8.dp))
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("${word.word} — ${word.accuracyScore.toInt()}分 pts (${word.errorType})",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        word.phonemes?.let { phonemes ->
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                phonemes.forEach { ph ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(ph.phoneme, fontSize = 14.sp, color = scoreColor(ph.accuracyScore), fontWeight = FontWeight.Bold)
                                        Text("${ph.accuracyScore.toInt()}", fontSize = 11.sp, color = scoreColor(ph.accuracyScore).copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackCard(title: String, content: String) {
    if (content.isBlank()) return
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = GoldAccent)
            Spacer(Modifier.height(2.dp))
            Text(content, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        }
    }
}

private fun scoreColor(score: Double): Color = when {
    score >= 80 -> ScoreGreen
    score >= 60 -> ScoreAmber
    else -> ScoreRed
}
