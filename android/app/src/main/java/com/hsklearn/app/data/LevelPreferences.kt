package com.hsklearn.app.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LevelPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hsk_prefs", Context.MODE_PRIVATE)

    private val _selectedLevel = MutableStateFlow(prefs.getInt("hsk_level", 4))
    val selectedLevel: StateFlow<Int> = _selectedLevel.asStateFlow()

    fun setLevel(level: Int) {
        prefs.edit().putInt("hsk_level", level).apply()
        _selectedLevel.value = level
    }

    companion object {
        val AVAILABLE_LEVELS = listOf(1, 2, 3, 4)
    }
}
