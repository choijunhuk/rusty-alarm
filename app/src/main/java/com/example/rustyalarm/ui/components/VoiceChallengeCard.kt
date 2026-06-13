package com.example.rustyalarm.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * Microphone-driven challenge: the user must say one of [acceptPhrases]
 * (defaults shown to user as "일어났다") to dismiss the alarm. Recognition
 * cycles continuously until the device hears a match.
 */
@Composable
fun VoiceChallengeCard(
    acceptPhrases: List<String> = DEFAULT_PHRASES,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    var heard by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("아래 문구를 또박또박 말해주세요.") }

    if (hasPermission) {
        DisposableEffect(Unit) {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                    status = "듣고 있어요…"
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false }
                override fun onError(error: Int) {
                    listening = false
                    status = "잘 못 들었어요. 다시 시도할게요."
                    runCatching { recognizer.startListening(intent) }
                }
                override fun onResults(results: Bundle?) {
                    handleResults(results)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    handleResults(partialResults, partial = true)
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}

                private fun handleResults(bundle: Bundle?, partial: Boolean = false) {
                    val list = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?: return
                    val text = list.joinToString(" ").trim()
                    if (text.isNotEmpty()) heard = text
                    val match = list.any { spoken ->
                        acceptPhrases.any { phrase ->
                            spoken.contains(phrase, ignoreCase = true) ||
                                normalise(spoken) == normalise(phrase)
                        }
                    }
                    if (match) {
                        onSuccess()
                        runCatching { recognizer.stopListening() }
                    } else if (!partial) {
                        runCatching { recognizer.startListening(intent) }
                    }
                }
            }
            recognizer.setRecognitionListener(listener)
            runCatching { recognizer.startListening(intent) }
            onDispose {
                runCatching {
                    recognizer.stopListening()
                    recognizer.cancel()
                    recognizer.destroy()
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "🎤 말해서 알람 끄기",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "\"${acceptPhrases.first()}\" 라고 말해주세요",
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            status,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        if (heard.isNotEmpty()) {
            Text(
                "들은 말: \"$heard\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (!hasPermission) {
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("마이크 권한 허용")
            }
        }
    }
}

private fun normalise(s: String): String =
    s.lowercase().filter { it.isLetterOrDigit() }

private val DEFAULT_PHRASES = listOf(
    "일어났다",
    "일어났어",
    "굿모닝",
    "good morning",
    "wake up",
    "기상",
)
