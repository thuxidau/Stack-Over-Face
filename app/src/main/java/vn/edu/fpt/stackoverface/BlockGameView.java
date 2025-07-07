package vn.edu.fpt.stackoverface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BlockGameView extends View {

    private Paint blockPaint;
    private float blockWidth, blockHeight;
    private float blockX, blockY;
    private boolean movingRight = true;

    private List<Float> stackHeights = new ArrayList<>();
    private int score = 0;

    private final int BLOCK_SPEED = 10;
    private final int FRAME_DELAY = 16; // ~60fps

    private Handler handler = new Handler();

    public BlockGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        blockPaint = new Paint();
        blockPaint.setColor(Color.RED);
        blockPaint.setStyle(Paint.Style.FILL);

        blockHeight = 80;
        blockWidth = 300;

        // Start with base block
        stackHeights.add(getHeight() - blockHeight);

        startMoving();
    }

    private void startMoving() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                moveBlock();
                invalidate();
                handler.postDelayed(this, FRAME_DELAY);
            }
        }, FRAME_DELAY);
    }

    private void moveBlock() {
        if (movingRight) {
            blockX += BLOCK_SPEED;
            if (blockX + blockWidth > getWidth()) {
                blockX = getWidth() - blockWidth;
                movingRight = false;
            }
        } else {
            blockX -= BLOCK_SPEED;
            if (blockX < 0) {
                blockX = 0;
                movingRight = true;
            }
        }
    }

    public void dropBlock() {
        float topY = getHeight() - blockHeight * (stackHeights.size() + 1);
        stackHeights.add(topY);
        blockY = topY;

        score++;
        blockPaint.setColor(generateNextColor(score));

        // Reset block to top
        blockX = 0;
        movingRight = true;
    }

    private int generateNextColor(int step) {
        float fraction = Math.min(1f, step / 10f); // Simple gradient cap at 10 blocks
        return Color.HSVToColor(new float[]{240f * (1 - fraction), 1f, 1f});
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw stacked blocks
        for (int i = 0; i < stackHeights.size(); i++) {
            float y = stackHeights.get(i);
            canvas.drawRect(0, y, blockWidth, y + blockHeight, blockPaint);
        }

        // Draw moving block
        float currentY = getHeight() - blockHeight * (stackHeights.size() + 1);
        canvas.drawRect(blockX, currentY, blockX + blockWidth, currentY + blockHeight, blockPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TEMP: for tap-to-drop testing
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            dropBlock();
            return true;
        }
        return false;
    }
}
