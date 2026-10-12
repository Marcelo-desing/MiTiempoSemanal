package com.senati.mitiemposemanal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Recuperamos el nombre de la tarea que enviamos al programar la alarma
        val nombreTarea = intent.getStringExtra("EXTRA_TAREA_NOMBRE") ?: "Tienes una tarea pendiente"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "canal_tareas_senati"

        // En Android 8+ es obligatorio crear un "Canal" de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones de Tareas",
                NotificationManager.IMPORTANCE_HIGH // Importancia alta para que suene y salga en pantalla
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Construimos el diseño de la notificación
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Icono por defecto de Android
            .setContentTitle("¡Hora de tu tarea!")
            .setContentText(nombreTarea)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Lanzamos la notificación (usamos un ID aleatorio para que no se sobreescriban si hay varias)
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}