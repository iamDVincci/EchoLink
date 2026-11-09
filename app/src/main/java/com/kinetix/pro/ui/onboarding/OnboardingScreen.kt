package com.kinetix.pro.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinetix.pro.core.ui.components.ObsidianInnerCard
import com.kinetix.pro.core.ui.components.SunsetPrimaryButton
import com.kinetix.pro.core.ui.theme.ContentHigh
import com.kinetix.pro.core.ui.theme.ContentMedium
import com.kinetix.pro.core.ui.theme.ContentMuted
import com.kinetix.pro.core.ui.theme.ObsidianCanvas
import com.kinetix.pro.core.ui.theme.ObsidianStroke
import com.kinetix.pro.core.ui.theme.ObsidianSurface1
import com.kinetix.pro.core.ui.theme.PrimarySunset

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        OnboardingStep(
            title = "Universal Grabber",
            subtitle = "Download at Full Fidelity",
            description = "Paste any link from YouTube, TikTok, Instagram, X, or Reddit. Pick 4K, 1080p, or extract direct 320kbps MP3 audio in seconds.",
            icon = Icons.Default.Download
        ),
        OnboardingStep(
            title = "On-Device CapCut Studio",
            subtitle = "Zero Server Rendering",
            description = "Hardware-accelerated editing: Split long videos into N equal parts, apply 40+ cinematic LUT filters, adjust colors, transform ratios with blurred backgrounds, and morph voices.",
            icon = Icons.Default.Movie
        ),
        OnboardingStep(
            title = "Reusable Brand Templates",
            subtitle = "Save Once, Apply Everywhere",
            description = "Capture your complete edit recipe into a lightweight template. Apply it across 10 clips or any future video with a single tap.",
            icon = Icons.Default.AutoAwesome
        )
    )

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Step Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            steps.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(4.dp)
                        .width(if (index == currentStepIndex) 36.dp else 12.dp)
                        .clip(CircleShape)
                        .background(if (index == currentStepIndex) PrimarySunset else ObsidianStroke)
                )
            }
        }

        // Center Graphic & Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurface1)
                    .border(2.dp, PrimarySunset, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = PrimarySunset,
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = step.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ContentHigh,
                textAlign = TextAlign.Center
            )

            Text(
                text = step.subtitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimarySunset,
                textAlign = TextAlign.Center
            )

            Text(
                text = step.description,
                fontSize = 13.sp,
                color = ContentMedium,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            if (currentStepIndex == steps.size - 1) {
                ObsidianInnerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Notice: Kinetix Pro operates strictly on-device for content you own or have permission to use. By continuing, you agree to respect copyright and platform terms.",
                        fontSize = 11.sp,
                        color = ContentMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SunsetPrimaryButton(
                text = if (currentStepIndex < steps.size - 1) "Next" else "Get Started",
                onClick = {
                    if (currentStepIndex < steps.size - 1) {
                        currentStepIndex++
                    } else {
                        onFinishOnboarding()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
