package platform.graphics.objects;

import java.util.Arrays;

public class FrameBuffer {
    public final int width;
    public final int height;
    public final int[] pixels;
    public final int[] depth;

    public FrameBuffer(int width, int height) {
        this(width, height, 1);
    }

    public FrameBuffer(int width, int height, int scale) {
        int safeScale = Math.max(1, scale);
        this.width 	= Math.max(1, width / safeScale);
        this.height = Math.max(1, height / safeScale);
        this.pixels = new int[this.width * this.height];
        this.depth  = new int[this.width * this.height];
    }

    public void clear(int color) 	{Arrays.fill(pixels, color);}
}
