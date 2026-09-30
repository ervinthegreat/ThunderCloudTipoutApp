package main;

import java.util.ArrayList;
import java.util.List;

import entities.Camera;
import entities.Entity;
import entities.Light;
import io.OBJLoader;
import math.Vec3;
import platform.display.DisplayManager;
import platform.graphics.Rasterizer;
import platform.graphics.Renderer;
import platform.graphics.TextureManager;
import platform.graphics.objects.Mesh;

public class Engine {

	public static final String BOLT_MODEL = "thunder";
	public static final String[] GEM_MODELS = {"gems/amethyst", "gems/diamond", "gems/sapphire", "gems/topaz"};
	private static final int BOLT_COLOR = 0xFFFFD21F;
	private static final int[] GEM_COLORS = {
			0xFFFF2A2A,	// red
			0xFFFF8A1A,	// orange
			0xFFFFE14D,	// gold
			0xFF8CFF3A,	// lime
			0xFF1AE87A,	// emerald
			0xFF2AE8FF,	// cyan
			0xFF3A6BFF,	// blue
			0xFF9B5DE5,	// purple
			0xFFFF3AD8,	// magenta
			0xFFFF5C8A,	// pink
			0xFFF2FBFF,	// white
	};

	private static final float BOLT_DEPTH = -4.0f;
	private static final float BOLT_SIZE = 2.0f;
	private static final float SPIN_DEGREES_PER_SECOND = 15f;
	private static final float TUMBLE_DEGREES_PER_SECOND = SPIN_DEGREES_PER_SECOND * 0.5f;

	private static Renderer renderer;
	private static TextureManager textureManager;
	private static Entity bolt;
	private static GemRain gemRain;
	private static Camera camera;
	private static Light light;

	/** One update() and one render() per frame. Speeds are per second, so changing this keeps motion speed. */
	public static final int FRAMES_PER_SECOND = 26;
	public static final float TICK_SECONDS = 1f / FRAMES_PER_SECOND;

	public static void init() {
		camera = new Camera(new Vec3(0, 0, 0));
		renderer = new Renderer();
		textureManager = new TextureManager();
		renderer.setTextureManager(textureManager);
		light = new Light(new Vec3(10, 10, 10), new Vec3(1, 1, 1), 30f, 8f);
		renderer.setLight(light);
		renderer.setMainCamera(camera);
		renderer.setClearColor(Utilities.floatToInt(0, 0, 0));

		GemRain.Model boltModel = loadModel(BOLT_MODEL);
		if (boltModel != null) {
			bolt = new Entity(boltModel.vaoId, new Vec3(0, 0, BOLT_DEPTH), 0, 0, 0);
			bolt.setScale(boltModel.extent > 0 ? BOLT_SIZE / boltModel.extent : 1f);
			bolt.setTextureId(solidColor(BOLT_COLOR));
			bolt.setLit(true);
		}

		List<GemRain.Model> gemShapes = new ArrayList<>();
		for (String name : GEM_MODELS) {
			GemRain.Model shape = loadModel(name);
			if (shape != null) gemShapes.add(shape);
		}
		int[] gemColors = new int[GEM_COLORS.length];
		for (int i = 0; i < GEM_COLORS.length; i++) gemColors[i] = solidColor(GEM_COLORS[i]);
		if (!gemShapes.isEmpty()) {
			float aspect = (float) DisplayManager.getWidth() / DisplayManager.getHeight();
			gemRain = new GemRain(gemShapes.toArray(new GemRain.Model[0]), gemColors, aspect);
		}
	}

	/** Loads res/<name>.obj once and centers it. Null if missing. */
	private static GemRain.Model loadModel(String name) {
		Mesh mesh = OBJLoader.loadObjModel(name);
		if (mesh == null) return null;
		float extent = centerMesh(mesh);
		return new GemRain.Model(Rasterizer.createVAO(mesh), extent);
	}

	/** One-pixel texture, so an entity renders as a single solid color. */
	private static int solidColor(int argb) {
		return textureManager.createTexture(1, 1, new int[] {argb});
	}

	/** Moves the mesh's bounding-box center to the origin so it spins in place; returns its largest dimension. */
	private static float centerMesh(Mesh mesh) {
		float[] p = mesh.positions;
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (int i = 0; i + 2 < p.length; i += 3) {
			minX = Math.min(minX, p[i]);     maxX = Math.max(maxX, p[i]);
			minY = Math.min(minY, p[i + 1]); maxY = Math.max(maxY, p[i + 1]);
			minZ = Math.min(minZ, p[i + 2]); maxZ = Math.max(maxZ, p[i + 2]);
		}
		float cx = (minX + maxX) * 0.5f, cy = (minY + maxY) * 0.5f, cz = (minZ + maxZ) * 0.5f;
		for (int i = 0; i + 2 < p.length; i += 3) {
			p[i] -= cx; p[i + 1] -= cy; p[i + 2] -= cz;
		}
		return Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
	}

	/** Advances the scene by one fixed tick (TICK_SECONDS). */
	public static void update() {
		if (bolt != null) bolt.setRotation(
				(bolt.getPitch() + TUMBLE_DEGREES_PER_SECOND * TICK_SECONDS) % 360f,
				(bolt.getYaw() + SPIN_DEGREES_PER_SECOND * TICK_SECONDS) % 360f,
				0);
		if (gemRain != null) gemRain.update(TICK_SECONDS);
	}

	public static void render() {
		DisplayManager.startFrame();
		renderer.prepare();
		if (bolt != null) renderer.render(bolt);
		if (gemRain != null) gemRain.render(renderer);
		DisplayManager.endFrame();
	}
}
