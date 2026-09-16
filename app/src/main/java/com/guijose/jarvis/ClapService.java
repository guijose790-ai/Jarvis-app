package com.guijose.jarvis;

import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClapService extends Service {

    private static final String CANAL_ID = "jarvis_canal";
    private static final AtomicBoolean pausado = new AtomicBoolean(false);

    private SpeechRecognizer speechRecognizer;
    private Handler handler;
    private NotificationManager notificationManager;
    private boolean rodando = false;

    public static void pausarEscuta() {
        pausado.set(true);
    }

    public static void retomarEscuta() {
        pausado.set(false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        criarNotificacao("Aguardando 'Jarvis'...");
        handler = new Handler(Looper.getMainLooper());
        rodando = true;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && matches.size() > 0) {
                    String texto = matches.get(0).toLowerCase();
                    atualizarNotificacao("Ouvi: " + texto);
                    if (texto.contains("jarvis")) {
                        Intent intent = new Intent(ClapService.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("ativar_microfone", true);
                        startActivity(intent);
                    }
                }
                reiniciarEscuta();
            }

            @Override public void onError(int error) {
                reiniciarEscuta();
            }

            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        iniciarEscutaContinua();
    }

    private void iniciarEscutaContinua() {
        if (pausado.get() || !rodando) {
            handler.postDelayed(this::iniciarEscutaContinua, 500);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            handler.postDelayed(this::iniciarEscutaContinua, 1000);
        }
    }

    private void reiniciarEscuta() {
        handler.postDelayed(this::iniciarEscutaContinua, 300);
    }

    private void criarNotificacao(String texto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID, "Jarvis Ativo", NotificationManager.IMPORTANCE_LOW);
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(canal);
        }

        Notification notificacao = new NotificationCompat.Builder(this, CANAL_ID)
                .setContentTitle("Jarvis")
                .setContentText(texto)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build();

        startForeground(1, notificacao);
    }

    private void atualizarNotificacao(String texto) {
        if (notificationManager != null) {
            Notification notificacao = new NotificationCompat.Builder(this, CANAL_ID)
                    .setContentTitle("Jarvis")
                    .setContentText(texto)
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .build();
            notificationManager.notify(1, notificacao);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        rodando = false;
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
