package me.tommymyers.llo;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

public class OverlayRenderer {

    private Identifier texturee;
	private double[] texureMinX, texureMaxX;
	private double[] texureMinY, texureMaxY;
	
	public OverlayRenderer() {
        texturee = new Identifier("lightleveloverlay", "textures/overlay.png");
		texureMinX = new double[64];
		texureMaxX = new double[64];
		texureMinY = new double[64];
		texureMaxY = new double[64];
		for (int i = 0; i < 64; i++) {
			texureMinX[i] = (i % 8) / 8.0;
			texureMaxX[i] = (i % 8 + 1) / 8.0;
			texureMinY[i] = (i / 8) / 8.0;
			texureMaxY[i] = (i / 8 + 1) / 8.0;
		}
	}
	
	public void render(double x, double y, double z, ArrayList<Overlay>[][] overlays) {
		
		TextureManager tm = MinecraftClient.getInstance().getTextureManager();
        // VertexBuffer
        tm.bindTexture(texturee);
		BufferBuilder vb = Tessellator.getInstance().getBufferBuilder();
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GL11.glPushMatrix();
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ZERO);
        vb.begin(GL11.GL_QUADS, VertexFormats.POSITION_UV_COLOR);
		vb.setOffset(-x, -y, -z);
		for (int i = 0; i < overlays.length; i++)
		for (int j = 0; j < overlays[i].length; j++) {
			for (Overlay u: overlays[i][j]) {
                vb.vertex(u.x,     u.y, u.z    ).texture(texureMinX[u.index], texureMinY[u.index]).color(255, 255, 255, 255).next();
                vb.vertex(u.x,     u.y, u.z + 1).texture(texureMinX[u.index], texureMaxY[u.index]).color(255, 255, 255, 255).next();
                vb.vertex(u.x + 1, u.y, u.z + 1).texture(texureMaxX[u.index], texureMaxY[u.index]).color(255, 255, 255, 255).next();
                vb.vertex(u.x + 1, u.y, u.z    ).texture(texureMaxX[u.index], texureMinY[u.index]).color(255, 255, 255, 255).next();
				/*vb.pos(u.x,     u.y, u.z    ).tex(texureMinX[u.index], texureMinY[u.index]).color(255, 255, 255, 255).endVertex();
				vb.pos(u.x,     u.y, u.z + 1).tex(texureMinX[u.index], texureMaxY[u.index]).color(255, 255, 255, 255).endVertex();
				vb.pos(u.x + 1, u.y, u.z + 1).tex(texureMaxX[u.index], texureMaxY[u.index]).color(255, 255, 255, 255).endVertex();
				vb.pos(u.x + 1, u.y, u.z    ).tex(texureMaxX[u.index], texureMinY[u.index]).color(255, 255, 255, 255).endVertex();*/
			}
		}
		vb.setOffset(0, 0, 0);
		Tessellator.getInstance().draw();
		GL11.glPopMatrix();
		GL11.glPopAttrib();
		
	}
	
}