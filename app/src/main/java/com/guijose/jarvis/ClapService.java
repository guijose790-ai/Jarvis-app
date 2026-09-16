package com.guijose.jarvis;

import android.app.*;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClapService extends Service {

    private boolean escutando = false;
    private static final int SAMPLE_RATE = 44100;
    private static final int THRESHOLD = 3000;
    private NotificationManager notificationManager;
    private static final String CANAL_ID = "jarvis_canal";
    private static final AtomicBoolean pausado = new AtomicBoolean(false);

    public static void pausarEscuta() {
        pausado.set(true);
    }

    public static void retomarEscuta() {
        pausado.set(false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        criarNotificacao("Iniciando...");
        escutando = true;
        new Thread(this::monitorarSom).start();
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

    private void monitorarSom() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize <= 0) {
            atualizarNotificacao("Erro: bufferSize invalido");
            return;
        }

        AudioRecord recorder = null;
        short[] buffer = new short[bufferSize];
        long ultimaAtualizacao = 0;
        long maiorMedia = 0;

        while (escutando) {

            if (pausado.get()) {
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                    recorder = null;
                    atualizarNotificacao("Pausado (usando microfone)");
                }
                try { Thread.sleep(300); } catch (Exception ignored) {}
                continue;
            }

            if (recorder == null) {
                try {
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                    if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                        atualizarNotificacao("Erro: AudioRecord nao iniciou");
                        recorder = null;
                        try { Thread.sleep(1000); } catch (Exception ignored) {}
                        continue;
                    }
                    recorder.startRecording();
                } catch (SecurityException e) {
                    atualizarNotificacao("Erro: sem permissao de microfone");
                    return;
                }
            }

            int lidos = recorder.read(buffer, 0, bufferSize);
            long soma = 0;
            for (int i = 0; i < lidos; i++) {
                soma += Math.abs(buffer[i]);
            }
            long media = lidos > 0 ? soma / lidos : 0;

            if (media > maiorMedia) {
                maiorMedia = media;
            }

            long agora = System.currentTimeMillis();
            if (agora - ultimaAtualizacao > 1000) {
                atualizarNotificacao("Nivel atual: " + media + " | Pico: " + maiorMedia);
                ultimaAtualizacao = agora;
            }

            if (media > THRESHOLD) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("ativar_microfone", true);
                startActivity(intent);
                try { Thread.sleep(3000); } catch (Exception ignored) {}
            }
        }

        if (recorder != null) {
            recorder.stop();
            recorder.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        escutando = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
