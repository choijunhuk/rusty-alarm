package com.example.rustyalarm.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun LocationChallengeCard(
    targetLat: Double,
    targetLng: Double,
    radiusMeters: Int,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    var distance by remember { mutableStateOf<Int?>(null) }
    var checking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "지정된 장소에 도착해야 알람이 꺼집니다.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            "허용 반경: ${radiusMeters}m",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        distance?.let {
            Text(
                "현재 거리: ${it}m",
                style = MaterialTheme.typography.titleMedium,
                color = if (it <= radiusMeters)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
        }
        status?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        }

        if (!hasPermission) {
            Button(onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("위치 권한 허용")
            }
        } else {
            Button(
                onClick = {
                    checking = true
                    status = null
                    checkLocation(context, targetLat, targetLng, radiusMeters,
                        onResult = { distMeters, success ->
                            checking = false
                            distance = distMeters
                            if (success) onSuccess()
                            else status = "아직 도착하지 않았어요. 더 가까이 가세요."
                        },
                        onError = { msg ->
                            checking = false
                            status = msg
                        }
                    )
                },
                enabled = !checking,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (checking) "확인 중..." else "현재 위치 확인")
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun checkLocation(
    context: android.content.Context,
    targetLat: Double,
    targetLng: Double,
    radiusMeters: Int,
    onResult: (Int, Boolean) -> Unit,
    onError: (String) -> Unit,
) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location: Location? ->
            if (location == null) {
                onError("위치를 가져올 수 없어요. GPS 켜져 있나요?")
                return@addOnSuccessListener
            }
            val target = Location("target").apply {
                latitude = targetLat
                longitude = targetLng
            }
            val dist = location.distanceTo(target).toInt()
            onResult(dist, dist <= radiusMeters)
        }
        .addOnFailureListener { onError("위치 가져오기 실패: ${it.localizedMessage}") }
}
