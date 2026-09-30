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
	private static final float BOLT_SIZE = 2.0f;
	private static final float BOLT_DISTANCE = 4.0f;
	private static final float SPIN_DEGREES_PER_TICK = 0.5f;
	private static final float TUMBLE_DEGREES_PER_TICK = SPIN_DEGREES_PER_TICK * 0.5f;
	private static final int BOLT_COLOR = 0xFFFFD21F;

	private static Renderer renderer;
	private static Entity bolt;
	private static Camera camera;
	private static Light light;

	/** Seconds per update() tick; matches the original 32 ms loop. */
	public static final float TICK_SECONDS = 0.032f;

	public static void init() {
		camera = new Camera(new Vec3(0, 0, 0));
		renderer = new Renderer();
		TextureManager textureManager = new TextureManager();
		int boltTexture = textureManager.createTexture(1, 1, new int[] {BOLT_COLOR});
		renderer.setTextureManager(textureManager);
		light = new Light(new Vec3(10, 10, 10), new Vec3(1, 1, 1), 20f, 8f);
		renderer.setLight(light);
		renderer.setMainCamera(camera);
		renderer.setClearColor(Utilities.floatToInt(0, 0, 0));

		Mesh model = OBJLoader.loadObjModel(BOLT_MODEL);
		if (model == null) return;
		float extent = centerMesh(model);
		bolt = new Entity(Rasterizer.createVAO(model), new Vec3(0, 0, -BOLT_DISTANCE), 0, 0, 0);
		bolt.setScale(extent > 0 ? BOLT_SIZE / extent : 1f);
		bolt.setTextureId(boltTexture);
		bolt.setLit(true);
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
	}

	public static void render() {
		DisplayManager.startFrame();
		renderer.prepare();
		if (bolt != null) renderer.render(bolt);
		DisplayManager.endFrame();
	}
}
