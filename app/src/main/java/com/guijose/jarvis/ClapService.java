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

public class ClapService extends Service {

    private boolean escutando = false;
    private static final int SAMPLE_RATE = 44100;
    private static final int THRESHOLD = 15000;

    @Override
    public void onCreate() {
        super.onCreate();
        criarNotificacao();
        escutando = true;
        new Thread(this::monitorarSom).start();
    }

    private void criarNotificacao() {
        String canalId = "jarvis_canal";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    canalId, "Jarvis Ativo", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(canal);
        }

        Notification notificacao = new NotificationCompat.Builder(this, canalId)
                .setContentTitle("Jarvis")
                .setContentText("Escutando por palmas...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build();

        startForeground(1, notificacao);
    }

    private void monitorarSom() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        short[] buffer = new short[bufferSize];
        recorder.startRecording();

        while (escutando) {
            int lidos = recorder.read(buffer, 0, bufferSize);
            long soma = 0;
            for (int i = 0; i < lidos; i++) {
                soma += Math.abs(buffer[i]);
            }
            long media = lidos > 0 ? soma / lidos : 0;

            if (media > THRESHOLD) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("ativar_microfone", true);
                startActivity(intent);
                try { Thread.sleep(2000); } catch (Exception ignored) {}
            }
        }

        recorder.stop();
        recorder.release();
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
