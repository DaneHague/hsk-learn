package com.hsklearn.app.ui.flashcard

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hsklearn.app.ui.theme.GoldAccent

private val CorrectGreen = Color(0xFF4CAF50)
private val WrongRed = Color(0xFFE53935)

@Composable
fun FlashcardScreen(viewModel: FlashcardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Stats bar
        StatsBar(
            dueCount = state.dueCount,
            learnedCount = state.learnedCount,
            totalCount = state.totalCount,
            sessionReviewed = state.sessionReviewed,
            sessionCorrect = state.sessionCorrect,
        )

        Spacer(Modifier.height(16.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldAccent)
            }
            return
        }

        state.error?.let { error ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = viewModel::loadCards) { Text("重试 Retry") }
                }
            }
            return
        }

        if (state.isDone) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                SessionCompleteCard(
                    sessionReviewed = state.sessionReviewed,
                    sessionCorrect = state.sessionCorrect,
                    onReload = viewModel::loadCards,
                )
            }
            return
        }

        val question = state.question
        if (question == null) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldAccent)
            }
            return
        }

        // Direction hint
        Text(
            text = if (question.showChinese) "选择正确的翻译 Choose the translation"
            else "选择正确的中文 Choose the Chinese",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )

        Spacer(Modifier.height(12.dp))

        // Question prompt card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (question.showChinese) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = question.card.word,
                            fontSize = 56.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = question.card.pinyin,
                            style = MaterialTheme.typography.titleLarge,
                            color = GoldAccent,
                        )
                    }
                } else {
                    Text(
                        text = question.prompt,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 4 choice buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            question.choices.forEachIndexed { index, choice ->
                ChoiceButton(
                    text = choice,
                    index = index,
                    correctIndex = question.correctIndex,
                    selectedAnswer = state.selectedAnswer,
                    onClick = { viewModel.selectAnswer(index) },
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // After answering: show result + next button
        if (state.hasAnswered) {
            // Show correct answer details if wrong
            if (!state.isCorrect) {
                Text(
                    text = "正确答案 Correct: ${question.card.word} (${question.card.pinyin}) — ${question.card.translation}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CorrectGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(onClick = viewModel::nextCard) {
                Text("下一题 Next")
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ChoiceButton(
    text: String,
    index: Int,
    correctIndex: Int,
    selectedAnswer: Int?,
    onClick: () -> Unit,
) {
    val hasAnswered = selectedAnswer != null
    val isThis = selectedAnswer == index
    val isCorrect = index == correctIndex

    val containerColor = when {
        !hasAnswered -> MaterialTheme.colorScheme.surface
        isCorrect -> CorrectGreen.copy(alpha = 0.2f)
        isThis -> WrongRed.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
    }

    val contentColor = when {
        !hasAnswered -> MaterialTheme.colorScheme.onSurface
        isCorrect -> CorrectGreen
        isThis -> WrongRed
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }

    OutlinedButton(
        onClick = onClick,
        enabled = !hasAnswered,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (hasAnswered && isCorrect) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun StatsBar(
    dueCount: Int, learnedCount: Int, totalCount: Int,
    sessionReviewed: Int, sessionCorrect: Int,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(value = "$dueCount", label = "待复习 Due")
            StatItem(
                value = if (sessionReviewed > 0) "$sessionCorrect/$sessionReviewed" else "0",
                label = "正确 Correct",
            )
            StatItem(value = "$learnedCount / $totalCount", label = "已学 Learned")
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = GoldAccent,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun SessionCompleteCard(sessionReviewed: Int, sessionCorrect: Int, onReload: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "做得好！Well done!",
                style = MaterialTheme.typography.headlineMedium,
                color = GoldAccent,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "正确率 Accuracy: $sessionCorrect / $sessionReviewed",
                style = MaterialTheme.typography.titleLarge,
                color = if (sessionReviewed > 0 && sessionCorrect * 2 >= sessionReviewed) CorrectGreen else WrongRed,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "所有到期卡片已完成，稍后再来！\nAll due cards finished. Come back later!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onReload) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("刷新 Refresh")
            }
        }
    }
}
