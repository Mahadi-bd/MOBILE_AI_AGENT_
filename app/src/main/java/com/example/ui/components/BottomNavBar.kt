package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppTab
import com.example.ui.theme.CyberBackgroundDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

/** Floating neon pill navigation with a raised glowing AI button in the middle. */
@Composable
fun CyberBottomNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(34.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp)
            .testTag("cyber_bottom_nav_bar")
    ) {
        // The pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 8.dp)
                .shadow(14.dp, pillShape, clip = false, ambientColor = NeonCyan, spotColor = NeonCyan)
                .clip(pillShape)
                .background(Color(0xEE08111E))
                .border(
                    1.5.dp,
                    Brush.horizontalGradient(listOf(NeonCyan, NeonPurple, NeonCyan)),
                    pillShape
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabItem("Home", Icons.Default.Home, NeonCyan, selectedTab == AppTab.HUD_HOME) { onTabSelected(AppTab.HUD_HOME) }
            NavTabItem("Chat", Icons.Default.ChatBubbleOutline, NeonPurple, selectedTab == AppTab.CHAT) { onTabSelected(AppTab.CHAT) }
            Spacer(modifier = Modifier.width(74.dp)) // room for the raised AI button
            NavTabItem("Tools", Icons.Default.GridView, NeonGreen, selectedTab == AppTab.TOOLS) { onTabSelected(AppTab.TOOLS) }
            NavTabItem("Persona", Icons.Default.PersonOutline, NeonAmber, selectedTab == AppTab.PERSONA) { onTabSelected(AppTab.PERSONA) }
        }

        // Raised center AI button: glowing double ring
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(76.dp)
                .shadow(16.dp, CircleShape, clip = false, ambientColor = NeonCyan, spotColor = NeonCyan)
                .clip(CircleShape)
                .background(CyberBackgroundDark)
                .border(2.5.dp, Brush.sweepGradient(listOf(NeonCyan, NeonPurple, NeonBlue, NeonCyan)), CircleShape)
                .padding(7.dp)
                .border(1.5.dp, NeonCyan.copy(alpha = 0.7f), CircleShape)
                .clickable { onTabSelected(AppTab.CHAT) }
                .testTag("center_ai_button"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .border(1.5.dp, NeonCyan, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "AI", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextWhite)
            }
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    icon: ImageVector,
    accent: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isSelected) accent else accent.copy(alpha = 0.55f)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) accent else TextMuted
        )
    }
}
