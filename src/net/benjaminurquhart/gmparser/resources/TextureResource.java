package net.benjaminurquhart.gmparser.resources;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.WeakReference;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import net.benjaminurquhart.gmparser.iff.IFFChunk;

public class TextureResource extends Resource {

	private WeakReference<BufferedImage> texture;
	private String format;
	
	public TextureResource(IFFChunk source, int offset, int length) {
		super(source, offset, length);
	}
	
	public BufferedImage getImage() {
		BufferedImage out = texture == null ? null : texture.get();
		if(out == null) {
			try {
				out = ImageIO.read(this.getStream());
				texture = new WeakReference<>(out);
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
		return out;
	}
	public String getImageFormat() {
		if(format != null) {
			return format;
		}
		try {
			ImageInputStream stream = ImageIO.createImageInputStream(this.getStream());
			format = ImageIO.getImageReaders(stream).next().getFormatName().toUpperCase();
			stream.close();
		}
		catch(Exception e) {
			e.printStackTrace();
			return "INVALID";
		}
		return format;
	}
	
	@Override
	public String toString() {
		String dimX = "???", dimY = "???";
		BufferedImage image = this.getImage();
		if(image != null) {
			dimX = String.valueOf(this.getImage().getWidth());
			dimY = String.valueOf(this.getImage().getHeight());
		}
		return String.format(
				"TextureResource [%s image (%s x %s) @ 0x%08x]",
				this.getImageFormat(),
				dimX,
				dimY,
				this.getSource().getOffset()+16+this.getOffset()
		);
	}
}
