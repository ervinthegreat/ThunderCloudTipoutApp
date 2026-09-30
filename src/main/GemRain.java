package main;

import java.util.Random;

import entities.Entity;
import math.Vec3;
import platform.graphics.Renderer;

/**
 * A fixed pool of gem entities that fall through the view. Gems that leave the bottom of the
 * screen are recycled to the top with a new random shape, color, column, depth, speed, size and spin.
 */
public class GemRain {

	private static final int POOL_SIZE = 10;
	private static final float NEAR_DEPTH = -3.0f;
	private static final float FAR_DEPTH = -10.0f;
	private static final float MIN_SIZE = 0.4f, MAX_SIZE = 0.7f;
	private static final float MIN_FALL_PER_SECOND = 1.0f, MAX_FALL_PER_SECOND = 2.5f;
	private static final float MIN_SPIN_DEGREES_PER_SECOND = 25f, MAX_SPIN_DEGREES_PER_SECOND = 95f;
	/** Extra distance beyond the screen edge so gems enter and leave fully out of view. */
	private static final float EDGE_MARGIN = 0.6f;

	/** A loaded mesh shared by many entities, with its unscaled largest dimension. */
	public static class Model {
		final int vaoId;
		final float extent;

		public Model(int vaoId, float extent) {
			this.vaoId = vaoId;
			this.extent = extent;
		}
	}

	private final Model[] shapes;
	private final int[] colorTextures;
	private final Entity[] drops = new Entity[POOL_SIZE];
	private final float[] fallSpeed = new float[POOL_SIZE];
	private final float[] spinPitch = new float[POOL_SIZE];
	private final float[] spinYaw = new float[POOL_SIZE];
	private final float tanHalfFov;
	private final float aspect;
	private final Random random = new Random();

	public GemRain(Model[] shapes, int[] colorTextures, float aspect) {
		this.shapes = shapes;
		this.colorTextures = colorTextures;
		this.aspect = aspect;
		this.tanHalfFov = (float) Math.tan(Math.toRadians(Renderer.FOV_Y_DEGREES) * 0.5);
		for (int i = 0; i < POOL_SIZE; i++) {
			drops[i] = new Entity(0, new Vec3(), 0, 0, 0);
			respawn(i);
			Vec3 p = drops[i].getPosition();
			float halfH = halfHeightAt(p.z);
			p.y = range(-halfH, halfH);
		}
	}

	private float halfHeightAt(float z) {return tanHalfFov * -z;}

	private float range(float min, float max) {return min + random.nextFloat() * (max - min);}

	/** Gives drop i a new random shape and color and places it just above the top of the screen. */
	private void respawn(int i) {
		Model shape = shapes[random.nextInt(shapes.length)];
		Entity e = drops[i];
		e.setVaoId(shape.vaoId);
		e.setTextureId(colorTextures[random.nextInt(colorTextures.length)]);
		e.setLit(true);
		e.setScale(shape.extent > 0 ? range(MIN_SIZE, MAX_SIZE) / shape.extent : 1f);
		e.setRotation(range(0, 360), range(0, 360), 0);

		float z = range(FAR_DEPTH, NEAR_DEPTH);
		float halfH = halfHeightAt(z);
		float halfW = halfH * aspect;
		e.getPosition().set(range(-halfW, halfW), halfH + EDGE_MARGIN, z);

		fallSpeed[i] = range(MIN_FALL_PER_SECOND, MAX_FALL_PER_SECOND);
		spinPitch[i] = randomSpin();
		spinYaw[i] = randomSpin();
	}

	/** Random speed with a guaranteed minimum and random direction, so both axes visibly turn. */
	private float randomSpin() {
		float speed = range(MIN_SPIN_DEGREES_PER_SECOND, MAX_SPIN_DEGREES_PER_SECOND);
		return random.nextBoolean() ? speed : -speed;
	}

	/** Advances every drop by dt seconds. */
	public void update(float dt) {
		for (int i = 0; i < POOL_SIZE; i++) {
			Entity e = drops[i];
			Vec3 p = e.getPosition();
			p.y -= fallSpeed[i] * dt;
			e.setRotation((e.getPitch() + spinPitch[i] * dt) % 360f, (e.getYaw() + spinYaw[i] * dt) % 360f, 0);
			if (p.y < -halfHeightAt(p.z) - EDGE_MARGIN) respawn(i);
		}
	}

	public void render(Renderer renderer) {
		for (Entity e : drops) renderer.render(e);
	}
}
