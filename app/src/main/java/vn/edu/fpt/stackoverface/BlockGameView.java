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

    private List<Block> stackBlocks = new ArrayList<>();
    private Block currentBlock;

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
        blockPaint.setStyle(Paint.Style.FILL);

        blockHeight = 80;
        blockWidth = 400;

        post(() -> {
            float baseX = (getWidth() - blockWidth) / 2f;
            float baseY = getHeight() - blockHeight;

            Block baseBlock = new Block(baseX, baseY, blockWidth, blockHeight, Color.RED);
            stackBlocks.add(baseBlock);

            float startY = baseY - blockHeight;
            int nextColor = generateNextColor(Color.RED);
            currentBlock = new Block(0, startY, blockWidth, blockHeight, nextColor);

            startMoving();
        });
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
            currentBlock.x += BLOCK_SPEED;
            if (currentBlock.x + currentBlock.width > getWidth()) {
                currentBlock.x = getWidth() - currentBlock.width;
                movingRight = false;
            }
        } else {
            currentBlock.x -= BLOCK_SPEED;
            if (currentBlock.x < 0) {
                currentBlock.x = 0;
                movingRight = true;
            }
        }
    }

    public void dropBlock() {
        if (currentBlock == null) return;

        // Set the block's Y position based on stack size
        float topY = getHeight() - blockHeight * (stackBlocks.size() + 1);
        currentBlock.y = topY;

        // Add to stack
        stackBlocks.add(currentBlock);
        score++;

        // Generate new block with a different color
        int newColor = generateNextColor(currentBlock.color);
        float newY = topY - blockHeight;
        currentBlock = new Block(0, newY, blockWidth, blockHeight, newColor);
        movingRight = true;
    }

    private int generateNextColor(int previousColor) {
        // Change HUE gradually, wrap from red (0) to blue (240)
        float[] hsv = new float[]{0f, 1f, 1f}; // default red
        Color.colorToHSV(previousColor, hsv);
        hsv[0] = (hsv[0] + 30f) % 360f; // increment hue

        return Color.HSVToColor(hsv);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw stacked blocks
        for (Block b : stackBlocks) {
            blockPaint.setColor(b.color);
            canvas.drawRect(b.x, b.y, b.x + b.width, b.y + b.height, blockPaint);
        }

        // Draw current moving block
        if (currentBlock != null) {
            blockPaint.setColor(currentBlock.color);
            canvas.drawRect(currentBlock.x, currentBlock.y,
                    currentBlock.x + currentBlock.width, currentBlock.y + currentBlock.height, blockPaint);
        }
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

    public int getScore() {
        return score;
    }
}
