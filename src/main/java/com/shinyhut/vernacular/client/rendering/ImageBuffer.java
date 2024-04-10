package com.shinyhut.vernacular.client.rendering;

public class ImageBuffer {

    private final int width;
    private final int height;
    private final boolean alpha;
    private final int[] buffer;

    public ImageBuffer(int width, int height, boolean alpha) {
        this.width = width;
        this.height = height;
        this.alpha = alpha;
        this.buffer = new int[width * height];
    }

    public int[] getBuffer() {
        return buffer;
    }

    public ImageBuffer extend(int nw, int nh) {
        var b = new ImageBuffer(nw, nh, alpha);
        for (int ox = 0; ox < nw; ox++) {
            for (int oy = 0; oy < nh; oy++) {
                b.buffer[b.flatIndex(ox, oy)] = ox < width && oy < height ? buffer[flatIndex(ox, oy)] : 0;
            }
        }
        return b;
    }

    private int flatIndex(int x, int y) {
        return y * width + (x % width);
    }

    public void set(int x, int y, int color) {
        buffer[flatIndex(x, y)] = color;
    }

    public void fillRect(int x, int y, int width, int height, int color) {
        for (int ox = 0; ox < width; ox++) {
            for (int oy = 0; oy < height; oy++) {
                buffer[flatIndex(x + ox, y + oy)] = color;
            }
        }
    }

    public int get(int x, int y) {
        return buffer[flatIndex(x, y)];
    }

    public void duplicate(int x, int y, int width, int height, int targetX, int targetY) {
        for (int ox = 0; ox < width; ox++) {
            for (int oy = 0; oy < height; oy++) {
                buffer[flatIndex(targetX + ox, targetY + oy)] = buffer[flatIndex(x + ox,y + oy)];
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isAlpha() {
        return alpha;
    }
}
