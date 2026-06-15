package com.nexuswavetech.nexusplus.features.imageviewer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.nexuswavetech.nexusplus.navigation.Screen
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NexusImageViewerScreen(
    initialUri: Uri? = null,
    onBack: () -> Unit,
    onOpenEditor: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    var currentUri by remember { mutableStateOf<Uri?>(initialUri) }
    var scale by remember { mutableStateOf(1f) }
    var rotationDegrees by remember { mutableStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }

    val imagePermission = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
    )

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            currentUri = uri
            scale = 1f
            rotationDegrees = 0f
            view.announceForAccessibility("Image opened")
        }
    }

    LaunchedEffect(Unit) {
        if (currentUri == null && !imagePermission.status.isGranted) {
            imagePermission.launchPermissionRequest()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showControls && !isFullscreen,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            NexusTopBar(
                title = "Nexus Image Viewer",
                onBack = onBack,
                actions = {
                    if (currentUri != null && onOpenEditor != null) {
                        IconButton(
                            onClick = { onOpenEditor(currentUri!!) },
                            modifier = Modifier.semantics { contentDescription = "Open in image editor" }
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                        }
                    }
                    IconButton(
                        onClick = {
                            isFullscreen = !isFullscreen
                            showControls = !isFullscreen
                        },
                        modifier = Modifier.semantics { contentDescription = if (isFullscreen) "Exit fullscreen" else "Enter fullscreen" }
                    ) {
                        Icon(
                            if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = null
                        )
                    }
                }
            )
        }

        if (currentUri == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Photo,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Open an Image",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Supports JPG, JPEG, PNG, WEBP, GIF, BMP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { filePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "Browse and open an image file"
                        }
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Browse Images")
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, rotation ->
                            scale = (scale * zoom).coerceIn(0.5f, 6f)
                            rotationDegrees += rotation
                        }
                    }
                    .semantics {
                        contentDescription = "Image viewer. Pinch to zoom, rotate with two fingers."
                    },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = currentUri,
                    contentDescription = "Opened image",
                    loading = {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    },
                    error = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.BrokenImage, null, tint = Color.White, modifier = Modifier.size(64.dp))
                            Text("Unable to load image", color = Color.White)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            rotationZ = rotationDegrees
                        ),
                    contentScale = ContentScale.Fit
                )
            }

            // Bottom controls
            AnimatedVisibility(
                visible = showControls && !isFullscreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(tonalElevation = 4.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Zoom out
                            IconButton(
                                onClick = { scale = (scale - 0.25f).coerceAtLeast(0.5f) },
                                modifier = Modifier.semantics { contentDescription = "Zoom out" }
                            ) { Icon(Icons.Filled.ZoomOut, contentDescription = null) }

                            // Reset
                            IconButton(
                                onClick = { scale = 1f; rotationDegrees = 0f },
                                modifier = Modifier.semantics { contentDescription = "Reset zoom and rotation" }
                            ) { Icon(Icons.Filled.FitScreen, contentDescription = null) }

                            // Zoom in
                            IconButton(
                                onClick = { scale = (scale + 0.25f).coerceAtMost(6f) },
                                modifier = Modifier.semantics { contentDescription = "Zoom in" }
                            ) { Icon(Icons.Filled.ZoomIn, contentDescription = null) }

                            // Rotate left
                            IconButton(
                                onClick = { rotationDegrees -= 90f; view.announceForAccessibility("Rotated left") },
                                modifier = Modifier.semantics { contentDescription = "Rotate left 90 degrees" }
                            ) { Icon(Icons.Filled.RotateLeft, contentDescription = null) }

                            // Rotate right
                            IconButton(
                                onClick = { rotationDegrees += 90f; view.announceForAccessibility("Rotated right") },
                                modifier = Modifier.semantics { contentDescription = "Rotate right 90 degrees" }
                            ) { Icon(Icons.Filled.RotateRight, contentDescription = null) }

                            // Open new image
                            IconButton(
                                onClick = { filePicker.launch("image/*") },
                                modifier = Modifier.semantics { contentDescription = "Open another image" }
                            ) { Icon(Icons.Filled.FolderOpen, contentDescription = null) }

                            // Share
                            IconButton(
                                onClick = {
                                    val shareIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_STREAM, currentUri)
                                        type = "image/*"
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share image"))
                                },
                                modifier = Modifier.semantics { contentDescription = "Share image" }
                            ) { Icon(Icons.Filled.Share, contentDescription = null) }
                        }
                    }
                }
            }
        }
    }
}
