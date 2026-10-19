package ru.embtlab.smartprice.presentation.scanner.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScannerNumPad(
    show99Button: Boolean,
    onKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Ряд 0-9
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { digit ->
                FilledTonalButton(
                    onClick = { onKeyClick(digit) },
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF3A3A3C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .padding(horizontal = 1.5.dp)
                ) {
                    Text(digit, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Модификаторы
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { onKeyClick(".") },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF3A3A3C), contentColor = Color.White),
                modifier = Modifier.weight(1f).height(36.dp)
            ) {
                Text("• Точка", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            if (show99Button) {
                FilledTonalButton(
                    onClick = { onKeyClick(".99") },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF3A3A3C), contentColor = Color.White),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Text(".99", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            FilledTonalButton(
                onClick = { onKeyClick("⌫") },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF3A3A3C), contentColor = Color.White),
                modifier = Modifier.weight(1f).height(36.dp)
            ) {
                Text("⌫ Стереть", fontSize = 13.sp)
            }

            FilledTonalButton(
                onClick = { onKeyClick("C") },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF48484A), contentColor = Color.White),
                modifier = Modifier.weight(0.9f).height(36.dp)
            ) {
                Text("Очистить", fontSize = 12.sp)
            }
        }
    }
}