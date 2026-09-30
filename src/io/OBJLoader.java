package io;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import math.Vec2;
import math.Vec3;
import platform.graphics.objects.Mesh;
import platform.io.Assets;

public class OBJLoader {

	public static Mesh loadObjModel(String fileName) {
		String source = Assets.loadText("res/"+fileName+".obj");
		if (source == null) {
			System.err.println("Couldn't load file!");
			return null;
		}

		List<Vec3> inPositions = new ArrayList<Vec3>();
		List<Vec2> inTexCoords = new ArrayList<Vec2>();
		List<Vec3> inNormals = new ArrayList<Vec3>();

		List<Float> outPositions = new ArrayList<Float>();
		List<Float> outTexCoords = new ArrayList<Float>();
		List<Float> outNormals = new ArrayList<Float>();
		List<Integer> outIndices = new ArrayList<Integer>();

		// Map full OBJ tuple "v/vt/vn" -> output vertex index
		Map<String, Integer> tupleToIndex = new HashMap<String, Integer>();

		try (BufferedReader reader = new BufferedReader(new StringReader(source))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) continue;

				if (line.startsWith("v ")) {
					String[] parts = line.split("\\s+");
					Vec3 v = new Vec3(
							Float.parseFloat(parts[1]),
							Float.parseFloat(parts[2]),
							Float.parseFloat(parts[3]));
					inPositions.add(v);
				} else if (line.startsWith("vt ")) {
					String[] parts = line.split("\\s+");
					Vec2 vt = new Vec2(
							Float.parseFloat(parts[1]),
							Float.parseFloat(parts[2]));
					inTexCoords.add(vt);
				} else if (line.startsWith("vn ")) {
					String[] parts = line.split("\\s+");
					Vec3 vn = new Vec3(
							Float.parseFloat(parts[1]),
							Float.parseFloat(parts[2]),
							Float.parseFloat(parts[3]));
					inNormals.add(vn);
				} else if (line.startsWith("f ")) {
					String[] parts = line.split("\\s+");
					// Face can be triangle/quad/ngon.
					List<Integer> faceCornerIndices = new ArrayList<Integer>();
					for (int i = 1; i < parts.length; i++) {
						String tuple = parts[i];
						if (tuple.length() == 0) continue;

						Integer outIndex = tupleToIndex.get(tuple);
						if (outIndex == null) {
							String[] comps = tuple.split("/");
							int vIndex = parseIndex(comps, 0, inPositions.size());
							int vtIndex = parseIndex(comps, 1, inTexCoords.size());
							int vnIndex = parseIndex(comps, 2, inNormals.size());

							Vec3 pos = inPositions.get(vIndex);
							outPositions.add(pos.x);
							outPositions.add(pos.y);
							outPositions.add(pos.z);

							if (vtIndex >= 0) {
								Vec2 uv = inTexCoords.get(vtIndex);
								outTexCoords.add(uv.x);
								outTexCoords.add(1 - uv.y);
							} else {
								outTexCoords.add(0f);
								outTexCoords.add(0f);
							}

							if (vnIndex >= 0) {
								Vec3 n = inNormals.get(vnIndex);
								outNormals.add(n.x);
								outNormals.add(n.y);
								outNormals.add(n.z);
							} else {
								outNormals.add(0f);
								outNormals.add(0f);
								outNormals.add(0f);
							}

							outIndex = (outPositions.size() / 3) - 1;
							tupleToIndex.put(tuple, outIndex);
						}

						faceCornerIndices.add(outIndex);
					}

					// Triangulate via fan: (0, i, i+1)
					for (int i = 1; i + 1 < faceCornerIndices.size(); i++) {
						outIndices.add(faceCornerIndices.get(0));
						outIndices.add(faceCornerIndices.get(i));
						outIndices.add(faceCornerIndices.get(i + 1));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			// Encourage GC; also avoids accidental reuse.
			inPositions.clear();
			inTexCoords.clear();
			inNormals.clear();
			tupleToIndex.clear();
		}

		float[] verticesArray = toFloatArray(outPositions);
		float[] textureArray = outTexCoords.isEmpty() ? null : toFloatArray(outTexCoords);
		float[] normalsArray = outNormals.isEmpty() ? null : toFloatArray(outNormals);
		int[] indicesArray = toIntArray(outIndices);

		// Clear temporary collections
		outPositions.clear();
		outTexCoords.clear();
		outNormals.clear();
		outIndices.clear();

		if (textureArray == null) textureArray = new float[(verticesArray.length / 3) * 2];
		if (normalsArray == null) normalsArray = computeNormals(verticesArray, indicesArray);


		return new Mesh(verticesArray, textureArray,indicesArray, normalsArray);
	}

	/** Smooth vertex normals: sum of area-weighted face normals at each vertex, normalized. */
	private static float[] computeNormals(float[] positions, int[] indices) {
		float[] normals = new float[positions.length];
		for (int i = 0; i + 2 < indices.length; i += 3) {
			int a = indices[i] * 3, b = indices[i + 1] * 3, c = indices[i + 2] * 3;
			float e1x = positions[b] - positions[a], e1y = positions[b + 1] - positions[a + 1], e1z = positions[b + 2] - positions[a + 2];
			float e2x = positions[c] - positions[a], e2y = positions[c + 1] - positions[a + 1], e2z = positions[c + 2] - positions[a + 2];
			float nx = e1y * e2z - e1z * e2y;
			float ny = e1z * e2x - e1x * e2z;
			float nz = e1x * e2y - e1y * e2x;
			for (int v : new int[] {a, b, c}) {
				normals[v] += nx; normals[v + 1] += ny; normals[v + 2] += nz;
			}
		}
		for (int i = 0; i < normals.length; i += 3) {
			float len = (float) Math.sqrt(normals[i] * normals[i] + normals[i + 1] * normals[i + 1] + normals[i + 2] * normals[i + 2]);
			if (len > 1e-12f) {
				normals[i] /= len; normals[i + 1] /= len; normals[i + 2] /= len;
			}
		}
		return normals;
	}

	private static int parseIndex(String[] comps, int compIndex, int size) {
		if (comps.length <= compIndex || comps[compIndex].length() == 0) return -1;
		int idx = Integer.parseInt(comps[compIndex]);
		if (idx > 0) return idx - 1;
		// Negative indices are relative to the end of the currently-defined list.
		return size + idx;
	}

	private static float[] toFloatArray(List<Float> list) {
		float[] arr = new float[list.size()];
		for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
		return arr;
	}

	private static int[] toIntArray(List<Integer> list) {
		int[] arr = new int[list.size()];
		for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
		return arr;
	}
}
