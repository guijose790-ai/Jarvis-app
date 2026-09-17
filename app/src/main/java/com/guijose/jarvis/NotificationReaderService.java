package com.guijose.jarvis;

import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;

public class NotificationReaderService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        String pacote = sbn.getPackageName();

        if (pacote.equals("com.whatsapp") || pacote.equals("com.instagram.android")) {
            Bundle extras = sbn.getNotification().extras;
            String titulo = extras.getString("android.title", "");
            CharSequence textoSeq = extras.getCharSequence("android.text");
            String texto = textoSeq != null ? textoSeq.toString() : "";

            if (!titulo.isEmpty() && !texto.isEmpty()) {
                SharedPreferences memoria = getSharedPreferences("jarvis_memoria", MODE_PRIVATE);
                memoria.edit()
                        .putString("ultima_mensagem_remetente", titulo)
                        .putString("ultima_mensagem_texto", texto)
                        .putString("ultima_mensagem_app", pacote.contains("whatsapp") ? "WhatsApp" : "Instagram")
                        .apply();
            }
        }
    }
}
