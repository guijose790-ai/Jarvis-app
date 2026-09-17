package com.guijose.jarvis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class ParticleView extends View {

    private ArrayList<Particula> particulas = new ArrayList<>();
    private Paint paint;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();
    private float nivelVoz = 0f;
    private boolean ouvindo = false;

    private static final int NUM_PARTICULAS = 60;

    private class Particula {
        float x, y, vx, vy, raio;
        Particula(float x, float y, float vx, float vy, float raio) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.raio = raio;
        }
    }

    public ParticleView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#4DA6FF"));

        post(this::inicializarParticulas);
        iniciarLoop();
    }

    private void inicializarParticulas() {
        int largura = getWidth() > 0 ? getWidth() : 1080;
        int altura = getHeight() > 0 ? getHeight() : 1920;

        particulas.clear();
        for (int i = 0; i < NUM_PARTICULAS; i++) {
            float x = random.nextFloat() * largura;
            float y = random.nextFloat() * altura;
            float vx = (random.nextFloat() - 0.5f) * 4f;
            float vy = (random.nextFloat() - 0.5f) * 4f;
            float raio = 4f + random.nextFloat() * 6f;
            particulas.add(new Particula(x, y, vx, vy, raio));
        }
    }

    public void setOuvindo(boolean valor) {
        ouvindo = valor;
        if (!valor) nivelVoz = 0f;
    }

    public void atualizarNivelVoz(float rms) {
        float normalizado = Math.max(0f, Math.min(1f, (rms + 2f) / 12f));
        nivelVoz = normalizado;
    }

    private void iniciarLoop() {
        Runnable loop = new Runnable() {
            @Override
            public void run() {
                atualizarParticulas();
                invalidate();
                handler.postDelayed(this, 16);
            }
        };
        handler.post(loop);
    }

    private void atualizarParticulas() {
        int largura = getWidth();
        int altura = getHeight();
        if (largura == 0 || altura == 0 || particulas.isEmpty()) return;

        float boost = 1f + (ouvindo ? nivelVoz * 5f : 0f);

        for (Particula p : particulas) {
            p.x += p.vx * boost;
            p.y += p.vy * boost;

            if (p.x < 0 || p.x > largura) p.vx *= -1;
            if (p.y < 0 || p.y > altura) p.vy *= -1;

            p.x = Math.max(0, Math.min(largura, p.x));
            p.y = Math.max(0, Math.min(altura, p.y));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float raioExtra = ouvindo ? nivelVoz * 6f : 0f;

        for (Particula p : particulas) {
            canvas.drawCircle(p.x, p.y, p.raio + raioExtra, paint);
        }
    }
}
