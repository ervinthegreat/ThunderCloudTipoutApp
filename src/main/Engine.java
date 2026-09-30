package main;

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
	private static final int[] GEM_COLORS = {0xFF9B5DE5, 0xFFE0F7FF, 0xFF2E6BFF, 0xFFFFA630};

	private static final float SCENE_DEPTH = -4.0f;
	private static final float BOLT_SIZE = 2.0f;
	private static final float GEM_SIZE = 0.6f;
	/**
	 * Gem centers in GEM_MODELS order, as fractions of the visible screen at SCENE_DEPTH:
	 * x -1 = left edge, 1 = right edge; y -1 = bottom, 1 = top. Keeps the layout on any aspect ratio.
	 */
	private static final float[][] GEM_LAYOUT = {
			{-0.40f, 0.54f},	// amethyst
			{ 0.30f, 0.47f},	// diamond
			{-0.32f, -0.54f},	// sapphire
			{ 0.70f, -0.45f},	// topaz
	};

	private static final float SPIN_DEGREES_PER_TICK = 0.5f;
	private static final float TUMBLE_DEGREES_PER_TICK = SPIN_DEGREES_PER_TICK * 0.5f;
	private static final float GEM_SPIN_DEGREES_PER_TICK = 1.0f;

	private static Renderer renderer;
	private static TextureManager textureManager;
	private static Entity bolt;
	private static Entity[] gems;
	private static Camera camera;
	private static Light light;

	/** Seconds per update() tick; matches the original 32 ms loop. */
	public static final float TICK_SECONDS = 0.032f;

	public static void init() {
		camera = new Camera(new Vec3(0, 0, 0));
		renderer = new Renderer();
		textureManager = new TextureManager();
		renderer.setTextureManager(textureManager);
		light = new Light(new Vec3(10, 10, 10), new Vec3(1, 1, 1), 20f, 8f);
		renderer.setLight(light);
		renderer.setMainCamera(camera);
		renderer.setClearColor(Utilities.floatToInt(0, 0, 0));

		bolt = createModel(BOLT_MODEL, BOLT_COLOR, BOLT_SIZE, new Vec3(0, 0, SCENE_DEPTH));
		float halfHeight = (float) Math.tan(Math.toRadians(Renderer.FOV_Y_DEGREES) * 0.5) * -SCENE_DEPTH;
		float halfWidth = halfHeight * DisplayManager.getWidth() / DisplayManager.getHeight();
		gems = new Entity[GEM_MODELS.length];
		for (int i = 0; i < GEM_MODELS.length; i++) {
			Vec3 pos = new Vec3(GEM_LAYOUT[i][0] * halfWidth, GEM_LAYOUT[i][1] * halfHeight, SCENE_DEPTH);
			gems[i] = createModel(GEM_MODELS[i], GEM_COLORS[i], GEM_SIZE, pos);
		}
	}

	/** Loads res/<name>.obj, centers it, scales its largest dimension to size, and colors it solid. Null if missing. */
	private static Entity createModel(String name, int color, float size, Vec3 position) {
		Mesh model = OBJLoader.loadObjModel(name);
		if (model == null) return null;
		float extent = centerMesh(model);
		Entity e = new Entity(Rasterizer.createVAO(model), position, 0, 0, 0);
		e.setScale(extent > 0 ? size / extent : 1f);
		e.setTextureId(textureManager.createTexture(1, 1, new int[] {color}));
		e.setLit(true);
		return e;
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
				(bolt.getPitch() + TUMBLE_DEGREES_PER_TICK) % 360f,
				(bolt.getYaw() + SPIN_DEGREES_PER_TICK) % 360f,
				0);
		for (Entity gem : gems) {
			if (gem != null) gem.setRotation(0, (gem.getYaw() + GEM_SPIN_DEGREES_PER_TICK) % 360f, 0);
		}
	}

	public static void render() {
		DisplayManager.startFrame();
		renderer.prepare();
		if (bolt != null) renderer.render(bolt);
		for (Entity gem : gems) {
			if (gem != null) renderer.render(gem);
		}
		DisplayManager.endFrame();
	}
}
