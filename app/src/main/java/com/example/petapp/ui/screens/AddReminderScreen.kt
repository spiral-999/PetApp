package com.example.petapp.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.petapp.data.PetRepository
import com.example.petapp.data.SettingsDataStore
import com.example.petapp.data.model.Priority
import com.example.petapp.data.model.Reminder
import com.example.petapp.notifications.AlarmScheduler
import com.example.petapp.notifications.NotificationHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(petId: Int, navController: NavController) {
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var showError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scheduler = AlarmScheduler(context)
    val coroutineScope = rememberCoroutineScope()
    val settingsDataStore = SettingsDataStore(context)
    val notificationsEnabled by settingsDataStore.notificationsEnabled.collectAsState(initial = true)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // --- LÓGICA DE PERMISSÕES ---
    // Verificador da permissão de notificação (Passo 1)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                Toast.makeText(context, "Sem a permissão, as notificações não serão exibidas.", Toast.LENGTH_LONG).show()
            }
        }
    )

    // ... (código dos date pickers, que não muda)
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val datePickerDialog = DatePickerDialog( context, { _, year, month, dayOfMonth -> selectedDate = LocalDate.of(year, month + 1, dayOfMonth) }, selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth )
    val timePickerDialog = TimePickerDialog( context, { _, hour, minute -> selectedTime = LocalTime.of(hour, minute) }, selectedTime.hour, selectedTime.minute, true )


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Lembrete") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... (código da UI que não muda: OutlinedTextField, Row de botões, Rádios de prioridade)
            OutlinedTextField( value = title, onValueChange = { title = it }, label = { Text("Título do Lembrete") }, modifier = Modifier.fillMaxWidth(), isError = showError && title.isBlank() )
            if (showError && title.isBlank()) { Text("O título é obrigatório", color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(16.dp))
            Row( modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround ) {
                Button(onClick = { datePickerDialog.show() }) { Text("Data: ${selectedDate.format(dateFormatter)}") }
                Button(onClick = { timePickerDialog.show() }) { Text("Hora: ${selectedTime.format(timeFormatter)}") }
            }
            Spacer(Modifier.height(24.dp))
            Text("Prioridade", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton( selected = priority == Priority.LOW, onClick = { priority = Priority.LOW } )
                Text("Baixa", Modifier.clickable { priority = Priority.LOW })
                Spacer(Modifier.width(16.dp))
                RadioButton( selected = priority == Priority.MEDIUM, onClick = { priority = Priority.MEDIUM } )
                Text("Média", Modifier.clickable { priority = Priority.MEDIUM })
                Spacer(Modifier.width(16.dp))
                RadioButton( selected = priority == Priority.HIGH, onClick = { priority = Priority.HIGH } )
                Text("Alta", Modifier.clickable { priority = Priority.HIGH })
            }
            Spacer(Modifier.height(32.dp))


            Button(
                onClick = {
                    if (title.isBlank()) {
                        showError = true
                        return@Button
                    }
                    if (!notificationsEnabled) {
                        Toast.makeText(context, "Ative as notificações nas configurações para agendar.", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    // --- INÍCIO DO FLUXO GUIADO DE PERMISSÕES ---

                    // PASSO 1: Checar e pedir a permissão de NOTIFICAÇÃO (o pop-up)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val notificationPermissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        if (notificationPermissionStatus != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@Button // Para a execução e espera a resposta do usuário
                        }
                    }

                    // PASSO 2: Checar e pedir a permissão de ALARME (a especial)
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Toast.makeText(context, "Agora, por favor, ative a permissão para 'Alarmes e lembretes' para finalizar.", Toast.LENGTH_LONG).show()
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                            context.startActivity(it)
                        }
                        return@Button // Para a execução e espera a ação do usuário nas configurações
                    }

                    // PASSO 3: Se todas as permissões estiverem OK, agendar o lembrete
                    coroutineScope.launch {
                        val dateTime = selectedDate.atTime(selectedTime)
                        val newReminder = Reminder(title = title, dateTime = dateTime, priority = priority)
                        PetRepository.addReminderToPet(petId, newReminder)
                        val channelId = when (priority) {
                            Priority.HIGH -> NotificationHelper.HIGH_PRIORITY_CHANNEL_ID
                            Priority.MEDIUM -> NotificationHelper.MEDIUM_PRIORITY_CHANNEL_ID
                            Priority.LOW -> NotificationHelper.LOW_PRIORITY_CHANNEL_ID
                        }
                        scheduler.schedule(
                            reminderId = newReminder.id,
                            time = dateTime,
                            title = "Lembrete para o seu Pet!",
                            message = title,
                            channelId = channelId
                        )
                        Toast.makeText(context, "Lembrete salvo e agendado!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar Lembrete")
            }
        }
    }
}