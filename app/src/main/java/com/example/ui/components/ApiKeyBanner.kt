package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.neonCard

/** Shown on Home and Chat until the user has added their own Gemini API key. */
@Composable
fun ApiKeyBanner(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .neonCard(NeonAmber, cornerRadius = 22.dp, glow = 8.dp)
            .clickable(onClick = onOpenSettings)
            .padding(14.dp)
            .testTag("api_key_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI চালু করুন",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "সেটিং খুলুন ›",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonAmber
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "চ্যাট ও ভয়েসের জন্য আপনার নিজের (ফ্রি) Google Gemini API key লাগবে। Persona ট্যাবে গিয়ে key দিন।",
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}
