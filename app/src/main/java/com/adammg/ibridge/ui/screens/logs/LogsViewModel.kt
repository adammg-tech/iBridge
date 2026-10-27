package com.adammg.ibridge.ui.screens.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adammg.ibridge.data.model.CrashReport
import com.adammg.ibridge.data.model.LogEntry
import com.adammg.ibridge.data.repository.LogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LogRepository(application)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _crashReports = MutableStateFlow<List<CrashReport>>(emptyList())
    val crashReports: StateFlow<List<CrashReport>> = _crashReports.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private var streamJob: Job? = null

    fun startLogStreaming() {
        stopLogStreaming()
        _isStreaming.value = true

        streamJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val logBuffer = java.util.Collections.synchronizedList(mutableListOf<LogEntry>())

            val flusherJob = launch {
                while (true) {
                    kotlinx.coroutines.delay(250)
                    if (logBuffer.isNotEmpty()) {
                        val currentList = ArrayList(logBuffer)
                        _logs.value = currentList.takeLast(200)
                    }
                }
            }

            try {
                repository.streamLiveSyslog().collect { entry ->
                    logBuffer.add(entry)
                    if (logBuffer.size > 500) {
                        logBuffer.subList(0, logBuffer.size - 200).clear()
                    }
                }
            } catch (e: Exception) {
                _logs.value = _logs.value + LogEntry("", "Error", "", "ERROR", "Syslog error: ${e.message}")
            } finally {
                flusherJob.cancel()
            }
        }
    }

    fun stopLogStreaming() {
        streamJob?.cancel()
        streamJob = null
        _isStreaming.value = false
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun fetchCrashReports() {
        viewModelScope.launch {
            _crashReports.value = repository.getCrashReports()
        }
    }
}
