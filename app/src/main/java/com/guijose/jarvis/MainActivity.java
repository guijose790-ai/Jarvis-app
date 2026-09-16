package com.guijose.jarvis;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Button ouvirButton = new Button(this);
            ouvirButton.setText("Ouvir");
            setContentView(ouvirButton);

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.POST_NOTIFICATIONS}, 1);

            textToSpeech = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(new Locale("pt", "BR"));
                }
            });

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && matches.size() > 0) {
                        processCommand(matches.get(0).toLowerCase());
                    }
                }

                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) {}
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });

            ouvirButton.setOnClickListener(v -> iniciarEscuta());

            Intent serviceIntent = new Intent(this, ClapService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            if (getIntent().getBooleanExtra("ativar_microfone", false)) {
                iniciarEscuta();
            }

        } catch (Exception e) {
            TextView erro = new TextView(this);
            erro.setText("ERRO: " + e.toString());
            erro.setTextColor(Color.RED);
            erro.setPadding(20, 100, 20, 20);
            setContentView(erro);
        }
    }

    private void iniciarEscuta() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        speechRecognizer.startListening(intent);
    }

    private void processCommand(String texto) {
        if (texto.contains("google")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("https://www.google.com"));
            startActivity(intent);
            falar("Abrindo o Google");
        } else if (texto.contains("wifi") || texto.contains("wi-fi")) {
            Intent wifiIntent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
            startActivity(wifiIntent);
            falar("Abrindo o Wi-Fi");
        } else if (texto.contains("bluetooth")) {
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            falar("Abrindo o Bluetooth");
        } else {
            falar("Não entendi o comando: " + texto);
        }
    }

    private void falar(String texto) {
        textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
    }
}
