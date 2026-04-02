package com.hsklearn.app.ui.reading

import android.Manifest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.hsklearn.app.data.model.SpokenAnswerEvaluation
import com.hsklearn.app.ui.theme.GoldAccent

private val RecordRed = Color(0xFFE53935)
private val ScoreGreen = Color(0xFF4CAF50)
private val ScoreAmber = Color(0xFFFFA726)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ReadingScreen(viewModel: ReadingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        val tabs = listOf("AI阅读 AI Reading", "口头问答 Spoken Q&A", "阅读理解 Comprehension")
        val selectedIndex = state.selectedTab.ordinal

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
                    onClick = { viewModel.selectTab(ReadingTab.entries[index]) },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedIndex == index) GoldAccent
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                        )
                    },
                )
            }
        }

        when (state.selectedTab) {
            ReadingTab.AI_READING -> AiReadingTab(state, viewModel)
            ReadingTab.SPOKEN_QA -> SpokenQaTab(state, viewModel)
            ReadingTab.COMPREHENSION -> ComprehensionTab(state, viewModel)
        }
    }
}

// ==================== AI READING TAB ====================

@Composable
private fun AiReadingTab(state: ReadingUiState, viewModel: ReadingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isLoading && state.generatedPassage == null) {
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = GoldAccent)
            Spacer(Modifier.height(8.dp))
            Text("正在生成文章… Generating passage...",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            return
        }

        state.error?.let {
            ErrorText(it)
            Spacer(Modifier.height(8.dp))
        }

        state.generatedPassage?.let { gp ->
            // Topic
            Text(
                text = gp.topic,
                style = MaterialTheme.typography.titleLarge,
                color = GoldAccent,
            )

            Spacer(Modifier.height(16.dp))

            // Pinyin above passage
            Text(
                text = gp.passagePinyin,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 24.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(8.dp))

            // Chinese passage
            Text(
                text = gp.passage,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp, lineHeight = 40.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(20.dp))

            // Translation reveal
            if (!state.isTranslationRevealed) {
                Button(
                    onClick = viewModel::revealTranslation,
                    enabled = !state.isLoading,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("显示翻译 Reveal Translation")
                }
            } else {
                state.translation?.let { tr ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = GoldAccent.copy(alpha = 0.1f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "English Translation",
                                style = MaterialTheme.typography.labelLarge,
                                color = GoldAccent,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = tr.translation,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Next passage
            OutlinedButton(onClick = viewModel::generatePassage) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("下一篇 Next Passage")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ==================== SPOKEN Q&A TAB ====================

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun SpokenQaTab(state: ReadingUiState, viewModel: ReadingViewModel) {
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isLoading && state.question == null) {
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = GoldAccent)
            return
        }

        state.question?.let { q ->
            // Question card
            Card(
                colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = q.questionChinese,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = q.questionEnglish,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Record button
            RecordButton(
                isRecording = state.isRecording,
                isLoading = state.isAssessing,
                onTap = {
                    if (!micPermission.status.isGranted) {
                        micPermission.launchPermissionRequest()
                    } else if (state.isRecording) {
                        viewModel.stopQaRecording()
                    } else {
                        viewModel.startQaRecording()
                    }
                },
            )

            if (state.isAssessing) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator(color = GoldAccent, modifier = Modifier.size(28.dp))
                Text("正在评估… Assessing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                ErrorText(it)
            }

            // Results
            state.spokenResult?.let { result ->
                Spacer(Modifier.height(20.dp))
                SpokenQaResults(result = result)
            }

            Spacer(Modifier.height(16.dp))

            // Next question
            OutlinedButton(onClick = viewModel::loadQuestion) {
                Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("下一题 Next Question")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SpokenQaResults(result: SpokenAnswerEvaluation) {
    val color = when {
        result.overallScore >= 80 -> ScoreGreen
        result.overallScore >= 60 -> ScoreAmber
        else -> RecordRed
    }

    // Recognised text
    if (result.recognisedText.isNotBlank()) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("识别结果 What you said", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))
                Text(result.recognisedText, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // Score
    Text("${result.overallScore}分 pts",
        style = MaterialTheme.typography.headlineLarge, color = color, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))

    // Feedback cards
    FeedbackCard("语法 Grammar", result.grammar)
    Spacer(Modifier.height(8.dp))
    FeedbackCard("内容 Content", result.content)
    Spacer(Modifier.height(8.dp))
    FeedbackCard("发音 Pronunciation", result.pronunciation)

    if (result.suggestions.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("建议 Suggestions", style = MaterialTheme.typography.labelLarge, color = GoldAccent)
                Spacer(Modifier.height(4.dp))
                result.suggestions.forEach {
                    Text("• $it", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }
            }
        }
    }

    if (result.encouragement.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(result.encouragement, style = MaterialTheme.typography.bodyMedium,
            color = GoldAccent, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

// ==================== COMPREHENSION TAB ====================

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ComprehensionTab(state: ReadingUiState, viewModel: ReadingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (state.isLoading && state.passage == null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldAccent)
            }
            return
        }

        state.error?.let {
            ErrorText(it)
            Spacer(Modifier.height(8.dp))
        }

        val passage = state.passage ?: return

        // Title
        Text(passage.title, style = MaterialTheme.typography.headlineMedium, color = GoldAccent)
        if (state.showPinyin) {
            Text(passage.titlePinyin, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(16.dp))

        // Passage with optional pinyin
        if (state.showPinyin) {
            Text(passage.passagePinyin,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
        }
        Text(passage.passage,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp, lineHeight = 40.sp),
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::togglePinyin) {
                Text(if (state.showPinyin) "隐藏拼音 Hide Pinyin" else "显示拼音 Show Pinyin")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Questions
        Text("阅读理解 Comprehension", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))

        passage.questions.forEachIndexed { qIdx, question ->
            QuestionCard(qIdx, question, state.selectedAnswers[qIdx], state.isSubmitted) {
                viewModel.selectAnswer(qIdx, it)
            }
            Spacer(Modifier.height(12.dp))
        }

        if (state.isSubmitted) {
            ResultsCard(state.score, passage.questions.size)
            Spacer(Modifier.height(12.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!state.isSubmitted) {
                Button(onClick = viewModel::submitAnswers,
                    enabled = state.selectedAnswers.size == passage.questions.size,
                    modifier = Modifier.weight(1f)) {
                    Text("提交答案 Submit")
                }
            }
            if (state.isSubmitted) {
                Button(onClick = viewModel::loadPassage, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("下一篇 Next")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ==================== SHARED COMPOSABLES ====================

@Composable
private fun RecordButton(isRecording: Boolean, isLoading: Boolean, onTap: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulseScale",
    )
    FloatingActionButton(
        onClick = onTap,
        modifier = Modifier.size(80.dp).scale(if (isRecording) pulseScale else 1f),
        shape = CircleShape,
        containerColor = if (isRecording) RecordRed else RecordRed.copy(alpha = 0.85f),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = if (isRecording) 8.dp else 4.dp),
    ) {
        if (isLoading && !isRecording) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        } else {
            Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop" else "Record",
                tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun QuestionCard(
    index: Int, question: com.hsklearn.app.data.model.ComprehensionQuestion,
    selectedOption: Int?, isSubmitted: Boolean, onSelect: (Int) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${index + 1}. ${question.question}", style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            question.options.forEachIndexed { optIdx, option ->
                val isSelected = selectedOption == optIdx
                val isCorrect = optIdx == question.correctIndex
                val borderColor by animateColorAsState(when {
                    !isSubmitted && isSelected -> GoldAccent
                    isSubmitted && isCorrect -> ScoreGreen
                    isSubmitted && isSelected && !isCorrect -> RecordRed
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }, label = "border")
                val containerColor by animateColorAsState(when {
                    !isSubmitted && isSelected -> GoldAccent.copy(alpha = 0.1f)
                    isSubmitted && isCorrect -> ScoreGreen.copy(alpha = 0.1f)
                    isSubmitted && isSelected && !isCorrect -> RecordRed.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }, label = "container")
                OutlinedButton(
                    onClick = { onSelect(optIdx) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    border = BorderStroke(1.dp, borderColor),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = containerColor),
                    enabled = !isSubmitted,
                ) {
                    Text("${'A' + optIdx}. $option", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start)
                }
            }
            if (isSubmitted && selectedOption != null && selectedOption != question.correctIndex) {
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
                    Text(question.explanation, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultsCard(score: Int, total: Int) {
    val color = when {
        score >= total - 1 -> ScoreGreen
        score == 1 -> GoldAccent
        else -> RecordRed
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score / $total", style = MaterialTheme.typography.headlineLarge,
                color = color, fontWeight = FontWeight.Bold)
            Text(when {
                score == total -> "全对了！太棒了！Perfect score!"
                score >= total - 1 -> "很不错！继续加油！Great job, keep going!"
                score == 1 -> "再仔细看看文章吧。Read the passage more carefully."
                else -> "别灰心，多读几遍试试。Don't give up, try reading again."
            }, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
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

@Composable
private fun ErrorText(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth())
}
