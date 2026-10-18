package com.example.calc760

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalcScreen() }
    }
}

private val LCD = Color(0xFFB9C5A7)
private val INK = Color(0xFF1B1F16)
private val BODY = Color(0xFF3A3C40)
private val BLACK_KEY = Color(0xFF17181A)
private val GRAY_KEY = Color(0xFF6E7279)
private val RED_KEY = Color(0xFFD01C1C)

// 6 أعمدة × 6 صفوف (الصف الأول يحتوي المفاتيح العلوية، "+" طويل بصفّين)
private val COLS = listOf(
    listOf("", "GT", "MU", "±", "CE", "AC"),
    listOf("", "M+", "7", "4", "1", "0"),
    listOf("", "M-", "8", "5", "2", "00"),
    listOf("CORRECT", "MRC", "9", "6", "3", "."),
    listOf("REPLAY", "CHK<", "%", "×", "+"),
    listOf("OFF", "CHK>", "√", "÷", "-", "=")
)

private fun label(k: String) = when (k) {
    "±" -> "+/−"
    "AC" -> "ON\nAC"
    "-" -> "−"
    "CHK<" -> "◀\nCHECK"
    "CHK>" -> "CHECK\n▶"
    "REPLAY" -> "AUTO\nREPLAY"
    else -> k
}

@Composable
fun CalcScreen() {
    val engine = remember { CalcEngine() }
    val ui = engine.ui
    val haptic = LocalHapticFeedback.current

    Column(
        Modifier.fillMaxSize().background(BODY).systemBarsPadding().padding(10.dp)
    ) {
        // الشاشة
        Box(
            Modifier.fillMaxWidth().height(120.dp)
                .clip(RoundedCornerShape(8.dp)).background(LCD).padding(10.dp)
        ) {
            if (ui.on) {
                Row(Modifier.align(Alignment.TopStart), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (ui.memory) Text("M", color = INK, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (ui.gt) Text("GT", color = INK, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (ui.error) Text("ERROR", color = INK, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (ui.tag.isNotEmpty()) Text(ui.tag, color = INK, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    ui.text,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    color = INK, fontSize = 38.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    maxLines = 1, textAlign = TextAlign.End
                )
            }
        }

        // الشريط: لوحة شمسية + نص
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(70.dp).height(22.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF5B4326)))
            Spacer(Modifier.width(10.dp))
            Text("112 CHECK & CORRECT · 12 DIGITS", color = Color(0xFFCCCCCC), fontSize = 11.sp)
        }

        // لوحة المفاتيح
        Row(Modifier.fillMaxWidth().weight(1f)) {
            COLS.forEach { col ->
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    col.forEach { k ->
                        if (k.isEmpty()) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            Key(k, if (k == "+") 2f else 1f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                engine.press(k)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Key(k: String, w: Float, onClick: () -> Unit) {
    val bg = when {
        k == "AC" -> RED_KEY
        k.length == 1 && k[0].isDigit() || k == "00" || k == "." -> BLACK_KEY
        else -> GRAY_KEY
    }
    Box(
        Modifier.weight(w).fillMaxWidth().padding(3.dp)
            .clip(RoundedCornerShape(14.dp)).background(bg).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val big = k.length <= 2 && !k.startsWith("M") && k != "AC"
        Text(
            label(k), color = Color.White,
            fontSize = if (big) 24.sp else 11.sp,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
        )
    }
}
