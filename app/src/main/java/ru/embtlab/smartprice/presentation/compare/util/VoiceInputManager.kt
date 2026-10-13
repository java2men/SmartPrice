package ru.embtlab.smartprice.presentation.compare.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private var currentPartialText = ""

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = VoiceRecognitionState.Error("Голосовой ввод недоступен на устройстве")
            return
        }

        stopListening()
        currentPartialText = ""

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = VoiceRecognitionState.Ready
                }

                override fun onBeginningOfSpeech() {
                    _state.value = VoiceRecognitionState.Listening(partialText = "", rmsDb = 0f)
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val current = _state.value
                    if (current is VoiceRecognitionState.Listening) {
                        _state.value = current.copy(rmsDb = rmsdB.coerceAtLeast(0f))
                    } else if (current is VoiceRecognitionState.Ready) {
                        _state.value = VoiceRecognitionState.Listening(partialText = currentPartialText, rmsDb = rmsdB.coerceAtLeast(0f))
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if ((error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) && currentPartialText.isNotBlank()) {
                        _state.value = VoiceRecognitionState.Success(currentPartialText)
                        return
                    }

                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Не удалось разобрать речь"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Время ожидания истекло"
                        SpeechRecognizer.ERROR_NETWORK -> "Нет сети или оффлайн-пакета русского языка"
                        SpeechRecognizer.ERROR_AUDIO -> "Ошибка аудиоустройства"
                        else -> "Ошибка распознавания ($error)"
                    }
                    _state.value = VoiceRecognitionState.Error(msg)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val finalCandidate = matches?.firstOrNull() ?: currentPartialText
                    if (finalCandidate.isNotBlank()) {
                        _state.value = VoiceRecognitionState.Success(finalCandidate)
                    } else {
                        _state.value = VoiceRecognitionState.Error("Текст не распознан")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val candidate = matches?.firstOrNull()
                    if (!candidate.isNullOrBlank()) {
                        currentPartialText = candidate
                        val rms = (_state.value as? VoiceRecognitionState.Listening)?.rmsDb ?: 0f
                        _state.value = VoiceRecognitionState.Listening(partialText = candidate, rmsDb = rms)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

            // Расширенные таймауты пауз для спокойного чтения ценника
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
        }

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _state.value = VoiceRecognitionState.Idle
    }
}