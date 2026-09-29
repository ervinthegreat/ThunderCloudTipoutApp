package math;

public class Vec3 {
	public float x,	y, z;
	
	public Vec3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vec3() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}

	public void set(Vec3 vec) {
		this.x = vec.x;
		this.y = vec.y;
		this.z = vec.z;
	}
	
	public void set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void cross(Vec3 a, Vec3 b) {}
	public void dot(Vec3 a, Vec3 b) {}
	public void add(Vec3 a, Vec3 b) {}
	public void sub(Vec3 a, Vec3 b) {}
}
