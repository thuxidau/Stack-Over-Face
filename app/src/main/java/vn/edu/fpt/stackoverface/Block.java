package vn.edu.fpt.stackoverface;

public class Block {
    float x, y, width, height;
    int color;

    Block(float x, float y, float width, float height, int color) {
        this.x = x; // center X
        this.y = y; // center Y
        this.width = width;
        this.height = height;
        this.color = color;
    }
}
