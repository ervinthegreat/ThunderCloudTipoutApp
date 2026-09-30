package entities;

import math.Vec3;

public class Entity {
	
	private int 	vaoId;
	private Vec3 	position = new Vec3(0,0,0);
	private int 	color = 0xFFFFFFFF;
	private boolean lit = true;

	private float 	roty,rotx,rotz;
	private float 	scale = 1;

	public Entity(int vaoId, Vec3 position, float x, float y, float z) {
		this.vaoId 	= vaoId;
		this.position = position;
		this.rotx 	= x;
		this.roty 	= y;
		this.rotz 	= z;
	}

	public void setPosition(Vec3 position) {this.position = position;}
	public void setVaoId(int vaoId) {this.vaoId = vaoId;}
	public void setRotation(float x, float y, float z) {this.rotx =x; this.roty = y; this.rotz = z;}
	public void setScale(float scale) {this.scale = scale;}
	public void setColor(int argb) {this.color = argb;}
	public void setLit(boolean lit) {this.lit = lit;}
	public int getVaoId() {return vaoId;}
	public int getColor() {return color;}
	public boolean isLit() {return lit;}
	public Vec3 getPosition() {return this.position;}
	public float getPitch() {return this.rotx;}
	public float getYaw()	{return this.roty;}
	public float getRoll()  {return this.rotz;}
	public float getScale()	{return this.scale;}

}
