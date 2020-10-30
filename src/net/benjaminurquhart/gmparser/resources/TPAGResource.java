package net.benjaminurquhart.gmparser.resources;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import net.benjaminurquhart.gmparser.GMDataFile;
import net.benjaminurquhart.gmparser.iff.IFFChunk;

public class TPAGResource extends Resource {

	private int x, y, width, height, targetX, targetY, targetWidth, targetHeight, boundingWidth, boundingHeight;
	private WeakReference<BufferedImage> image;
	private TextureResource sheet;
	
	public TPAGResource(GMDataFile dataFile, IFFChunk source, int offset) {
		super(source, offset, 22);
		
		ByteBuffer buff = source.asByteBuffer();
		buff.position(offset);
		
		this.x = buff.getShort();
		this.y = buff.getShort();
		
		this.width = buff.getShort();
		this.height = buff.getShort();
		
		this.targetX = buff.getShort();
		this.targetY = buff.getShort();
		
		this.targetWidth = buff.getShort();
		this.targetHeight = buff.getShort();
		
		this.boundingWidth = buff.getShort();
		this.boundingHeight = buff.getShort();
		
		int sheetIndex = buff.getShort();
		
		this.sheet = sheetIndex < 0 ? null : dataFile.getTextures().get(sheetIndex);
		
		if(x < 0 || y < 0) {
			StringBuilder byteStr = new StringBuilder();
			for(byte b : this.getBytes()) {
				byteStr.append(String.format("0x%02x ", b));
			}
			throw new IllegalStateException("Illegal sprite location: (" + x + ", " + y + "). Bytes: " + byteStr.toString().trim());
		}
		if(width <= 0 || height <= 0) {
			throw new IllegalStateException("Illegal sprite dimensions: [" + width + " x " + height + "]");
		}
		if(sheet == null) {
			System.err.printf("WARNING: invalid sheet index %d for %s\n", sheetIndex, this);
			return;
		}
		BufferedImage sheetImage = sheet.getImage();
		if(x+width > sheetImage.getWidth()) {
			throw new IllegalStateException(
					String.format(
							"x + width (%d + %d = %d) goes out of bounds for the provided spritesheet (%s)",
							x,
							width,
							x+width,
							sheet
					)
			);
		}
		if(y+height > sheetImage.getHeight()) {
			throw new IllegalStateException(
					String.format(
							"y + height (%d + %d = %d) goes out of bounds for the provided spritesheet (%s)",
							y,
							height,
							y+height,
							sheet
					)
			);
		}
	}
	
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	public int getTargetX() {
		return targetX;
	}
	public int getTargetY() {
		return targetY;
	}
	public int getTargetWidth() {
		return targetWidth;
	}
	public int getTargetHeight() {
		return targetHeight;
	}
	public int getBoundingWidth() {
		return boundingWidth;
	}
	public int getBoundingHeight() {
		return boundingHeight;
	}
	public BufferedImage getImage() {
		BufferedImage out = image == null ? null : image.get();
		if(out == null) {
			if(sheet == null) {
				out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				
				Graphics2D graphics = out.createGraphics();
				graphics.setColor(Color.BLACK);
				graphics.fillRect(0, height/2, width/2, height/2);
				graphics.fillRect(width/2, 0,  width/2, height/2);
				
				graphics.setColor(Color.MAGENTA);
				graphics.fillRect(0, 0, width/2, height/2);
				graphics.fillRect(width/2, height/2,  width/2, height/2);
				
				graphics.dispose();
			}
			else {
				out = sheet.getImage().getSubimage(x, y, width, height);
			}
			image = new WeakReference<>(out);
		}
		return out;
	}
	public TextureResource getSpriteSheet() {
		return sheet;
	}
	
	@Override
	public String toString() {
		return String.format(
				"TPAGResource @ 0x%08x [coords=[%d, %d], dim=(%d x %d), target=([%d, %d], %d x %d), bounds=(%d x %d), sheet=%s]",
				this.getOffset()+this.getSource().getOffset()+8,
				x,
				y,
				width,
				height,
				targetX,
				targetY,
				targetWidth,
				targetHeight,
				boundingWidth,
				boundingHeight,
				sheet
		);
	}
}
