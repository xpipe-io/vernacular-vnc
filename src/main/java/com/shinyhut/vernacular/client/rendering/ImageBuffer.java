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
        return y * width + x;
    }

    public boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public void set(int x, int y, int color) {
        if (contains(x, y)) {
            buffer[flatIndex(x, y)] = color;
        }
    }

    public void fillRect(int x, int y, int width, int height, int color) {
        int startX = Math.max(x, 0);
        int startY = Math.max(y, 0);
        int endX = Math.min(x + width, this.width);
        int endY = Math.min(y + height, this.height);
        for (int oy = startY; oy < endY; oy++) {
            for (int ox = startX; ox < endX; ox++) {
                buffer[flatIndex(ox, oy)] = color;
            }
        }
    }

    public int get(int x, int y) {
        return buffer[flatIndex(x, y)];
    }

    public void duplicate(int x, int y, int width, int height, int targetX, int targetY) {
        // Copy the source region first, as it may overlap with the target region
        int[] source = new int[width * height];
        for (int oy = 0; oy < height; oy++) {
            for (int ox = 0; ox < width; ox++) {
                source[oy * width + ox] = contains(x + ox, y + oy) ? buffer[flatIndex(x + ox, y + oy)] : 0;
            }
        }
        for (int oy = 0; oy < height; oy++) {
            for (int ox = 0; ox < width; ox++) {
                set(targetX + ox, targetY + oy, source[oy * width + ox]);
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
