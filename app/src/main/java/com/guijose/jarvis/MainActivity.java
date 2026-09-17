package com.guijose.jarvis;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private ParticleView particleView;
    private boolean textToSpeechPronto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            particleView = new ParticleView(this);
            setContentView(particleView);

            textToSpeech = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(new Locale("pt", "BR"));
                    textToSpeech.setPitch(0.6f);
                    textToSpeech.setSpeechRate(0.9f);

                    for (Voice voz : textToSpeech.getVoices()) {
                        if (voz.getLocale().getLanguage().equals("pt")
                                && (voz.getName().toLowerCase().contains("male")
                                    || voz.getName().toLowerCase().contains("masculin"))) {
                            textToSpeech.setVoice(voz);
                            break;
                        }
                    }

                    textToSpeechPronto = true;

                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override public void onStart(String utteranceId) {}
                        @Override public void onError(String utteranceId) {}

                        @Override
                        public void onDone(String utteranceId) {
                            if ("saudacao".equals(utteranceId)) {
                                iniciarEscuta();
                            }
                        }
                    });

                    if (getIntent().getBooleanExtra("ativar_microfone", false)) {
                        saudarEEscutar();
                    }
                } else {
                    Toast.makeText(MainActivity.this,
                            "Erro ao iniciar TextToSpeech: " + status, Toast.LENGTH_LONG).show();
                }
            });

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    particleView.setOuvindo(true);
                }

                @Override
                public void onResults(Bundle results) {
                    particleView.setOuvindo(false);
                    ClapService.retomarEscuta();
                    ArrayList<String> matches = results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && matches.size() > 0) {
                        processCommand(matches.get(0).toLowerCase());
                    }
                }

                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}

                @Override public void onError(int error) {
                    particleView.setOuvindo(false);
                    ClapService.retomarEscuta();
                }

                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });

            particleView.setOnClickListener(v -> iniciarEscuta());

            verificarPermissoesEIniciarServico();

        } catch (Exception e) {
            TextView erro = new TextView(this);
            erro.setText("ERRO: " + e.toString());
            erro.setTextColor(Color.RED);
            erro.setPadding(20, 100, 20, 20);
            setContentView(erro);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("ativar_microfone", false) && textToSpeechPronto) {
            saudarEEscutar();
        }
    }

    private void saudarEEscutar() {
        ClapService.pausarEscuta();
        HashMap<String, String> params = new HashMap<>();
        params.put(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "saudacao");
        textToSpeech.speak("O que deseja, senhor Guilherme?", TextToSpeech.QUEUE_FLUSH, params);
    }

    private void verificarPermissoesEIniciarServico() {
        boolean temAudio = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        if (temAudio) {
            iniciarServico();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarServico();
        }
    }

    private void iniciarServico() {
        Intent serviceIntent = new Intent(this, ClapService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void iniciarEscuta() {
        ClapService.pausarEscuta();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        speechRecognizer.startListening(intent);
    }

    private void processCommand(String texto) {
        SharedPreferences memoria = getSharedPreferences("jarvis_memoria", MODE_PRIVATE);

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

        } else if (texto.contains("aprenda que")) {
            String resto = texto.substring(texto.indexOf("aprenda que") + "aprenda que".length()).trim();
            if (resto.contains(" é ")) {
                String[] partes = resto.split(" é ", 2);
                String chave = partes[0].trim();
                String valor = partes[1].trim();
                memoria.edit().putString(chave, valor).apply();
                falar("Entendido. Vou lembrar que " + chave + " é " + valor);
            } else {
                falar("Não entendi. Fale assim: aprenda que alguma coisa é outra coisa.");
            }

        } else if (texto.startsWith("o que é") || texto.startsWith("o que e")) {
            String chave = texto.replaceFirst("o que (é|e)", "").trim();
            String valor = memoria.getString(chave, null);
            if (valor != null) {
                falar(chave + " é " + valor);
            } else {
                falar("Ainda não sei o que é " + chave);
            }

        } else {
            falar("Não entendi o comando: " + texto);
        }
    }

    private void falar(String texto) {
        textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
    }
}
