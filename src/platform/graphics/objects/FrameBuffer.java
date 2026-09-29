package platform.graphics.objects;

import java.util.Arrays;

public class FrameBuffer {
    public static final int TILE_SIZE = 16;
    public final int width;
    public final int height;
    public final int[] pixels;
    public final int[] depth;
    private final int tileWidth;
    private final int tileHeight;
    private final int[] tileMaxDepth;
    private final short[] tileCoverage;
    private final short[] tileCoverageLimit;

    public FrameBuffer(int width, int height) {
        this(width, height, 1);
    }

    public FrameBuffer(int width, int height, int scale) {
        int safeScale = Math.max(1, scale);
        this.width 	= Math.max(1, width / safeScale);
        this.height = Math.max(1, height / safeScale);
        this.pixels = new int[this.width * this.height];
        this.depth  = new int[this.width * this.height];
        this.tileWidth = (this.width + TILE_SIZE - 1) / TILE_SIZE;
        this.tileHeight = (this.height + TILE_SIZE - 1) / TILE_SIZE;
        this.tileMaxDepth = new int[this.tileWidth * this.tileHeight];
        this.tileCoverage = new short[this.tileWidth * this.tileHeight];
        this.tileCoverageLimit = new short[this.tileWidth * this.tileHeight];
        for (int ty = 0; ty < tileHeight; ty++) {
            int tilePixelHeight = Math.min(TILE_SIZE, this.height - ty * TILE_SIZE);
            for (int tx = 0; tx < tileWidth; tx++) {
                int tilePixelWidth = Math.min(TILE_SIZE, this.width - tx * TILE_SIZE);
                int idx = ty * tileWidth + tx;
                tileMaxDepth[idx] = Integer.MIN_VALUE;
                tileCoverage[idx] = 0;
                tileCoverageLimit[idx] = (short) (tilePixelWidth * tilePixelHeight);
            }
        }
    }

    public void clear(int color) 	{Arrays.fill(pixels, color);}
    public void clearDepth(int depthValue) {
        Arrays.fill(depth, depthValue);
        Arrays.fill(tileMaxDepth, Integer.MIN_VALUE);
        Arrays.fill(tileCoverage, (short) 0);
    }
    public boolean hierarchicalZTest(int tileX, int tileY, int zFixed) {
        if (tileX < 0 || tileX >= tileWidth || tileY < 0 || tileY >= tileHeight) return true;
        int idx = tileY * tileWidth + tileX;
        if (tileCoverage[idx] != tileCoverageLimit[idx]) return true;
        return zFixed < tileMaxDepth[idx];
    }

    public void updateHierarchicalZ(int x, int y, int zFixed, int prevDepth) {
        int tileX = x / TILE_SIZE;
        int tileY = y / TILE_SIZE;
        if (tileX < 0 || tileX >= tileWidth || tileY < 0 || tileY >= tileHeight) return;
        int idx = tileY * tileWidth + tileX;
        if (prevDepth == Integer.MAX_VALUE && tileCoverage[idx] < tileCoverageLimit[idx]) {
            tileCoverage[idx]++;
        }
        if (zFixed > tileMaxDepth[idx]) {
            tileMaxDepth[idx] = zFixed;
        }
    }
}