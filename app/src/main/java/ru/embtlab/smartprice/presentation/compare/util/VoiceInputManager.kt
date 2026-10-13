package ru.embtlab.smartprice.presentation.compare.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface VoiceRecognitionState {
    data object Idle : VoiceRecognitionState
    data object Ready : VoiceRecognitionState
    data class Listening(val partialText: String = "", val rmsDb: Float = 0f) : VoiceRecognitionState
    data class Success(val text: String) : VoiceRecognitionState
    data class Error(val message: String) : VoiceRecognitionState
}

class VoiceInputManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    val state: StateFlow<VoiceRecognitionState> = _state.asStateFlow()

    private var currentText = ""
    private var isUserHolding = false
    private var finalizeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private fun getOrCreateRecognizer(): SpeechRecognizer? {
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _state.value = VoiceRecognitionState.Ready
                    }

                    override fun onBeginningOfSpeech() {
                        _state.value = VoiceRecognitionState.Listening(partialText = currentText, rmsDb = 0f)
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        if (_state.value is VoiceRecognitionState.Listening || _state.value is VoiceRecognitionState.Ready) {
                            _state.value = VoiceRecognitionState.Listening(
                                partialText = currentText,
                                rmsDb = rmsdB.coerceAtLeast(0f)
                            )
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        finalizeJob?.cancel()
                        // Если палец уже отпустили и в буфере был хоть какой-то текст — отдаем его как успех!
                        if (!isUserHolding && currentText.isNotBlank()) {
                            _state.value = VoiceRecognitionState.Success(currentText.trim())
                            return
                        }

                        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            _state.value = VoiceRecognitionState.Error("Ошибка записи ($error)")
                        } else {
                            _state.value = VoiceRecognitionState.Idle
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        finalizeJob?.cancel()
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.ifBlank { null } ?: currentText

                        if (text.isNotBlank()) {
                            currentText = text
                            _state.value = VoiceRecognitionState.Success(text.trim())
                        } else {
                            _state.value = VoiceRecognitionState.Idle
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            currentText = text
                            val rms = (_state.value as? VoiceRecognitionState.Listening)?.rmsDb ?: 0f
                            _state.value = VoiceRecognitionState.Listening(partialText = text, rmsDb = rms)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
        return speechRecognizer
    }

    fun startListening() {
        finalizeJob?.cancel()
        val recognizer = getOrCreateRecognizer() ?: run {
            _state.value = VoiceRecognitionState.Error("Голосовой ввод недоступен")
            return
        }

        try { recognizer.cancel() } catch (_: Exception) {}

        currentText = ""
        isUserHolding = true
        _state.value = VoiceRecognitionState.Ready

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
        }

        try {
            recognizer.startListening(intent)
        } catch (_: Exception) {
            isUserHolding = false
            _state.value = VoiceRecognitionState.Error("Не удалось запустить микрофон")
        }
    }

    /**
     * Пользователь отпустил палец.
     * Мы останавливаем запись и даём движку до 600 мс вернуть финал.
     * Если движок молчит — форсируем отправку накопленного currentText.
     */
    fun stopListeningAndCommit() {
        if (!isUserHolding) return
        isUserHolding = false

        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}

        // Страховочный таймер: если onResults задерживается, через 500мс отдаем то, что есть
        finalizeJob?.cancel()
        finalizeJob = scope.launch {
            delay(500)
            if (currentText.isNotBlank() && _state.value !is VoiceRecognitionState.Success) {
                _state.value = VoiceRecognitionState.Success(currentText.trim())
            } else if (currentText.isBlank()) {
                _state.value = VoiceRecognitionState.Idle
            }
        }
    }

    fun cancel() {
        finalizeJob?.cancel()
        isUserHolding = false
        currentText = ""
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        _state.value = VoiceRecognitionState.Idle
    }

    fun resetState() {
        finalizeJob?.cancel()
        _state.value = VoiceRecognitionState.Idle
        currentText = ""
    }

    fun destroy() {
        cancel()
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }
}