package vn.edu.fpt.stackoverface;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockGameView extends View {

    private List<Block> stackBlocks = new ArrayList<>();
    private Block currentBlock;
    private Paint blockPaint;
    private float blockWidth, blockHeight;
    private boolean movingRight = true;
    private final List<FallingBlock> fallingBlocks = new ArrayList<>();
    private int score = 0;
    private final int BLOCK_SPEED = 4;
    private final int FRAME_DELAY = 16; // ~60fps
    private static float stackOffsetY = 200; // Initial vertical offset from bottom
    private float stackShiftPerDrop = 30; // How much to move down each time
    private Handler handler = new Handler();
    private Runnable gameOverCallback;
    private boolean isGameOver = false;
    private boolean tapEnabled = true;
    private Runnable scoreUpdateCallback;
    private MediaPlayer dropSoundPlayer;
    private Context context;


    public void setGameOver(boolean over) {
        isGameOver = over;
    }

    public void setTapEnabled(boolean enabled) {
        this.tapEnabled = enabled;
    }

    public void setScoreUpdateCallback(Runnable callback) {
        this.scoreUpdateCallback = callback;
    }

    public void setContext(Context context) {
        this.context = context;
        dropSoundPlayer = MediaPlayer.create(context, R.raw.drop_block);
    }

    public BlockGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        blockPaint = new Paint();
        blockPaint.setStyle(Paint.Style.FILL);

        blockHeight = 30;
        blockWidth = 400;

        post(() -> {
            float centerX = getWidth() / 2f;
            float baseY = getHeight() - blockHeight - stackOffsetY - 100; // raised above bottom

            float[] hsv = new float[3];
            Color.colorToHSV(Color.parseColor("#D84137"), hsv);

            Block baseBlock = new Block(centerX, baseY, blockWidth, blockHeight, Color.HSVToColor(new float[]{3.5f, 0.74f, 0.85f}));
            stackBlocks.add(baseBlock);
            stackOffsetY -= 400;

            float startY = baseY - blockHeight;
            int nextColor = generateNextColor(Color.HSVToColor(new float[]{3.5f, 0.74f, 0.85f}));
            currentBlock = new Block(centerX, startY, blockWidth, blockHeight, nextColor);

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
        if (isGameOver || currentBlock == null) return;

        float halfWidth = currentBlock.width / 2f;

        if (movingRight) {
            currentBlock.x += BLOCK_SPEED;
            if (currentBlock.x + halfWidth >= getWidth()) {
                currentBlock.x = getWidth() - halfWidth;
                movingRight = false;
            }
        } else {
            currentBlock.x -= BLOCK_SPEED;
            if (currentBlock.x - halfWidth <= 0) {
                currentBlock.x = halfWidth;
                movingRight = true;
            }
        }
    }

    public void dropBlock() {
        if (currentBlock == null) return;

        Block last = stackBlocks.get(stackBlocks.size() - 1);

        float lastLeft = last.x - last.width / 2f;
        float lastRight = last.x + last.width / 2f;

        float currLeft = currentBlock.x - currentBlock.width / 2f;
        float currRight = currentBlock.x + currentBlock.width / 2f;

        float overlapLeft = Math.max(lastLeft, currLeft);
        float overlapRight = Math.min(lastRight, currRight);
        float overlapWidth = overlapRight - overlapLeft;

        if (overlapWidth <= 0) {
            // No overlap – GAME OVER
            gameOverCallback.run();
            return;
        }

        // Trimmed block values
        currentBlock.x = (overlapLeft + overlapRight) / 2f;
        currentBlock.width = overlapWidth;

        // Cut off redundant pieces
        if (currLeft < overlapLeft) {
            // Left cut-off
            float cutWidth = overlapLeft - currLeft;
            float cutX = currLeft + cutWidth / 2f;
            fallingBlocks.add(new FallingBlock(cutX, currentBlock.y, cutWidth, blockHeight, currentBlock.color));
        }
        if (currRight > overlapRight) {
            // Right cut-off
            float cutWidth = currRight - overlapRight;
            float cutX = overlapRight + cutWidth / 2f;
            fallingBlocks.add(new FallingBlock(cutX, currentBlock.y, cutWidth, blockHeight, currentBlock.color));
        }

        // Stack it
        currentBlock.y = last.y - blockHeight;
        stackBlocks.add(currentBlock);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean soundEnabled = prefs.getBoolean("sound_enabled", true);

        if (soundEnabled && dropSoundPlayer != null) {
            dropSoundPlayer.start();
        }

        score++;
        stackOffsetY += stackShiftPerDrop;

        if (scoreUpdateCallback != null) {
            scoreUpdateCallback.run();
        }

        // Create next block
        int newColor = generateNextColor(currentBlock.color);
        float newY = currentBlock.y - blockHeight;
        float screenWidth = getWidth();

        movingRight = (score % 2 == 0);
        float startX = movingRight
                ? blockWidth / 2f
                : screenWidth - blockWidth / 2f;

        // Start full width again for next block
        currentBlock = new Block(startX, newY, currentBlock.width, blockHeight, newColor);
    }

    private int generateNextColor(int previousColor) {
        float[] hsv = new float[]{0f, 0.5f, 0.75f}; // softer saturation, brighter value

        if (previousColor != 0) {
            Color.colorToHSV(previousColor, hsv);
            hsv[0] = (hsv[0] + 30f) % 360f; // rotate hue
        }

        return Color.HSVToColor(hsv);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Iterator<FallingBlock> iterator = fallingBlocks.iterator();
        while (iterator.hasNext()) {
            FallingBlock fb = iterator.next();
            fb.update();
            fb.draw(canvas, blockPaint);

            // Remove if it falls out of screen
            if (fb.y + fb.height + stackOffsetY > getHeight() + 200) {
                iterator.remove();
            }
        }

        // Draw stacked blocks
        for (Block b : stackBlocks) {
            drawIsometricBlock(canvas, b);
        }

        // Draw current moving block
        if (currentBlock != null) {
            drawIsometricBlock(canvas, currentBlock);
        }
    }

    protected static void drawIsometricBlock(Canvas canvas, Block block) {
        float centerX = block.x;
        float centerY = block.y + stackOffsetY;
        float size = block.width;
        float height = block.height;

        float half = size / 2f;
        float quarter = size / 4f;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        // --- TOP FACE ---
        Path top = new Path();
        top.moveTo(centerX, centerY - quarter);            // top point
        top.lineTo(centerX + half, centerY);               // right point
        top.lineTo(centerX, centerY + quarter);            // bottom point
        top.lineTo(centerX - half, centerY);               // left point
        top.close();
        paint.setColor(block.color);
        canvas.drawPath(top, paint);

        // --- RIGHT SIDE ---
        Path right = new Path();
        right.moveTo(centerX, centerY + quarter);          // top-left of side
        right.lineTo(centerX + half, centerY);             // top-right
        right.lineTo(centerX + half, centerY + height);    // bottom-right
        right.lineTo(centerX, centerY + height + quarter); // bottom-left
        right.close();
        paint.setColor(darkenColor(block.color, 0.75f));
        canvas.drawPath(right, paint);

        // --- LEFT SIDE ---
        Path left = new Path();
        left.moveTo(centerX - half, centerY);              // top-left
        left.lineTo(centerX, centerY + quarter);           // top-right
        left.lineTo(centerX, centerY + height + quarter);  // bottom-right
        left.lineTo(centerX - half, centerY + height);     // bottom-left
        left.close();
        paint.setColor(darkenColor(block.color, 0.55f));
        canvas.drawPath(left, paint);
    }

    private static int darkenColor(int color, float factor) {
        int r = (int)(Color.red(color) * factor);
        int g = (int)(Color.green(color) * factor);
        int b = (int)(Color.blue(color) * factor);
        return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isGameOver || !tapEnabled) return false;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            dropBlock();
            return true;
        }
        return false;
    }

    public int getScore() {
        return score;
    }

    public void setGameOverCallback(Runnable callback) {
        this.gameOverCallback = callback;
    }
}