package com.example.dopadopa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dopadopa.ui.theme.Theme

/** index.html の #goalModal 相当。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSheetSheet(
    currentGoalMinutes: Int,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var minutesText by remember { mutableStateOf(currentGoalMinutes.toString()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "目標時間を設定",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.ink,
            )

            Text(
                text = "目標時間（分）",
                fontSize = 13.sp,
                color = Theme.subtleInk,
            )

            OutlinedTextField(
                value = minutesText,
                onValueChange = { value -> if (value.all(Char::isDigit)) minutesText = value },
                placeholder = { Text("180") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val minutes = minutesText.toIntOrNull()
                        if (minutes != null && minutes > 0) onSave(minutes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Theme.primary),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("保存")
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("キャンセル")
                }
            }
        }
    }
}
