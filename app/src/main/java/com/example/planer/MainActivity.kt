package com.example.planer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

enum class LinkType {
    URL,
    PACKAGE
}

data class ScheduleItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dateTime: LocalDateTime,
    val note: String,
    val linkType: LinkType,
    val linkValue: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                ScheduleApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleApp() {
    val context = LocalContext.current
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    val snackbarHostState = remember { SnackbarHostState() }
    val schedules = remember { mutableStateListOf<ScheduleItem>() }

    var title by remember { mutableStateOf("") }
    var dateTimeText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var linkType by remember { mutableStateOf(LinkType.URL) }
    var linkValue by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("일정 등록", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("제목") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = dateTimeText,
                onValueChange = { dateTimeText = it },
                label = { Text("날짜/시간 (예: 2026-02-14 09:30)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("메모") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = linkType == LinkType.URL,
                    onClick = { linkType = LinkType.URL },
                    label = { Text("URL") }
                )
                FilterChip(
                    selected = linkType == LinkType.PACKAGE,
                    onClick = { linkType = LinkType.PACKAGE },
                    label = { Text("패키지명") }
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = linkValue,
                onValueChange = { linkValue = it },
                label = {
                    Text(if (linkType == LinkType.URL) "https://..." else "com.example.app")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (title.isBlank() || dateTimeText.isBlank() || linkValue.isBlank()) {
                        Toast.makeText(context, "필수값을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val parsedDateTime = try {
                        LocalDateTime.parse(dateTimeText, formatter)
                    } catch (_: DateTimeParseException) {
                        null
                    }

                    if (parsedDateTime == null) {
                        Toast.makeText(
                            context,
                            "날짜 형식이 올바르지 않습니다. yyyy-MM-dd HH:mm",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    schedules += ScheduleItem(
                        title = title.trim(),
                        dateTime = parsedDateTime,
                        note = note.trim(),
                        linkType = linkType,
                        linkValue = linkValue.trim()
                    )
                    schedules.sortBy { it.dateTime }

                    title = ""
                    dateTimeText = ""
                    note = ""
                    linkValue = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("일정 추가")
            }

            Spacer(Modifier.height(16.dp))
            Text("등록된 일정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = schedules, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(item.dateTime.format(formatter), style = MaterialTheme.typography.bodyMedium)
                            if (item.note.isNotBlank()) {
                                Text(item.note, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(8.dp))
                            AssistChip(
                                onClick = {
                                    when (item.linkType) {
                                        LinkType.URL -> {
                                            val url = if (item.linkValue.startsWith("http")) {
                                                item.linkValue
                                            } else {
                                                "https://${item.linkValue}"
                                            }
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        }

                                        LinkType.PACKAGE -> {
                                            val launchIntent = context.packageManager
                                                .getLaunchIntentForPackage(item.linkValue)
                                            if (launchIntent != null) {
                                                context.startActivity(launchIntent)
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "설치된 앱을 찾을 수 없습니다.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        if (item.linkType == LinkType.URL) {
                                            "링크 열기"
                                        } else {
                                            "앱 실행"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
