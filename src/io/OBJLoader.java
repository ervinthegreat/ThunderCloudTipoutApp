package io;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import platform.graphics.objects.Mesh;
import platform.io.Assets;

/** Reads only positions and faces; normals come from the triangle geometry and texture coordinates are unused. */
public class OBJLoader {

	public static Mesh loadObjModel(String fileName) {
		String source = Assets.loadText("res/"+fileName+".obj");
		if (source == null) {
			System.err.println("Couldn't load file!");
			return null;
		}

		List<Float> positions = new ArrayList<Float>();
		List<Integer> indices = new ArrayList<Integer>();

		try (BufferedReader reader = new BufferedReader(new StringReader(source))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("v ")) {
					String[] parts = line.split("\\s+");
					positions.add(Float.parseFloat(parts[1]));
					positions.add(Float.parseFloat(parts[2]));
					positions.add(Float.parseFloat(parts[3]));
				} else if (line.startsWith("f ")) {
					String[] parts = line.split("\\s+");
					// Face can be triangle/quad/ngon; each corner is "v", "v/vt", "v//vn" or "v/vt/vn".
					List<Integer> corners = new ArrayList<Integer>();
					for (int i = 1; i < parts.length; i++) {
						if (parts[i].length() == 0) continue;
						corners.add(parseIndex(parts[i].split("/")[0], positions.size() / 3));
					}
					// Triangulate via fan: (0, i, i+1)
					for (int i = 1; i + 1 < corners.size(); i++) {
						indices.add(corners.get(0));
						indices.add(corners.get(i));
						indices.add(corners.get(i + 1));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		float[] positionArray = new float[positions.size()];
		for (int i = 0; i < positionArray.length; i++) positionArray[i] = positions.get(i);
		int[] indexArray = new int[indices.size()];
		for (int i = 0; i < indexArray.length; i++) indexArray[i] = indices.get(i);
		return new Mesh(positionArray, indexArray);
	}

	private static int parseIndex(String s, int count) {
		int idx = Integer.parseInt(s);
		if (idx > 0) return idx - 1;
		// Negative indices are relative to the end of the currently-defined list.
		return count + idx;
	}
}
