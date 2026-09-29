package platform.io;

/** Platform-specific resource access. Paths are relative to the project root, e.g. "res/marble.png". */
public interface AssetSource {
	/** Returns null if the image can't be loaded. */
	Bitmap loadImage(String path);

	/** Returns null if the file can't be loaded. */
	String loadText(String path);
}
