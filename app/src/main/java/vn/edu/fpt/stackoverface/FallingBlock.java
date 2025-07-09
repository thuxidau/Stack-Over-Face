package vn.edu.fpt.stackoverface;

import android.graphics.Canvas;
import android.graphics.Paint;

public class FallingBlock {
    float x, y, width, height;
    int color;
    float velocityY = 0;

    FallingBlock(float x, float y, float width, float height, int color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
    }

    void update() {
        velocityY += 2.5f; // gravity
        y += velocityY;
    }

    void draw(Canvas canvas, Paint paint) {
        BlockGameView.drawIsometricBlock(canvas, new Block(x, y, width, height, color));
    }
}