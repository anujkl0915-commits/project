package com.rekaro.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.YuvImage
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.rekaro.app.ai.WasteClassifier
import com.rekaro.app.model.IndiaWasteDatabase
import com.rekaro.app.ui.theme.*
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onResult: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val classifier = remember { WasteClassifier(context) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "camera")
    val scanLineAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "scanLineAlpha"
    )
    val cornerPulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "cornerPulse"
    )
    val scanLinePosition by infiniteTransition.animateFloat(
        initialValue = -100f, targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "scanLinePosition"
    )

    // Request permission
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            classifier.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        if (permissionGranted) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setFlashMode(if (flashEnabled) ImageCapture.FLASH_MODE_ON
                            else ImageCapture.FLASH_MODE_OFF)
                            .build()

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner, cameraSelector, preview, imageCapture
                            )
                        } catch (_: Exception) { }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DarkBackground.copy(alpha = 0.7f),
                                Color.Transparent, Color.Transparent,
                                DarkBackground.copy(alpha = 0.5f)
                            )
                        )
                    )
            ) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Text("✕", color = TextPrimary, fontSize = 24.sp)
                    }
                    Text("Scan Waste", style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { flashEnabled = !flashEnabled }) {
                        Text(if (flashEnabled) "⚡" else "🔦", fontSize = 20.sp)
                    }
                }

                // Scan Frame
                Box(
                    modifier = Modifier.fillMaxSize().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(280.dp)) {
                        CornerBracket(Modifier.align(Alignment.TopStart), cornerPulse, 0f)
                        CornerBracket(Modifier.align(Alignment.TopEnd), cornerPulse, 90f)
                        CornerBracket(Modifier.align(Alignment.BottomStart), cornerPulse, 270f)
                        CornerBracket(Modifier.align(Alignment.BottomEnd), cornerPulse, 180f)
                        Text("📷", fontSize = 32.sp, modifier = Modifier.align(Alignment.Center))
                    }
                }

                // Scan line
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)
                        .offset(y = scanLinePosition.dp + 120.dp).height(2.dp)
                        .alpha(scanLineAlpha)
                        .background(Brush.horizontalGradient(
                            listOf(Color.Transparent, GreenPrimary, AccentBlue, GreenPrimary, Color.Transparent)
                        ))
                )

                // Bottom controls
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Position waste in the frame",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                    Spacer(Modifier.height(20.dp))

                    // Capture button
                    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(80.dp)
                            .border(3.dp, TextPrimary, CircleShape))
                        Button(
                            onClick = {
                                if (!isAnalyzing) {
                                    captureAndAnalyze(
                                        imageCapture, classifier, context,
                                        onStart = { isAnalyzing = true },
                                        onResult = { name, cat ->
                                            isAnalyzing = false
                                            onResult(name, cat)
                                        },
                                        onError = { isAnalyzing = false }
                                    )
                                }
                            },
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            contentPadding = PaddingValues(0.dp),
                            enabled = !isAnalyzing
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(color = GreenPrimary,
                                    modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White))
                            }
                        }
                    }
                }
            }
        } else {
            // Permission denied
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)) {
                    Text("📷", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Camera Permission Needed",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("ReKaro needs camera access to scan your waste items.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp)) {
                        Text("Grant Permission", color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onBack) { Text("Go Back", color = TextSecondary) }
                }
            }
        }
    }
}

@Composable
private fun CornerBracket(modifier: Modifier, alpha: Float, rotation: Float) {
    Box(modifier = modifier.size(36.dp).alpha(alpha)) {
        Box(Modifier.width(3.dp).height(20.dp).background(GreenPrimary)
            .align(if (rotation % 180f == 0f) Alignment.TopCenter else Alignment.BottomCenter))
        Box(Modifier.width(20.dp).height(3.dp).background(GreenPrimary)
            .align(if (rotation % 180f == 0f) Alignment.TopStart else Alignment.BottomStart))
    }
}

private fun captureAndAnalyze(
    imageCapture: ImageCapture?,
    classifier: WasteClassifier,
    context: android.content.Context,
    onStart: () -> Unit,
    onResult: (String, String) -> Unit,
    onError: () -> Unit
) {
    if (imageCapture == null) { onError(); return }
    onStart()

    imageCapture.takePicture(ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                if (bitmap != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                classifier.analyze(bitmap)
                            }
                            onResult(result.itemName, result.category.displayName)
                        } catch (_: Exception) {
                            onResult("Unknown", "Non-Recyclable")
                        }
                    }
                } else {
                    onError()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                onError()
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val yuvImage = YuvImage(bytes, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 90, out)
        val jpegData = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)

        val matrix = Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (_: Exception) { null }
}