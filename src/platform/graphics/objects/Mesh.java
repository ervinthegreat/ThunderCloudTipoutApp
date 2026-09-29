package platform.graphics.objects;

public class Mesh {
	public float[] positions;
	public float[] texCoords;
	public int[] indices;
	public float[] normals;

	public Mesh(float[] positions, float[] texCoords, int[] indices) {
		this.positions = positions;
		this.texCoords = texCoords;
		this.indices = indices;
	}

	public Mesh(float[] positions, float[] texCoords, int[] indices, float[] normals) {
		this.positions = positions;
		this.texCoords = texCoords;
		this.indices = indices;
		this.normals = normals;
	}
}
