package com.example.tvvoiceprobe;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Static blue/violet light inside the card; never blurs or captures TV content. */
public final class AuroraGlassDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final float radius;
    private int opacity = 255;
    private ColorFilter filter;

    public AuroraGlassDrawable(float radius) { this.radius = radius; }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        RectF area = new RectF(bounds);
        clip.reset();
        clip.addRoundRect(area, radius, radius, Path.Direction.CW);
        int saved = canvas.save();
        canvas.clipPath(clip);
        paint.setColorFilter(filter);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xF20A1225);
        paint.setAlpha(Math.round(242 * opacity / 255f));
        canvas.drawRect(area, paint);
        float[] x = {0.06f, 0.40f, 0.65f, 0.92f};
        int[] colors = {0x803F7DFF, 0x405871EE, 0x556F4FC2, 0x5054BDFF};
        for (int i = 0; i < x.length; i++) {
            int light = canvas.save();
            canvas.translate(bounds.left + area.width() * x[i], bounds.top + area.height() * 0.5f);
            canvas.scale(0.30f, 1f);
            paint.setShader(new RadialGradient(0, 0, Math.max(1, area.height()),
                    colors[i], 0x000A1225, Shader.TileMode.CLAMP));
            paint.setAlpha(opacity);
            canvas.drawRect(-area.height(), -area.height(), area.height(), area.height(), paint);
            canvas.restoreToCount(light);
        }
        canvas.restoreToCount(saved);
        paint.setShader(null);
        paint.setColor(0x407FAEFF);
        paint.setAlpha(Math.round(64 * opacity / 255f));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        area.inset(1, 1);
        canvas.drawRoundRect(area, radius, radius, paint);
    }

    @Override public void setAlpha(int alpha) { opacity = Math.max(0, Math.min(255, alpha)); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter value) { filter = value; invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
