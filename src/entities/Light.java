package entities;

import math.Vec3;

public class Light {
	public Vec3 position;
	public Vec3 color;
	public float radius;
	public float falloff;

	public Light(Vec3 position, Vec3 color, float radius, float falloff) {
		this.position = position;
		this.color = color;
		this.radius = radius;
		this.falloff = falloff;
	}
}
