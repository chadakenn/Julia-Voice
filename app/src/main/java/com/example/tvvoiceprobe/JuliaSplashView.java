package com.example.tvvoiceprobe;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/**
 * Short Julia Voice splash for full-screen launches. Drawn entirely with Android
 * primitives, so the TV UI doesn't need a large bitmap and scales to any display.
 * The ⭐ overlay shortcut launches its foreground service directly, without splash.
 */
public final class JuliaSplashView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public JuliaSplashView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false);
        setClickable(false);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int save = canvas.save();
        canvas.scale(getWidth() / 1000f, getHeight() / 560f);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, 1000, 560,
                0xff070e26, 0xff071937, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, 1000, 560, paint);
        paint.setShader(new RadialGradient(500, 285, 400,
                new int[] {0x7f173b85, 0x14162f66, 0x00000000},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, 1000, 560, paint);
        paint.setShader(null);

        drawWave(canvas, 0x554f6dff, 4.5f,
                0, 255, 140, 320, 232, 120, 345, 212, 480, 320,
                570, 395, 735, 195, 825, 225, 930, 330, 1000, 275);
        drawWave(canvas, 0x8849c9fe, 2f,
                0, 310, 140, 352, 245, 167, 356, 228, 480, 342,
                590, 425, 750, 243, 842, 270, 929, 346, 1000, 305);
        drawWave(canvas, 0x88815af9, 2.2f,
                0, 365, 148, 420, 255, 234, 350, 298, 482, 383,
                593, 458, 753, 293, 848, 331, 938, 416, 1000, 370);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(440, 125, 565, 228,
                new int[] {0xffbb75ff, 0xff6caaff, 0xff54f0f5}, null,
                Shader.TileMode.CLAMP));
        int[] heights = {19, 42, 63, 87, 118, 87, 63, 42, 19};
        for (int index = 0; index < heights.length; index++) {
            float x = 452 + index * 12f;
            float height = heights[index];
            canvas.drawRoundRect(new RectF(x, 170f - height / 2f, x + 7f,
                    170f + height / 2f), 3.5f, 3.5f, paint);
        }
        paint.setShader(null);

        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(86);
        paint.setColor(Color.WHITE);
        canvas.drawText("JULIA", 500, 326, paint);

        paint.setTextSize(53);
        paint.setShader(new LinearGradient(365, 355, 632, 355,
                0xffb48bff, 0xff4ddffd, Shader.TileMode.CLAMP));
        canvas.drawText("V O I C E", 500, 397, paint);
        paint.setShader(null);

        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(26);
        paint.setColor(0xffc6d2ef);
        canvas.drawText("Your voice. Your home. Your AI.", 500, 454, paint);
        paint.setTextSize(16);
        paint.setColor(0xff839fc3);
        canvas.drawText("Local-first voice for Home Assistant", 500, 520, paint);
        canvas.restoreToCount(save);
    }

    private void drawWave(Canvas canvas, int color, float width, float... v) {
        Path p = new Path();
        p.moveTo(v[0], v[1]);
        p.cubicTo(v[2], v[3], v[4], v[5], v[6], v[7]);
        p.cubicTo(v[8], v[9], v[10], v[11], v[12], v[13]);
        p.cubicTo(v[14], v[15], v[16], v[17], v[18], v[19]);
        paint.setShader(null);
        paint.setColor(color);
        paint.setStrokeWidth(width);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawPath(p, paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
