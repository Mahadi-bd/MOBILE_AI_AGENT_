package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.VerifyResult
import com.example.ui.KeyCheckState
import com.example.ui.MahadiViewModel
import com.example.ui.theme.CyberBackgroundDark
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.neonCard

/** Where every user enters their OWN Gemini API key (nothing is built into the app). */
@Composable
fun AiSettingsCard(viewModel: MahadiViewModel, modifier: Modifier = Modifier) {
    val hasKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    val check by viewModel.keyCheck.collectAsStateWithLifecycle()
    val speakAll by viewModel.speakAll.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    var keyInput by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .neonCard(NeonCyan, cornerRadius = 26.dp, glow = 10.dp)
            .padding(16.dp)
            .testTag("ai_settings_card")
    ) {
        Text(text = "AI সেটিং", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "এই অ্যাপে কোনো AI key দেওয়া নেই। আপনার নিজের Google Gemini API key দিন (ফ্রি পাওয়া যায়)। " +
                "Key টি এনক্রিপ্ট করে শুধু আপনার ফোনে রাখা হয় এবং শুধু Google এর কাছে পাঠানো হয়।",
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (hasKey) "✓ আপনার API key সংরক্ষিত আছে" else "✗ এখনো কোনো API key নেই (AI চলবে না)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (hasKey) NeonGreen else NeonMagenta
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("Gemini API key (AIza...)", color = TextSecondary, fontSize = 11.sp) },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Text(
                    text = if (visible) "লুকান" else "দেখান",
                    fontSize = 11.sp,
                    color = NeonCyan,
                    modifier = Modifier
                        .clickable { visible = !visible }
                        .padding(8.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CyberSurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api_key_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    viewModel.saveApiKey(keyInput)
                    keyInput = ""
                    visible = false
                },
                enabled = keyInput.isNotBlank() && check !is KeyCheckState.Checking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = CyberBackgroundDark,
                    disabledContainerColor = Color(0x3322D3EE)
                ),
                modifier = Modifier.testTag("save_api_key_button")
            ) {
                Text(text = "সংরক্ষণ ও যাচাই", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            if (hasKey) {
                Text(
                    text = "Key মুছুন",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonMagenta,
                    modifier = Modifier
                        .clickable { viewModel.clearApiKey() }
                        .padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (val state = check) {
            is KeyCheckState.Idle -> {}
            is KeyCheckState.Checking -> Text("যাচাই করা হচ্ছে...", fontSize = 12.sp, color = NeonCyan)
            is KeyCheckState.Done -> Text(
                text = state.message,
                fontSize = 12.sp,
                color = when (state.status) {
                    VerifyResult.Status.OK -> NeonGreen
                    VerifyResult.Status.INVALID -> NeonMagenta
                    else -> TextSecondary
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "ফ্রি API key কীভাবে পাবেন:  ১) নিচের লিংক খুলুন  ২) Google অ্যাকাউন্ট দিয়ে লগইন  ৩) \"Create API key\" চাপুন  ৪) key কপি করে এখানে paste করুন।",
            fontSize = 11.sp,
            color = TextSecondary,
            lineHeight = 16.sp
        )
        Text(
            text = "Google AI Studio খুলুন ›",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = NeonPurple,
            modifier = Modifier
                .clickable { uriHandler.openUri("https://aistudio.google.com/apikey") }
                .padding(vertical = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "প্রতিটি উত্তর জোরে পড়ে শোনাও", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(text = "বন্ধ থাকলে শুধু ভয়েসে প্রশ্ন করলে বা বাবলে চাপলে শোনাবে।", fontSize = 11.sp, color = TextSecondary)
            }
            Switch(
                checked = speakAll,
                onCheckedChange = { viewModel.setSpeakAll(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonCyan,
                    checkedTrackColor = NeonCyan.copy(alpha = 0.35f)
                )
            )
        }
    }
}
