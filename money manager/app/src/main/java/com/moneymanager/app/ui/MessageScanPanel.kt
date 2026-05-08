package com.moneymanager.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.model.MessageScanRange
import com.moneymanager.app.ui.theme.LossRed
import com.moneymanager.app.ui.theme.Navy900
import com.moneymanager.app.ui.theme.PrimaryBlue
import com.moneymanager.app.ui.theme.TextMuted
import com.moneymanager.app.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MessageScanPanel(onScan: (MessageScanRange, LocalDate, LocalDate) -> Unit) {
    val today = LocalDate.now()
    var selectedRange by remember { mutableStateOf(MessageScanRange.Today) }
    var startDate by remember { mutableStateOf(today.minusDays(6)) }
    var endDate by remember { mutableStateOf(today) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val isCustomValid = selectedRange != MessageScanRange.Custom || !startDate.isAfter(endDate)
    val buttonLabel = when (selectedRange) {
        MessageScanRange.Today -> "Scan Today"
        MessageScanRange.Yesterday -> "Scan Yesterday"
        MessageScanRange.Week -> "Scan Last 7 Days"
        MessageScanRange.Custom -> "Scan Range"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy900)
            .padding(16.dp)
    ) {
        Text("Message Scanner", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MessageScanRange.values().forEach { range ->
                FilterChip(
                    selected = selectedRange == range,
                    onClick = { selectedRange = range },
                    label = { Text(range.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = if (selectedRange == range) PrimaryBlue else Navy900,
                        labelColor = if (selectedRange == range) Color.White else TextMuted
                    )
                )
            }
        }
        if (selectedRange == MessageScanRange.Custom) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start: ${formatter.format(startDate)}")
                }
                OutlinedButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("End: ${formatter.format(endDate)}")
                }
            }
            if (!isCustomValid) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Start date must be on or before end date.", color = LossRed)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = { onScan(selectedRange, startDate, endDate) },
            enabled = selectedRange != MessageScanRange.Custom || isCustomValid,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue,
                contentColor = Color(0xFF001A42)
            )
        ) {
            Text(buttonLabel, fontWeight = FontWeight.Bold)
        }
    }

    if (showStartPicker) {
        val context = LocalContext.current
        LaunchedEffect(showStartPicker) {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selected = LocalDate.of(year, month + 1, dayOfMonth)
                    startDate = selected
                    if (startDate.isAfter(endDate)) {
                        endDate = startDate
                    }
                    showStartPicker = false
                },
                startDate.year,
                startDate.monthValue - 1,
                startDate.dayOfMonth
            ).apply {
                datePicker.maxDate = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                show()
            }
        }
    }

    if (showEndPicker) {
        val context = LocalContext.current
        LaunchedEffect(showEndPicker) {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    endDate = LocalDate.of(year, month + 1, dayOfMonth)
                    if (endDate.isAfter(today)) {
                        endDate = today
                    }
                    if (startDate.isAfter(endDate)) {
                        startDate = endDate
                    }
                    showEndPicker = false
                },
                endDate.year,
                endDate.monthValue - 1,
                endDate.dayOfMonth
            ).apply {
                datePicker.maxDate = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                show()
            }
        }
    }
}
