package com.nexuswavetech.nexusplus.features.imagegen

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.androidx.compose.koinViewModel
import java.net.URLEncoder

data class AiImageUiState(
    val prompt: String = "",
    val style: ImageStyle = ImageStyle.NONE,
    val isGenerating: Boolean = false,
    val generatedBitmap: Bitmap? = null,
    val error: String? = null,
    val savedSuccess: Boolean = false
)

enum class ImageStyle(val label: String, val modifier: String) {
    NONE("Default", ""),
    PHOTOREALISTIC("Photorealistic", "photorealistic, ultra realistic, 8K"),
    ANIME("Anime", "anime style, vibrant, detailed"),
    OIL_PAINTING("Oil Painting", "oil painting, classical art style"),
    WATERCOLOR("Watercolor", "watercolor painting, soft colors"),
    CYBERPUNK("Cyberpunk", "cyberpunk, neon lights, futuristic"),
    MINIMALIST("Minimalist", "minimalist, clean lines, flat design")
}

class AiImageViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AiImageUiState())
    val uiState: StateFlow<AiImageUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .callTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun onPromptChanged(prompt: String) {
        _uiState.update { it.copy(prompt = prompt, error = null) }
    }

    fun onStyleSelected(style: ImageStyle) {
        _uiState.update { it.copy(style = style) }
    }

    fun generateImage() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a prompt") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isGenerating = true, error = null, generatedBitmap = null) }
            val fullPrompt = if (_uiState.value.style == ImageStyle.NONE) prompt
            else "$prompt, ${_uiState.value.style.modifier}"
            val encoded = URLEncoder.encode(fullPrompt, "UTF-8")
            val url = "https://image.pollinations.ai/prompt/$encoded?width=1024&height=1024&nologo=true"
            val request = Request.Builder().url(url).build()
            try {
                val response = client.newCall(request).execute()
                val bytes = response.body?.bytes()
                if (bytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    _uiState.update { it.copy(isGenerating = false, generatedBitmap = bitmap) }
                } else {
                    _uiState.update { it.copy(isGenerating = false, error = "Failed to decode image") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, error = "Generation failed: ${e.message}") }
            }
        }
    }

    fun saveToGallery(context: android.content.Context) {
        val bitmap = _uiState.value.generatedBitmap ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "nexus_ai_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NexusPlus")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                }
                _uiState.update { state -> state.copy(savedSuccess = true) }
                delay(2000)
                _uiState.update { state -> state.copy(savedSuccess = false) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiImageGeneratorScreen(
    onBack: () -> Unit,
    viewModel: AiImageViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(uiState.isGenerating) {
        if (uiState.isGenerating) view.announceForAccessibility("Generating AI image. Please wait.")
    }
    LaunchedEffect(uiState.generatedBitmap) {
        if (uiState.generatedBitmap != null) view.announceForAccessibility("Image generated. Double tap Save to Gallery button to save.")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "AI Image Generator", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.prompt,
                onValueChange = viewModel::onPromptChanged,
                label = { Text("Describe your image…") },
                placeholder = { Text("e.g. A cyberpunk city at night with neon lights") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Image prompt input. Describe the image you want to generate." },
                maxLines = 4,
                isError = uiState.error != null
            )

            // Style selector
            Text(
                text = "Style",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ImageStyle.values().forEach { style ->
                    FilterChip(
                        selected = uiState.style == style,
                        onClick = { viewModel.onStyleSelected(style) },
                        label = { Text(style.label) },
                        modifier = Modifier.semantics {
                            contentDescription = "${style.label} image style" +
                                if (uiState.style == style) ". Currently selected." else ""
                        }
                    )
                }
            }

            Button(
                onClick = {
                    view.announceForAccessibility("Generating image for prompt: ${uiState.prompt}")
                    viewModel.generateImage()
                },
                enabled = !uiState.isGenerating && uiState.prompt.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = "Generate AI image. Activate to start generation." }
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Generating…")
                } else {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Image")
                }
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Generated image result
            AnimatedVisibility(visible = uiState.generatedBitmap != null) {
                uiState.generatedBitmap?.let { bitmap ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Generated AI image for prompt: ${uiState.prompt}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { viewModel.generateImage() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Regenerate")
                            }

                            Button(
                                onClick = {
                                    viewModel.saveToGallery(context)
                                    view.announceForAccessibility("Image saved to gallery")
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Filled.SaveAlt, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Save")
                            }
                        }

                        if (uiState.savedSuccess) {
                            Text(
                                "✓ Saved to Pictures/NexusPlus",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.semantics {
                                    contentDescription = "Image successfully saved to gallery."
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
