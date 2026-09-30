package platform.graphics;

import platform.graphics.objects.Mesh;

public class Shapes {
	public static Mesh createTriangle() {
		float[] positions = {
				-0.5f, -0.5f, 0f,
				 0.5f, -0.5f, 0f,
				 0.0f,  0.5f, 0f
		};
		int[] indices = { 0, 1, 2 };
		return new Mesh(positions, indices);
	}
	
	public static Mesh cube() {
		float[] vertices = {			
				-0.5f,0.5f,-0.5f,-0.5f,-0.5f,-0.5f,0.5f,-0.5f,-0.5f,0.5f,0.5f,-0.5f,		
				-0.5f,0.5f,0.5f,-0.5f,-0.5f,0.5f,0.5f,-0.5f,0.5f,0.5f,0.5f,0.5f,
				0.5f,0.5f,-0.5f,0.5f,-0.5f,-0.5f,0.5f,-0.5f,0.5f,0.5f,0.5f,0.5f,
				-0.5f,0.5f,-0.5f,-0.5f,-0.5f,-0.5f,-0.5f,-0.5f,0.5f,-0.5f,0.5f,0.5f,
				-0.5f,0.5f,0.5f,-0.5f,0.5f,-0.5f,0.5f,0.5f,-0.5f,0.5f,0.5f,0.5f,
				-0.5f,-0.5f,0.5f,-0.5f,-0.5f,-0.5f,0.5f,-0.5f,-0.5f,0.5f,-0.5f,0.5f
		};
		int[] indices = {
				3,1,0,2,1,3,4,5,7,7,5,6,11,9,8,10,9,11,12,13,
				15,15,13,14,19,17,16,18,17,19,20,21,23,23,21,22
		};
		return new Mesh(vertices, indices);
	}
}
