package com.rekaro.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekaro.app.ui.theme.AccentBlue
import com.rekaro.app.ui.theme.DarkBackground
import com.rekaro.app.ui.theme.GreenPrimary
import com.rekaro.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    var animationPhase by remember { mutableStateOf(0) }

    // Logo scale animation
    val logoScale by animateFloatAsState(
        targetValue = if (animationPhase >= 1) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    // Logo alpha animation
    val logoAlpha by animateFloatAsState(
        targetValue = if (animationPhase >= 1) 1f else 0f,
        animationSpec = tween(600),
        label = "logoAlpha"
    )

    // Tagline alpha animation
    val taglineAlpha by animateFloatAsState(
        targetValue = if (animationPhase >= 2) 1f else 0f,
        animationSpec = tween(500),
        label = "taglineAlpha"
    )

    // Subtitle alpha animation
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (animationPhase >= 3) 1f else 0f,
        animationSpec = tween(500),
        label = "subtitleAlpha"
    )

    // Glow pulse animation (continuous)
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    LaunchedEffect(Unit) {
        // Phase 1: Logo appears
        animationPhase = 1
        delay(500)
        // Phase 2: Tagline fades in
        animationPhase = 2
        delay(400)
        // Phase 3: Subtitle fades in
        animationPhase = 3
        delay(800)
        // Phase 4: Hold and proceed
        delay(1000)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .alpha(logoAlpha)
                .scale(logoScale)
        ) {
            // App Logo Icon
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .alpha(glowAlpha)
                        .background(
                            color = GreenPrimary.copy(alpha = 0.15f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                // Inner circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = GreenPrimary.copy(alpha = 0.2f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♻️",
                        fontSize = 40.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "ReKaro",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Sort Smart. Waste Less.",
                style = MaterialTheme.typography.titleLarge,
                color = GreenPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alpha(taglineAlpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = "India's AI Waste Assistant",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Decorative line
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .background(
                        color = AccentBlue.copy(alpha = subtitleAlpha * 0.6f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}