package com.hsklearn.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hsklearn.app.data.LevelPreferences
import com.hsklearn.app.data.model.ProgressSummary
import com.hsklearn.app.data.model.TaskSummary
import com.hsklearn.app.data.model.WeakWord
import com.hsklearn.app.ui.theme.GoldAccent

private val AccentBlue = Color(0xFF5C7AEA)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentOrange = Color(0xFFFFA726)
private val AccentPurple = Color(0xFFAB47BC)

@Composable
fun HomeScreen(
    onNavigateToReading: () -> Unit = {},
    onNavigateToSpeaking: () -> Unit = {},
    onNavigateToWriting: () -> Unit = {},
    onNavigateToFlashcards: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Welcome banner
        WelcomeBanner()

        Spacer(Modifier.height(12.dp))

        // Level selector
        LevelSelector(
            selectedLevel = state.selectedLevel,
            onSelectLevel = viewModel::setLevel,
        )

        Spacer(Modifier.height(16.dp))

        if (state.isLoading && state.summary == null) {
            CircularProgressIndicator(
                color = GoldAccent,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(32.dp),
            )
        }

        state.summary?.let { summary ->
            VocabularyProgressCard(summary, state.selectedLevel)

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(
                    title = "阅读 Reading", subtitle = "",
                    icon = Icons.Default.MenuBook, accent = AccentBlue,
                    summary = summary.reading, modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "口语 Speaking", subtitle = "",
                    icon = Icons.Default.Mic, accent = AccentGreen,
                    summary = summary.speaking, modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "书写 Writing", subtitle = "",
                    icon = Icons.Default.Edit, accent = AccentOrange,
                    summary = summary.writing, modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // Quick practice
        Text(
            text = "快速练习 Quick Practice",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickPracticeCard(
                label = "阅读一篇\nRead", icon = Icons.Default.MenuBook,
                accent = AccentBlue, onClick = onNavigateToReading,
                modifier = Modifier.weight(1f),
            )
            QuickPracticeCard(
                label = "朗读一句\nSpeak", icon = Icons.Default.Mic,
                accent = AccentGreen, onClick = onNavigateToSpeaking,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickPracticeCard(
                label = "书写一字\nWrite", icon = Icons.Default.Edit,
                accent = AccentOrange, onClick = onNavigateToWriting,
                modifier = Modifier.weight(1f),
            )
            QuickPracticeCard(
                label = "复习卡片\nFlashcards", icon = Icons.Default.Style,
                accent = AccentPurple, onClick = onNavigateToFlashcards,
                modifier = Modifier.weight(1f),
            )
        }

        // Weak words
        state.summary?.let { summary ->
            if (summary.weakWords.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "薄弱词汇 Weak Words",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                WeakWordsList(words = summary.weakWords)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LevelSelector(selectedLevel: Int, onSelectLevel: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        LevelPreferences.AVAILABLE_LEVELS.forEach { level ->
            FilterChip(
                selected = selectedLevel == level,
                onClick = { onSelectLevel(level) },
                label = { Text("HSK $level") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldAccent,
                    selectedLabelColor = Color(0xFF1A1A2E),
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WelcomeBanner() {
    val today = java.time.LocalDate.now()
    val dayOfWeek = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
    val dow = dayOfWeek[today.dayOfWeek.value - 1]
    val dateStr = "${today.year}年${today.monthValue}月${today.dayOfMonth}日 $dow"

    Card(
        colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = dateStr, style = MaterialTheme.typography.bodyMedium, color = GoldAccent)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "继续加油！每天进步一点点。\nKeep it up! A little progress every day.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun VocabularyProgressCard(summary: ProgressSummary, level: Int) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "HSK $level 词汇掌握 Vocabulary",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${summary.totalWordsLearned} / ${summary.totalWords}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GoldAccent, fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            val progress = if (summary.totalWords > 0)
                summary.totalWordsLearned.toFloat() / summary.totalWords else 0f
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = GoldAccent, trackColor = GoldAccent.copy(alpha = 0.15f),
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String, subtitle: String, icon: ImageVector,
    accent: Color, summary: TaskSummary, modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier,
    ) {
        Column {
            Spacer(modifier = Modifier.fillMaxWidth().height(4.dp).background(accent))
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${summary.itemsCompleted}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = accent, fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "完成 Done", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                if (summary.averageScore > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "均分 Avg. ${summary.averageScore.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPracticeCard(
    label: String, icon: ImageVector, accent: Color,
    onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f)),
        modifier = modifier.height(80.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = label, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WeakWordsList(words: List<WeakWord>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(words) { word -> WeakWordChip(word) }
    }
}

@Composable
private fun WeakWordChip(word: WeakWord) {
    val scoreColor = if (word.averageScore >= 60) Color(0xFFFFA726) else Color(0xFFE53935)
    OutlinedCard {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = word.word, fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = word.pinyin, style = MaterialTheme.typography.bodySmall, color = GoldAccent)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${word.averageScore.toInt()}分",
                style = MaterialTheme.typography.bodyMedium,
                color = scoreColor, fontWeight = FontWeight.Bold,
            )
        }
    }
}
