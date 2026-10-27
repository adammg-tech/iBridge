package com.adammg.ibridge.core.logger

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val _logFlow = MutableStateFlow<List<String>>(emptyList())
    val logFlow: StateFlow<List<String>> = _logFlow.asStateFlow()

    fun log(tag: String, message: String) {
        val time = dateFormat.format(Date())
        val formattedLine = "[$time] [$tag] $message"
        Log.d(tag, message)

        synchronized(this) {
            val currentList = _logFlow.value.toMutableList()
            currentList.add(formattedLine)
            if (currentList.size > 500) {
                _logFlow.value = currentList.takeLast(300)
            } else {
                _logFlow.value = currentList
            }
        }
    }

    fun clear() {
        synchronized(this) {
            _logFlow.value = emptyList()
        }
    }
}
