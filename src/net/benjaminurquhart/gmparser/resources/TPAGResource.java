package net.benjaminurquhart.gmparser.resources;

import java.awt.image.BufferedImage;

import net.benjaminurquhart.gmparser.GMDataFile;
import net.benjaminurquhart.gmparser.iff.IFFChunk;

public class TPAGResource extends Resource {

	private int x, y, renderX, renderY, width, height;
	private TextureResource sheet;
	
	public TPAGResource(GMDataFile dataFile, IFFChunk source, int offset) {
		super(source, offset, 22);
		
		byte[] bytes = this.getBytes();
		int coords = source.readInt(offset);
		int render = source.readInt(offset+8);
		int dimensions = source.readInt(offset+4);
		int sheetIndex = (bytes[21]<<8)|bytes[20];
		
		this.x = coords&0xffff;
		this.y = (coords>>16)&0xffff;
		this.width = dimensions&0xffff;
		this.height = (dimensions>>16)&0xffff;
		
		this.renderX = render&0xffff;
		this.renderY = (render>>16)%0xffff;
		
		//System.out.printf("0x%04x\n", sheetIndex);
		//sheetIndex = ((sheetIndex&0xff)<<8)|(sheetIndex>>8);
		//System.out.printf("0x%04x\n", sheetIndex);
		
		this.sheet = dataFile.getTextures().get(sheetIndex);
		
		if(x < 0 || y < 0) {
			StringBuilder byteStr = new StringBuilder();
			for(byte b : this.getBytes()) {
				byteStr.append(String.format("0x%02x ", b));
			}
			throw new IllegalStateException("Illegal sprite location: (" + x + ", " + y + "). Bytes: " + byteStr.toString().trim());
		}
		if(width < 0 || width < 0) {
			throw new IllegalStateException("Illegal sprite dimensions: [" + width + " x " + height + "]");
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
	public int getRenderX() {
		return renderX;
	}
	public int getRenderY() {
		return renderY;
	}
	public TextureResource getSpriteSheet() {
		return sheet;
	}
	
	@Override
	public String toString() {
		return String.format(
				"TPAGResource [coords=[%d, %d], dim=(%d x %d), sheet=%s]",
				x,
				y,
				width,
				height,
				sheet
		);
	}
}
