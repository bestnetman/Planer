package com.example.planer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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

enum class HomeScreen {
    PLANNER,
    SCENARIOS
}

data class UserScenario(
    val title: String,
    val persona: String,
    val goal: String,
    val steps: List<String>,
    val expectedOutcome: String
)

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
    val scenarios = remember {
        listOf(
            UserScenario(
                title = "아침 출근 준비",
                persona = "직장인 민지",
                goal = "출근 전에 필요한 앱과 문서를 빠르게 실행한다.",
                steps = listOf(
                    "일정 등록 화면에서 '출근 준비' 일정을 추가한다.",
                    "링크 타입을 패키지명으로 선택하고 메일 앱 패키지명을 입력한다.",
                    "등록된 일정 카드에서 '앱 실행'을 눌러 바로 실행한다."
                ),
                expectedOutcome = "정해진 시간에 맞춰 업무 앱을 즉시 열어 준비 시간을 줄인다."
            ),
            UserScenario(
                title = "회의 링크 즉시 접속",
                persona = "기획자 준호",
                goal = "반복 회의 URL을 일정과 함께 저장해 지각을 방지한다.",
                steps = listOf(
                    "회의 제목과 시작 시간을 입력한다.",
                    "링크 타입을 URL로 선택하고 회의 주소를 붙여넣는다.",
                    "회의 시작 직전에 카드의 '링크 열기'를 눌러 접속한다."
                ),
                expectedOutcome = "회의 링크를 찾는 시간을 줄이고 회의 참여를 빠르게 완료한다."
            ),
            UserScenario(
                title = "학습 루틴 관리",
                persona = "취업 준비생 서윤",
                goal = "하루 학습 일정을 시간순으로 정리해 루틴을 유지한다.",
                steps = listOf(
                    "여러 학습 일정을 날짜/시간과 함께 등록한다.",
                    "메모에 학습 목표나 체크포인트를 기록한다.",
                    "등록 목록을 시간순으로 확인하며 순서대로 실행한다."
                ),
                expectedOutcome = "일정 누락 없이 학습 루틴을 꾸준히 실천할 수 있다."
            )
        )
    }

    var selectedScreen by remember { mutableStateOf(HomeScreen.PLANNER) }
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
            Text("Planer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedScreen == HomeScreen.PLANNER,
                    onClick = { selectedScreen = HomeScreen.PLANNER },
                    label = { Text("일정 관리") }
                )
                FilterChip(
                    selected = selectedScreen == HomeScreen.SCENARIOS,
                    onClick = { selectedScreen = HomeScreen.SCENARIOS },
                    label = { Text("사용자 시나리오") }
                )
            }

            Spacer(Modifier.height(12.dp))
            if (selectedScreen == HomeScreen.PLANNER) {
                Text("일정 등록", style = MaterialTheme.typography.titleLarge)
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
            } else {
                Text("사용자 시나리오", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "실제 사용 흐름을 화면에서 바로 확인할 수 있도록 대표 시나리오를 구성했습니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(scenarios) { scenario ->
                        ScenarioCard(scenario = scenario)
                    }
                }
            }
        }
    }
}

@Composable
fun ScenarioCard(scenario: UserScenario) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(scenario.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("페르소나: ${scenario.persona}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text("목표", fontWeight = FontWeight.SemiBold)
            Text(scenario.goal, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(8.dp))
            Text("사용 단계", fontWeight = FontWeight.SemiBold)
            scenario.steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${index + 1}",
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(step, modifier = Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("기대 결과", fontWeight = FontWeight.SemiBold)
            Text(
                scenario.expectedOutcome,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1B5E20)
            )
        }
    }
}
