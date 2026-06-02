package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.MealRecord
import com.example.data.MealDatabase
import com.example.data.PreferenceHelper
import com.example.network.GenerateContentRequest
import com.example.network.Content
import com.example.network.Part
import com.example.network.NetworkClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface SheetFetchState {
    object Idle : SheetFetchState
    object Loading : SheetFetchState
    data class Success(
        val sheetName: String,
        val date: Int,
        val month: String,
        val column: Int,
        val breakfast: Int,
        val lunch: Int,
        val dinner: Int
    ) : SheetFetchState
    data class Error(val message: String) : SheetFetchState
}

sealed interface GeminiState {
    object Idle : GeminiState
    object Loading : GeminiState
    data class Success(val responseText: String) : GeminiState
    data class Error(val message: String) : GeminiState
}

class MealViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MealDatabase.getDatabase(application, viewModelScope)
    private val dao = db.mealDao()

    // Sheet simulation state
    private val _allRecords = MutableStateFlow<List<MealRecord>>(emptyList())
    val allRecords: StateFlow<List<MealRecord>> = _allRecords.asStateFlow()

    // Current date selection (defaults to current calendar day)
    private val _selectedDate = MutableStateFlow(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    val selectedDate: StateFlow<Int> = _selectedDate.asStateFlow()

    // Apps Script web app URL state
    private val _appsScriptUrl = MutableStateFlow("")
    val appsScriptUrl: StateFlow<String> = _appsScriptUrl.asStateFlow()

    // Sheet Network Fetch state
    private val _sheetFetchState = MutableStateFlow<SheetFetchState>(SheetFetchState.Idle)
    val sheetFetchState: StateFlow<SheetFetchState> = _sheetFetchState.asStateFlow()

    // Gemini AI recommendation state
    private val _geminiState = MutableStateFlow<GeminiState>(GeminiState.Idle)
    val geminiState: StateFlow<GeminiState> = _geminiState.asStateFlow()

    init {
        // Load stored Apps Script URL
        _appsScriptUrl.value = PreferenceHelper.getAppsScriptUrl(application)
        
        // Collect DB records
        viewModelScope.launch {
            dao.getAllRecords().collect { records ->
                _allRecords.value = records
            }
        }
    }

    fun selectDate(date: Int) {
        if (date in 1..31) {
            _selectedDate.value = date
        }
    }

    fun saveUrl(url: String) {
        _appsScriptUrl.value = url.trim()
        PreferenceHelper.saveAppsScriptUrl(getApplication(), url.trim())
    }

    fun updateMealCount(date: Int, type: String, increment: Boolean) {
        viewModelScope.launch {
            val record = dao.getRecordForDate(date) ?: MealRecord(date, 0, 0, 0)
            val updated = when (type.lowercase()) {
                "breakfast" -> {
                    val newVal = if (increment) record.breakfast + 1 else maxOf(0, record.breakfast - 1)
                    record.copy(breakfast = newVal)
                }
                "lunch" -> {
                    val newVal = if (increment) record.lunch + 1 else maxOf(0, record.lunch - 1)
                    record.copy(lunch = newVal)
                }
                "dinner" -> {
                    val newVal = if (increment) record.dinner + 1 else maxOf(0, record.dinner - 1)
                    record.copy(dinner = newVal)
                }
                else -> record
            }
            dao.insertOrUpdate(updated)
        }
    }

    fun fetchLiveStats() {
        val url = _appsScriptUrl.value
        if (url.isEmpty()) {
            _sheetFetchState.value = SheetFetchState.Error("Please configure your script Web App URL first in the setup tab.")
            return
        }

        _sheetFetchState.value = SheetFetchState.Loading
        viewModelScope.launch {
            try {
                val response = NetworkClient.appsScriptService.fetchLiveMealStats(url)
                if (response.success && response.data != null) {
                    _sheetFetchState.value = SheetFetchState.Success(
                        sheetName = response.sheetName ?: "Sheet Tab",
                        date = response.date ?: _selectedDate.value,
                        month = response.month ?: "Current Month",
                        column = response.column ?: 1,
                        breakfast = response.data.breakfast,
                        lunch = response.data.lunch,
                        dinner = response.data.dinner
                    )
                    
                    // Sync our local DB simulation for that fetched day!
                    response.date?.let { date ->
                        dao.insertOrUpdate(MealRecord(
                            date = date,
                            breakfast = response.data.breakfast,
                            lunch = response.data.lunch,
                            dinner = response.data.dinner
                        ))
                        _selectedDate.value = date
                    }
                } else {
                    _sheetFetchState.value = SheetFetchState.Error(response.error ?: "Failed to read Google Sheet data.")
                }
            } catch (e: Exception) {
                Log.e("MealViewModel", "Error fetching stats", e)
                _sheetFetchState.value = SheetFetchState.Error(e.message ?: "Connection error. Make sure your Apps Script is deployed as a Web App with access 'Anyone'.")
            }
        }
    }

    fun askGemini(breakfast: Int, lunch: Int, dinner: Int, day: Int) {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            _geminiState.value = GeminiState.Error("Security: Gemini API Key not specified in Secrets. Please configure your key in Google AI Studio panel.")
            return
        }

        _geminiState.value = GeminiState.Loading
        viewModelScope.launch {
            try {
                val prompt = """
                    You are a professional chef & nutrition planner guiding a kitchen service team.
                    We are catering meal requests for Day $day of our calendar sheet:
                    - Breakfasts requested: $breakfast meals
                    - Lunches requested: $lunch meals
                    - Dinners requested: $dinner meals
                    
                    Provide an executive briefing:
                    1. Practical catering planning: Advice preparing ingredients matched to our specific stats.
                    2. Recommended high-yield meals that scale effortlessly.
                    3. Kitchen timing schedule & kitchen prep flow.
                    4. Food waste reduction advice.
                    
                    Keep your answer professional, clean, bulleted, and extremely elegant. Format beautifully for mobile layout.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                val response = NetworkClient.geminiApiService.generateContent(key, request)
                val answer = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (answer != null) {
                    _geminiState.value = GeminiState.Success(answer)
                } else {
                    _geminiState.value = GeminiState.Error("Gemini returned empty response.")
                }
            } catch (e: Exception) {
                Log.e("MealViewModel", "Gemini error", e)
                _geminiState.value = GeminiState.Error(e.message ?: "Failed querying Gemini.")
            }
        }
    }
    
    fun dismissGemini() {
        _geminiState.value = GeminiState.Idle
    }
}
