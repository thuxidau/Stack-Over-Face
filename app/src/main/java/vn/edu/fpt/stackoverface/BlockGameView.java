package vn.edu.fpt.stackoverface;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockGameView extends View {

    private final List<Block> stackBlocks = new ArrayList<>(); // List of already stacked blocks
    private Block currentBlock; // The moving block currently controlled by the player
    private Paint blockPaint; // Paint object used to draw blocks
    private float blockWidth, blockHeight; // Dimensions of each block
    private boolean movingRight = true; // Direction flag for block movement, default is true for moving right
    private final List<FallingBlock> fallingBlocks = new ArrayList<>(); // List of redundant blocks
    private int score = 0; // Current score
    private final int FRAME_DELAY = 16; // ~60fps
    private static float stackOffsetY = 200; // Initial vertical offset from bottom
    private final Handler handler = new Handler();
    private Runnable gameOverCallback; // Runnable to run when game ends
    private boolean isGameOver = false; // Whether game is over
    private boolean tapEnabled = true; // Whether tapping to drop is allowed (used in blink mode)
    private Runnable scoreUpdateCallback; // Runnable to run when the score is updated
    private MediaPlayer dropSoundPlayer; // MediaPlayer to play drop sound
    private Context context; // App context for sounds and prefs

    public int getScore() {
        return score;
    }

    public void setGameOverCallback(Runnable callback) {
        this.gameOverCallback = callback;
    }

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
        // Draw blocks with a solid fill color
        blockPaint = new Paint();
        blockPaint.setStyle(Paint.Style.FILL);

        // Set block dimensions
        blockHeight = 30; // pixels
        blockWidth = 400; // pixels

        post(() -> {
            // Initial positions
            float centerX = getWidth() / 2f;
            float baseY = getHeight() - blockHeight - stackOffsetY - 100; // raised above bottom

            // Create the base block
            Block baseBlock = new Block(centerX, baseY, blockWidth, blockHeight, Color.HSVToColor(new float[]{3.5f, 0.74f, 0.85f}));
            stackBlocks.add(baseBlock);
            stackOffsetY -= 400; // Update vertical offset

            // Create the moving block
            float startY = baseY - blockHeight; // Place the new block directly above the base block
            int nextColor = generateNextColor(Color.HSVToColor(new float[]{3.5f, 0.74f, 0.85f}));
            currentBlock = new Block(centerX, startY, blockWidth, blockHeight, nextColor);

            // Block starts moving
            startMoving();
        });
    }

    private void startMoving() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                moveBlock(); // Update the position of the block
                invalidate(); // Request a redraw of the view to reflect changes
                handler.postDelayed(this, FRAME_DELAY); // Repost itself to create a continuous loop
            }
        }, FRAME_DELAY);
    }

    private void moveBlock() {
        // Stop movement if the game is over or block doesn't exist
        if (isGameOver || currentBlock == null) return;

        float halfWidth = currentBlock.width / 2f; // Screen-edge collisions
        int BLOCK_SPEED = 4; // Block moving speed

        if (movingRight) {
            currentBlock.x += BLOCK_SPEED;

            // The block hits the right edge of the view
            if (currentBlock.x + halfWidth >= getWidth()) {
                // Set the X coordinate of the block to be just inside the right edge of the screen
                currentBlock.x = getWidth() - halfWidth;
                movingRight = false;
            }
        } else {
            currentBlock.x -= BLOCK_SPEED;

            // The block hits the left edge of the view
            if (currentBlock.x - halfWidth <= 0) {
                // Set the X coordinate of the block to be just inside the left edge of the screen
                currentBlock.x = halfWidth;
                movingRight = true;
            }
        }
    }

    public void dropBlock() {
        // If there's no active moving block, do nothing
        if (currentBlock == null) return;

        // Get the last stacked block
        Block last = stackBlocks.get(stackBlocks.size() - 1);

        // Calculate last block edges
        float lastLeft = last.x - last.width / 2f;
        float lastRight = last.x + last.width / 2f;

        // Calculate current block edges
        float currLeft = currentBlock.x - currentBlock.width / 2f;
        float currRight = currentBlock.x + currentBlock.width / 2f;

        // Calculate overlap
        float overlapLeft = Math.max(lastLeft, currLeft);
        float overlapRight = Math.min(lastRight, currRight);
        float overlapWidth = overlapRight - overlapLeft;

        // No overlap –> GAME OVER
        if (overlapWidth <= 0) {
            gameOverCallback.run();
            return;
        }

        // Trim the current block
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

        // Add trimmed block to stack
        currentBlock.y = last.y - blockHeight;
        stackBlocks.add(currentBlock);

        // Add vibration when the block is dropped
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean vibrationEnabled = prefs.getBoolean("vibration_enabled", true);
        if (vibrationEnabled) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }

        // Play a drop sound if the user has sound enabled
        boolean soundEnabled = prefs.getBoolean("sound_enabled", true);
        if (soundEnabled && dropSoundPlayer != null) {
            dropSoundPlayer.start();
        }

        // Update score & offset
        score++;
        stackOffsetY += 30; // Shift the stack downward by 30 pixels

        // Trigger UI or logic updates for the new score
        if (scoreUpdateCallback != null) {
            scoreUpdateCallback.run();
        }

        // Create next moving block
        int newColor = generateNextColor(currentBlock.color);
        float newY = currentBlock.y - blockHeight;

        // If score is even, the block will move from left to right and vice versa
        movingRight = (score % 2 == 0);
        float startX = movingRight
                ? blockWidth / 2f
                : getWidth() - blockWidth / 2f;

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
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Draw falling blocks (trimmed pieces)
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
        // Calculate geometry
        // Visual center of the block
        float centerX = block.x;
        float centerY = block.y + stackOffsetY;

        // Block width and height
        float size = block.width; // Block width (used for both top face and perspective)
        float height = block.height;

        // Helper values to create diamond and skewed side shapes
        float half = size / 2f;
        float quarter = size / 4f;

        // Create paint object
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); // Anti-aliasing for smooth edges
        paint.setStyle(Paint.Style.FILL); // Paint style is filled shapes (not stroked outlines)

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
        right.moveTo(centerX, centerY + quarter);             // top-left of side
        right.lineTo(centerX + half, centerY);                // top-right
        right.lineTo(centerX + half, centerY + height);    // bottom-right
        right.lineTo(centerX, centerY + height + quarter);    // bottom-left
        right.close();
        paint.setColor(darkenColor(block.color, 0.75f));
        canvas.drawPath(right, paint);

        // --- LEFT SIDE ---
        Path left = new Path();
        left.moveTo(centerX - half, centerY);                 // top-left
        left.lineTo(centerX, centerY + quarter);              // top-right
        left.lineTo(centerX, centerY + height + quarter);     // bottom-right
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
        // If the game is over or user interaction is disabled, touch is ignored
        if (isGameOver || !tapEnabled) return false;

        // When the user taps, the view calls performClick()
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            performClick();
            return true;
        }
        return false;
    }

    @Override
    public boolean performClick() {
        super.performClick(); // Call the default behavior (important for accessibility)
        dropBlock(); // Simulate the actual tap action
        return true;
    }
}