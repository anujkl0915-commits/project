package com.rekaro.app.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekaro.app.model.IndiaWasteDatabase
import com.rekaro.app.model.WasteAnalysisResult
import com.rekaro.app.model.WasteCategory
import com.rekaro.app.ui.theme.*

@Composable
fun ResultScreen(
    itemName: String,
    categoryName: String,
    onScanAgain: () -> Unit,
    onHome: () -> Unit
) {
    // Find the result from our database
    val result = remember(itemName) {
        IndiaWasteDatabase.analyze(itemName.lowercase())
            ?: IndiaWasteDatabase.guessFromKeywords(itemName.lowercase())
            ?: WasteAnalysisResult(
                itemName = itemName,
                category = when (categoryName) {
                    "Recyclable" -> WasteCategory.RECYCLABLE
                    "Compostable" -> WasteCategory.COMPOSTABLE
                    "Hazardous" -> WasteCategory.HAZARDOUS
                    "E-Waste" -> WasteCategory.E_WASTE
                    else -> WasteCategory.NON_RECYCLABLE
                },
                confidence = 0.7f,
                description = "Based on your scan, here's how to dispose this item properly.",
                disposalSteps = listOf(
                    com.rekaro.app.model.DisposalStep("📋", "Follow the bin color guide below"),
                    com.rekaro.app.model.DisposalStep("♻️", "Dispose in the correct bin")
                ),
                tips = listOf(
                    "When in doubt, check the packaging for recycling symbols",
                    "Clean items are more likely to be recycled"
                )
            )
    }

    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "result")

    // Category color
    val categoryColor = when (result.category) {
        WasteCategory.RECYCLABLE -> GreenPrimary
        WasteCategory.NON_RECYCLABLE -> RedBin
        WasteCategory.COMPOSTABLE -> GreenBin
        WasteCategory.HAZARDOUS -> ErrorRed
        WasteCategory.E_WASTE -> WarningAmber
    }

    val categoryBgColor = when (result.category) {
        WasteCategory.RECYCLABLE -> GreenSurface
        WasteCategory.NON_RECYCLABLE -> ErrorRedDark
        WasteCategory.COMPOSTABLE -> GreenSurface
        WasteCategory.HAZARDOUS -> ErrorRedDark
        WasteCategory.E_WASTE -> WarningSurface
    }

    val categoryIcon = when (result.category) {
        WasteCategory.RECYCLABLE -> "♻️"
        WasteCategory.NON_RECYCLABLE -> "🚫"
        WasteCategory.COMPOSTABLE -> "🌱"
        WasteCategory.HAZARDOUS -> "⚠️"
        WasteCategory.E_WASTE -> "🔌"
    }

    // Sequential reveal animations
    var revealPhase by remember { mutableStateOf(0) }

    val headerAlpha by animateFloatAsState(
        targetValue = if (revealPhase >= 1) 1f else 0f,
        animationSpec = tween(500),
        label = "headerAlpha"
    )

    val headerScale by animateFloatAsState(
        targetValue = if (revealPhase >= 1) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "headerScale"
    )

    val stepsAlpha by animateFloatAsState(
        targetValue = if (revealPhase >= 2) 1f else 0f,
        animationSpec = tween(400),
        label = "stepsAlpha"
    )

    val tipsAlpha by animateFloatAsState(
        targetValue = if (revealPhase >= 3) 1f else 0f,
        animationSpec = tween(400),
        label = "tipsAlpha"
    )

    val buttonsAlpha by animateFloatAsState(
        targetValue = if (revealPhase >= 4) 1f else 0f,
        animationSpec = tween(400),
        label = "buttonsAlpha"
    )

    // Confidence glow
    val confidenceGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "confidenceGlow"
    )

    LaunchedEffect(Unit) {
        revealPhase = 1
        kotlinx.coroutines.delay(400)
        revealPhase = 2
        kotlinx.coroutines.delay(300)
        revealPhase = 3
        kotlinx.coroutines.delay(300)
        revealPhase = 4
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- Back button ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onHome) {
                    Text("←", color = TextPrimary, fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ---- Result Header ----
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(headerAlpha)
                    .scale(headerScale)
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .alpha(confidenceGlow * 0.3f)
                        .background(
                            color = categoryColor.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = categoryBgColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = categoryIcon,
                            fontSize = 36.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Item name
                Text(
                    text = result.itemName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(categoryBgColor)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = categoryIcon,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = result.category.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = categoryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Confidence bar
                if (result.confidence > 0f) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Confidence:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${(result.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = categoryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(DarkSurfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .scale(
                                        scaleX = result.confidence,
                                        scaleY = 1f
                                    )
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                categoryColor.copy(alpha = 0.7f),
                                                categoryColor
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = result.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ---- Disposal Steps ----
            if (result.disposalSteps.isNotEmpty()) {
                Column(
                    modifier = Modifier.alpha(stepsAlpha)
                ) {
                    Text(
                        text = "✅ Disposal Steps",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    result.disposalSteps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (step.isCritical) ErrorRedDark.copy(alpha = 0.3f)
                                    else DarkSurfaceVariant
                                )
                                .border(
                                    width = if (step.isCritical) 1.dp else 0.dp,
                                    color = if (step.isCritical) ErrorRed.copy(alpha = 0.3f)
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Step number
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (step.isCritical) ErrorRed.copy(alpha = 0.2f)
                                        else GreenPrimary.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = step.icon,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.instruction,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (step.isCritical) ErrorRed else TextPrimary,
                                    fontWeight = if (step.isCritical) FontWeight.SemiBold
                                    else FontWeight.Normal
                                )
                                if (step.isCritical) {
                                    Text(
                                        text = "⚠️ Critical",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ErrorRed.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Check number
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(GreenPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GreenPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---- Tips Section ----
            if (result.tips.isNotEmpty()) {
                Column(
                    modifier = Modifier.alpha(tipsAlpha)
                ) {
                    Text(
                        text = "💡 Pro Tips",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    result.tips.forEach { tip ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                color = AccentBlue,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // ---- Alternative note ----
            if (result.alternativeDisposalNote != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(tipsAlpha),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = BlueSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("🔄", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = result.alternativeDisposalNote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentBlueLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ---- Action Buttons ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(buttonsAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Scan Again
                Button(
                    onClick = onScanAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary
                    )
                ) {
                    Text(
                        text = "📷  Scan Again",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Home
                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    ),
                    border = BorderStroke(1.dp, TextTertiary)
                ) {
                    Text(
                        text = "🏠  Home",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}