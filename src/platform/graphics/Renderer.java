package platform.graphics;

import entities.Camera;
import entities.Entity;
import entities.Light;
import math.Mat4;
import math.Maths;
import math.Vec3;
import platform.display.DisplayManager;

public class Renderer {
	public static final float FOV_Y_DEGREES = 70f;
	private Rasterizer rasterizer;
	private Camera mainCamera;
	private TextureManager textureManager;
	
	public Mat4 viewMatrix 		 = new Mat4();
	public Mat4 projectionMatrix = new Mat4();
	public Mat4 transformMatrix  = new Mat4();
	private final Mat4 mvpMatrix = new Mat4();
	private final Mat4 pvMatrix = new Mat4();
	
	private int clearColor = 0;
	
	public Renderer() {
		rasterizer = new Rasterizer(DisplayManager.getFramebuffer());	
		Maths.viewMatrix(viewMatrix, new Vec3(0,0,0), 0f, 0f, 0f);
		Maths.perspective(projectionMatrix, FOV_Y_DEGREES, 0.1f, 100f);
	}
	public void setMainCamera(Camera camera) 	{this.mainCamera = camera;}
	public void setClearColor(int color) 		{clearColor = color;}	
	public void setTextureManager(TextureManager textureManager) {
		this.textureManager = textureManager;
		this.rasterizer.setTextureManager(textureManager);
	}
	public void setLight(Light light) {
		this.rasterizer.setLight(light);
	}
	
	
	public void render(Entity entity) {
		
		Maths.transform(transformMatrix, entity.getPosition(), 
				   						 entity.getPitch(), 
				   						 entity.getYaw(), 
				   						 entity.getRoll(), 
				   						 entity.getScale());
		// MVP = (P*V) * model (column-vector order)
		mvpMatrix.multiply(pvMatrix, transformMatrix);
		rasterizer.setMVP(mvpMatrix);
		rasterizer.setModelMatrix(transformMatrix);
		rasterizer.setLit(entity.isLit());
		if (textureManager != null) {
			textureManager.activeTexture(0);
			textureManager.bindTexture(entity.getTextureId());
		}
		int vaoId = entity.getVaoId();
		rasterizer.drawArrays(vaoId, 3, 0, rasterizer.getVertexCount(vaoId));
	}
	
	public void prepare() {
		rasterizer.clearColor(clearColor);
		Maths.viewMatrix(viewMatrix, mainCamera.getPosition(), 
									mainCamera.getPitch(), 
									mainCamera.getYaw(), 
									mainCamera.getRoll());
		pvMatrix.multiply(projectionMatrix, viewMatrix);
	}
	
}
