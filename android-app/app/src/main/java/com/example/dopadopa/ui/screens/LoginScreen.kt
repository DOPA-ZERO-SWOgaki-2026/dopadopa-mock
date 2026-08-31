package com.example.dopadopa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dopadopa.ui.theme.Theme

/** index.html の #loginScreen 相当。 */
@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
    var username by remember { mutableStateOf("") }

    fun submit() {
        if (username.trim().isNotEmpty()) onLogin(username)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.background)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.cardBackground, RoundedCornerShape(Theme.cardCorner))
                .padding(28.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Theme.ringGradient, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("D", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "WELCOME",
                color = Theme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
            )

            Text(
                text = "DopaDopa",
                color = Theme.ink,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
            )

            Text(
                text = "ユーザーネームを登録して、あなたのデトックス記録を開始しましょう。",
                color = Theme.subtleInk,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "ユーザーネーム",
                    color = Theme.subtleInk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("例: yuki") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Theme.controlCorner)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Theme.background,
                        unfocusedContainerColor = Theme.background,
                        focusedBorderColor = Theme.primary,
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.08f),
                        focusedTextColor = Theme.ink,
                        unfocusedTextColor = Theme.ink,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
            }

            Button(
                onClick = { submit() },
                enabled = username.trim().isNotEmpty(),
                shape = RoundedCornerShape(Theme.controlCorner),
                colors = ButtonDefaults.buttonColors(containerColor = Theme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp),
            ) {
                Text(
                    text = "アカウント作成",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
    }
}
