package com.example.rustyalarm.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Camera viewfinder that fires [onSuccess] the first time any QR code is
 * detected in frame. Optionally constrain to a target string via [requiredValue].
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun QrChallengeCard(
    requiredValue: String? = null,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    var status by remember { mutableStateOf("QR 코드를 카메라에 비춰주세요.") }
    var solved by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "QR 코드를 스캔해서 알람을 꺼주세요",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (!requiredValue.isNullOrBlank()) {
            Text(
                "지정된 QR 코드만 통과돼요.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Text(
            status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )

        if (!hasPermission) {
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("카메라 권한 허용")
            }
        } else {
            val scanner = remember {
                BarcodeScanning.getClient()
            }
            val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
            DisposableEffect(Unit) {
                onDispose {
                    runCatching { analyzerExecutor.shutdown() }
                    runCatching { scanner.close() }
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(14.dp)),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraFuture: ListenableFuture<ProcessCameraProvider> =
                        ProcessCameraProvider.getInstance(ctx)
                    cameraFuture.addListener({
                        runCatching {
                            val provider = cameraFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { ia ->
                                    ia.setAnalyzer(analyzerExecutor) { imageProxy ->
                                        if (solved) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }
                                        val media = imageProxy.image
                                        if (media == null) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }
                                        val rotation = imageProxy.imageInfo.rotationDegrees
                                        val img = InputImage.fromMediaImage(media, rotation)
                                        scanner.process(img)
                                            .addOnSuccessListener { barcodes ->
                                                if (solved) return@addOnSuccessListener
                                                val match = barcodes.firstOrNull { b ->
                                                    b.valueType == Barcode.TYPE_TEXT ||
                                                        b.valueType == Barcode.TYPE_URL ||
                                                        b.rawValue != null
                                                }
                                                val value = match?.rawValue
                                                if (value != null) {
                                                    val ok = requiredValue.isNullOrBlank() ||
                                                        requiredValue == value
                                                    if (ok) {
                                                        solved = true
                                                        status = "✓ 인식됨"
                                                        onSuccess()
                                                    } else {
                                                        status = "다른 QR 코드를 비춰주세요."
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    }
                                }
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview, analysis,
                            )
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
            )
        }
    }
}
