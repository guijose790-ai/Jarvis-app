package com.guijose.jarvis;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ParticleView extends View {

    private float anguloBase = 0f;
    private boolean ouvindo = false;
    private ValueAnimator animator;
    private Paint paintParticula;
    private Paint paintNucleo;
    private static final int NUM_PARTICULAS = 24;

    public ParticleView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);

        paintParticula = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintParticula.setColor(Color.parseColor("#FFC94D"));

        paintNucleo = new Paint(Paint.ANTI_ALIAS_FLAG);

        animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(6000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(anim -> {
            anguloBase = (float) anim.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void setOuvindo(boolean valor) {
        ouvindo = valor;
        animator.setDuration(ouvindo ? 1500 : 6000);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float raioBase = Math.min(getWidth(), getHeight()) / 4f;

        int corGlow = ouvindo ? Color.parseColor("#FFD966") : Color.parseColor("#FFC94D");

        RadialGradient gradient = new RadialGradient(cx, cy, raioBase * 1.4f,
                new int[]{corGlow, Color.TRANSPARENT},
                null, Shader.TileMode.CLAMP);
        paintNucleo.setShader(gradient);
        canvas.drawCircle(cx, cy, raioBase * 1.4f, paintNucleo);

        paintParticula.setColor(corGlow);
        canvas.drawCircle(cx, cy, raioBase * 0.35f, paintParticula);

        for (int i = 0; i < NUM_PARTICULAS; i++) {
            float anguloParticula = (float) (anguloBase + (360f / NUM_PARTICULAS) * i);
            double rad = Math.toRadians(anguloParticula);

            float variacao = (i % 3 == 0) ? 1.15f : 1f;
            float raio = raioBase * variacao;

            float x = (float) (cx + raio * Math.cos(rad));
            float y = (float) (cy + raio * Math.sin(rad));

            float tamanho = ouvindo ? 8f : 5f;
            canvas.drawCircle(x, y, tamanho, paintParticula);
        }
    }
}
