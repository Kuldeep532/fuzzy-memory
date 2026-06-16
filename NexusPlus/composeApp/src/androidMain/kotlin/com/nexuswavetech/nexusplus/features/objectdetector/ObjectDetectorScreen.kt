package com.nexuswavetech.nexusplus.features.objectdetector

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

data class DetectedObject(
    val label: String,
    val confidence: Float,
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ObjectDetectorScreen(onBack: () -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view           = LocalView.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var detectedObjects by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }

    val labeler = remember {
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    }

    DisposableEffect(Unit) { onDispose { labeler.close() } }

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = "Object Detector", onBack = onBack)

        if (!cameraPermission.status.isGranted) {
            CameraPermissionRequest(onRequest = { cameraPermission.launchPermissionRequest() })
        } else {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { pv ->
                            val provider = ProcessCameraProvider.getInstance(ctx)
                            provider.addListener({
                                val cameraProvider = provider.get()
                                val preview = Preview.Builder().build().apply {
                                    setSurfaceProvider(pv.surfaceProvider)
                                }
                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .apply {
                                        setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                                            val image = InputImage.fromMediaImage(
                                                proxy.image!!,
                                                proxy.imageInfo.rotationDegrees
                                            )
                                            labeler.process(image)
                                                .addOnSuccessListener { labels ->
                                                    detectedObjects = labels
                                                        .filter { it.confidence >= 0.65f }
                                                        .map { DetectedObject(it.text, it.confidence) }
                                                    labels.firstOrNull()?.let {
                                                        view.announceForAccessibility("Detected: ${it.text}")
                                                    }
                                                    proxy.close()
                                                }
                                                .addOnFailureListener { proxy.close() }
                                        }
                                    }
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "Camera preview for object detection. Detected labels are announced." }
                )

                // Labels overlay
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (detectedObjects.isEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            Text(
                                "Point camera at objects",
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        detectedObjects.take(5).forEach { obj ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.CenterFocusStrong,
                                        null,
                                        Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text  = "${obj.label}  ${(obj.confidence * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionRequest(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Filled.CameraAlt, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Camera Permission Required", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(
                "Object Detector needs camera access to identify objects in real-time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRequest,
                modifier = Modifier.semantics { contentDescription = "Grant camera permission for object detection" }
            ) {
                Text("Grant Permission")
            }
        }
    }
}
