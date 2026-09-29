package entities;

import math.Vec3;
import platform.input.Keyboard;
import platform.input.Mouse;
import math.Maths;


public class Camera {
	public Vec3 position;
	public float pitch;  // degrees, up/down
	public float yaw;    // degrees, left/right
	public float roll;

	private float mouseSensitivity = 0.15f;
	private float moveSpeed = 0.1f;
	private final Vec3 reusableForward = new Vec3();
	private final Vec3 reusableRight = new Vec3();

	public Camera() {
		this.position = new Vec3();
		this.pitch = 0f;
		this.yaw = 0f;
		this.roll = 0f;
	}

	public Camera(Vec3 position) {
		this.position = position != null ? position : new Vec3();

	}

	public void move() {
		if (Mouse.isGrabbed()) {
			float dx = Mouse.getDX();
			float dy = -Mouse.getDY();
			yaw += dx * mouseSensitivity;
			pitch -= dy * mouseSensitivity;
			if (pitch > 90f) pitch = 90f;
			if (pitch < -90f) pitch = -90f;
			if (yaw >= 360f) yaw -= 360f;
			if (yaw < 0f) yaw += 360f;
		}

		float cosYaw = Maths.cosDeg(yaw);
		float sinYaw = Maths.sinDeg(yaw);
		float cosPitch = Maths.cosDeg(pitch);
		float sinPitch = Maths.sinDeg(pitch);

		reusableForward.set(
			sinYaw * cosPitch,
			-sinPitch,
			-cosYaw * cosPitch
		);
		reusableRight.set(-cosYaw, 0f, -sinYaw);

		float speed = moveSpeed;
		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
			speed = 0.01f;

		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			position.x += reusableForward.x * speed;
			position.y += reusableForward.y * speed;
			position.z += reusableForward.z * speed;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			position.x -= reusableForward.x * speed;
			position.y -= reusableForward.y * speed;
			position.z -= reusableForward.z * speed;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			position.x += reusableRight.x * speed;
			position.z += reusableRight.z * speed;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			position.x -= reusableRight.x * speed;
			position.z -= reusableRight.z * speed;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
			position.y += speed;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			position.y -= speed;
		}
	}
	public float getPitch() 	{return pitch;}
	public float getYaw() 		{return yaw;}
	public float getRoll() 		{return roll;}
	public Vec3 getPosition() 	{return this.position;}
}
