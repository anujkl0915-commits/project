package com.rekaro.app.ai

import android.content.Context
import android.graphics.Bitmap
import com.rekaro.app.model.IndiaWasteDatabase
import com.rekaro.app.model.WasteAnalysisResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI Waste Classifier for ReKaro.
 *
 * Uses Google ML Kit for on-device image labeling to identify waste items,
 * then maps them to the India-specific waste disposal database.
 *
 * This runs entirely on-device — no internet required after initial model download.
 */
class WasteClassifier(private val context: Context) {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.DEFAULT_OPTIONS
    )

    /**
     * Analyze a bitmap image and return waste classification results.
     *
     * @param bitmap The captured or selected image
     * @return WasteAnalysisResult with disposal instructions
     */
    suspend fun analyze(bitmap: Bitmap): WasteAnalysisResult = withContext(Dispatchers.Default) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        try {
            // Run ML Kit labeler
            val labels = withContext(Dispatchers.IO) {
                labeler.process(inputImage).await()
            }

            // Extract top labels
            val topLabels = labels
                .sortedByDescending { it.confidence }
                .take(5)

            if (topLabels.isEmpty()) {
                return@withContext createUnknownResult()
            }

            // Try to match labels against our India waste database
            val primaryLabel = topLabels.first().text.lowercase()
            val allLabels = topLabels.map { it.text.lowercase() }
            val avgConfidence = topLabels.take(3).map { it.confidence }.average().toFloat()

            // Search in our database
            var result = IndiaWasteDatabase.analyze(primaryLabel)

            // If not found, try the combined label text
            if (result == null) {
                for (label in allLabels) {
                    result = IndiaWasteDatabase.analyze(label)
                    if (result != null) break
                }
            }

            // Final fallback: guess from keywords
            if (result == null) {
                result = IndiaWasteDatabase.guessFromKeywords(
                    allLabels.joinToString(" ")
                )
            }

            result ?: createUnknownResult().copy(
                description = "This appears to be: ${topLabels.first().text}. " +
                        "We're still learning about this item."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            createUnknownResult()
        }
    }

    /**
     * Analyze using a text query (fallback when image recognition fails).
     */
    suspend fun analyzeByText(query: String): WasteAnalysisResult = withContext(Dispatchers.Default) {
        IndiaWasteDatabase.analyze(query)
            ?: IndiaWasteDatabase.guessFromKeywords(query)
            ?: createUnknownResult()
    }

    private fun createUnknownResult() = WasteAnalysisResult(
        itemName = "Unknown Item",
        category = com.rekaro.app.model.WasteCategory.NON_RECYCLABLE,
        confidence = 0.0f,
        description = "We couldn't identify this item from the image. " +
                "Please try a clearer photo with better lighting.",
        disposalSteps = listOf(
            com.rekaro.app.model.DisposalStep("📸", "Take a clearer photo in good lighting"),
            com.rekaro.app.model.DisposalStep("🔍", "Make sure the item fills the frame"),
            com.rekaro.app.model.DisposalStep("♻️", "When in doubt, dispose in the RED bin")
        ),
        tips = listOf(
            "We're constantly learning — check back for updates!",
            "Until then, follow the 'when in doubt, throw it out' rule"
        )
    )

    /**
     * Release resources when done.
     */
    fun close() {
        labeler.close()
    }
}

/**
 * Simple Promise-like await for Tasks.
 */
suspend fun <T> com.google.mlkit.vision.common.Task<T>.await(): T {
    return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result) {}
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }
}