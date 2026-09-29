package main;

public class Utilities {
	
	public static int floatToInt(float r, float g, float b) {
	    int ri = (int) (r * 255);
	    int gi = (int) (g * 255);
	    int bi = (int) (b * 255);
	    if (ri > 255) ri = 255;
	    if (gi > 255) gi = 255;
	    if (bi > 255) bi = 255;
	    return (255 << 24) | (ri << 16) | (gi << 8) | bi;
	}
}
