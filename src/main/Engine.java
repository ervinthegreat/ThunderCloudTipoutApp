package main;

import entities.Camera;
import entities.Entity;
import entities.Light;
import math.Vec3;
import platform.display.DisplayManager;
import platform.graphics.Rasterizer;
import platform.graphics.Renderer;
import platform.graphics.Shapes;
import platform.graphics.TextureManager;
import platform.graphics.objects.Mesh;

public class Engine {

	private static Renderer renderer;
	private static Entity[] entities;
	private static Camera camera;
	private static Light light;

	/** Seconds per update() tick; matches the original 32 ms loop. */
	public static final float TICK_SECONDS = 0.032f;

	public static void init() {
		createEntity();
	}

	private static void createEntity() {
		Mesh model = Shapes.cube();
		int vaoId = Rasterizer.createVAO(model);
		camera = new Camera(new Vec3(0,0,0));
		renderer = new Renderer();
		TextureManager textureManager = new TextureManager();
		int marbleId = textureManager.loadTexture("res/marble.png");
		renderer.setTextureManager(textureManager);
		light = new Light(new Vec3(10, 10, 10), new Vec3(1, 1, 1), 20f, 8f);
		renderer.setLight(light);
		renderer.setMainCamera(camera);
		renderer.setClearColor(Utilities.floatToInt(0, 0, 0));
		
		entities = new Entity[28];
		int idx = 0;
		int gridX = 3;
		int gridY = 3;
		int gridZ = 3;
		float spacing = 3.0f;
		float startX = -((gridX - 1) * spacing) * 0.5f;
		float startY = -((gridY - 1) * spacing) * 0.5f;
		float startZ = -6.0f;
		for (int z = 0; z < gridZ; z++) {
			for (int y = 0; y < gridY; y++) {
				for (int x = 0; x < gridX; x++) {
					if (idx >= entities.length) break;
					Vec3 pos = new Vec3(
							startX + x * spacing,
							startY + y * spacing,
							startZ - z * spacing);
					Entity e = new Entity(vaoId, pos, 0, 0, 0);
					e.setTextureId(marbleId);
					e.setLit(true);
					entities[idx++] = e;
				}
			}
		}
	}

	/** Advances the scene by one fixed tick (TICK_SECONDS). */
	public static void update() {
		camera.move();
		for (int i = 0; i < entities.length; i++) {
			Entity e = entities[i];
			if (e == null) continue;
			e.setRotation(e.getPitch() + 1f, e.getYaw() + 1f, 0);
		}
	}

	public static void render() {
		DisplayManager.startFrame();
		renderer.prepare();
		for (int i = 0; i < entities.length; i++) {
			Entity e = entities[i];
			if (e == null) continue;
			renderer.render(e);
		}
		DisplayManager.endFrame();
	}
}
