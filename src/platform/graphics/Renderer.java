package platform.graphics;

import entities.Entity;
import math.Mat4;
import math.Maths;
import platform.display.DisplayManager;

public class Renderer {
	public static final float FOV_Y_DEGREES = 70f;
	private Rasterizer rasterizer;
	
	public Mat4 transformMatrix  = new Mat4();
	
	private int clearColor = 0;
	
	public Renderer() {
		rasterizer = new Rasterizer(DisplayManager.getFramebuffer(), FOV_Y_DEGREES);
	}
	public void setClearColor(int color) 		{clearColor = color;}	
	
	public void render(Entity entity) {
		
		Maths.transform(transformMatrix, entity.getPosition(), 
				   						 entity.getPitch(), 
				   						 entity.getYaw(), 
				   						 entity.getRoll(), 
				   						 entity.getScale());
		rasterizer.setModelMatrix(transformMatrix);
		rasterizer.setLit(entity.isLit());
		rasterizer.setColor(entity.getColor());
		rasterizer.drawMesh(entity.getVaoId());
	}
	
	public void prepare() {
		rasterizer.clearColor(clearColor);
	}
	
}
