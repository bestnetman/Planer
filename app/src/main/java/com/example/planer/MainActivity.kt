package com.example.planer

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

const val CHANNEL_ID = "planner_reminders"

class MainActivity : ComponentActivity() {
    private val alarmScheduler by lazy { ReminderScheduler(this) }
    private val appViewModel by viewModels<PlannerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createReminderChannel()
        enableEdgeToEdge()
        setContent {
            PlannerApp(
                viewModel = appViewModel,
                onScheduleReminder = { event -> alarmScheduler.schedule(event) }
            )
        }
    }

    private fun createReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "일정 리마인더",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "일정 시작 전 알림을 전송합니다."
            }
            manager.createNotificationChannel(channel)
        }
    }
}

data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: LocalDate,
    val time: LocalTime,
    val owner: String,
    val participants: List<String>,
    val reminderMinutes: Long,
    val isShared: Boolean
) {
    val startsAt: LocalDateTime = LocalDateTime.of(date, time)
}

class PlannerViewModel : ViewModel() {
    private val _events = MutableStateFlow(
        listOf(
            CalendarEvent(
                title = "스프린트 플래닝",
                date = LocalDate.now(),
                time = LocalTime.of(10, 0),
                owner = "나",
                participants = listOf("디자인팀", "개발팀"),
                reminderMinutes = 30,
                isShared = true
            ),
            CalendarEvent(
                title = "개인 운동",
                date = LocalDate.now().plusDays(1),
                time = LocalTime.of(19, 30),
                owner = "나",
                participants = emptyList(),
                reminderMinutes = 15,
                isShared = false
            )
        )
    )

    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    fun addEvent(event: CalendarEvent) {
        _events.value = (_events.value + event).sortedBy { it.startsAt }
    }
}

enum class PlannerTab(val label: String) {
    SCHEDULE("일정"),
    SHARED("공유"),
    ALERTS("알림")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerApp(
    viewModel: PlannerViewModel = viewModel(),
    onScheduleReminder: (CalendarEvent) -> Unit
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(PlannerTab.SCHEDULE) }
    var showForm by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Planer · 팀 캘린더") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showForm = !showForm }) {
                Icon(Icons.Default.Add, contentDescription = "일정 추가")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                PlannerTab.entries.forEach { tab ->
                    val icon = when (tab) {
                        PlannerTab.SCHEDULE -> Icons.Default.Today
                        PlannerTab.SHARED -> Icons.Default.People
                        PlannerTab.ALERTS -> Icons.Default.Notifications
                    }
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (showForm) {
                AddEventForm(
                    onSubmit = { event ->
                        viewModel.addEvent(event)
                        onScheduleReminder(event)
                        showForm = false
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            when (selectedTab) {
                PlannerTab.SCHEDULE -> EventList(
                    events = events,
                    emptyMessage = "등록된 일정이 없어요. + 버튼으로 일정을 추가하세요."
                )

                PlannerTab.SHARED -> EventList(
                    events = events.filter { it.isShared },
                    emptyMessage = "공유 일정이 없습니다. 참여자를 추가해 보세요."
                )

                PlannerTab.ALERTS -> AlertsList(events)
            }
        }
    }
}

@Composable
private fun EventList(events: List<CalendarEvent>, emptyMessage: String) {
    if (events.isEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(emptyMessage)
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(events, key = { it.id }) { event ->
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(event.title, fontWeight = FontWeight.Bold)
                    Text("${event.date} ${event.time}")
                    if (event.participants.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            event.participants.take(3).forEach {
                                AssistChip(onClick = {}, label = { Text(it) })
                            }
                        }
                    }
                    Text("알림 ${event.reminderMinutes}분 전")
                }
            }
        }
    }
}

@Composable
private fun AlertsList(events: List<CalendarEvent>) {
    val formatter = remember { DateTimeFormatter.ofPattern("M월 d일 HH:mm") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(events, key = { "alert-${it.id}" }) { event ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = event.title, fontWeight = FontWeight.SemiBold)
                    Text(text = "예정 시각: ${event.startsAt.format(formatter)}")
                    Text(text = "리마인더: ${event.reminderMinutes}분 전 푸시")
                }
            }
        }
    }
}

@Composable
private fun AddEventForm(onSubmit: (CalendarEvent) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var timeText by rememberSaveable { mutableStateOf("09:00") }
    var participantsText by rememberSaveable { mutableStateOf("") }
    var reminderText by rememberSaveable { mutableStateOf("30") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("새 일정", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("제목") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("날짜 (yyyy-MM-dd)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("시간 (HH:mm)") },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = participantsText,
                onValueChange = { participantsText = it },
                label = { Text("참여자 (쉼표로 구분)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = reminderText,
                onValueChange = { reminderText = it.filter(Char::isDigit) },
                label = { Text("알림(분 전)") },
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = {
                    val safeTitle = title.ifBlank { "새 일정" }
                    val date = runCatching { LocalDate.parse(dateText) }.getOrElse { LocalDate.now() }
                    val time = runCatching { LocalTime.parse(timeText) }.getOrElse { LocalTime.of(9, 0) }
                    val participants = participantsText
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    onSubmit(
                        CalendarEvent(
                            title = safeTitle,
                            date = date,
                            time = time,
                            owner = "나",
                            participants = participants,
                            reminderMinutes = reminderText.toLongOrNull() ?: 30,
                            isShared = participants.isNotEmpty()
                        )
                    )
                },
                modifier = Modifier.align(androidx.compose.ui.Alignment.End)
            ) {
                Text("추가")
            }
        }
    }
}

class ReminderScheduler(private val context: Context) {
    fun schedule(event: CalendarEvent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = event.startsAt
            .minusMinutes(event.reminderMinutes)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", event.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }
}
