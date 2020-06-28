package net.benjaminurquhart.gmparser.resources;

import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import net.benjaminurquhart.gmparser.GMDataFile;
import net.benjaminurquhart.gmparser.iff.IFFChunk;

public class SpriteResource extends Resource {

	private GMDataFile dataFile;
	
	private WeakReference<BufferedImage[]> frames;
	private TPAGResource[] tpags;
	private StringResource name;
	
	private WeakReference<byte[]> animation;
	
	private int[] tpagOffsets;
	private byte[] bytes;
	
	private int nameOffset;
	
	// Sprites are a bit different than other resources
	// Most of their data is stored within the TXTR and STRG chunks rather than the SPRT chunk
	// As a result, they can't simply read the data from their parent chunk
	public SpriteResource(GMDataFile dataFile, IFFChunk source, int nameOffset, int[] tpagOffsets, int offset) {
		super(source, offset, 8);
		
		this.tpagOffsets = tpagOffsets;
		this.nameOffset = nameOffset;
		this.dataFile = dataFile;
		
		this.tpags = new TPAGResource[tpagOffsets.length];
		
		TPAGResource tpag;
		int index = 0;
		
		for(int tpagOffset : tpagOffsets) {
			tpag = dataFile.getTPAGFromAbsoluteOffset(tpagOffset);
			
			if(tpag == null) {
				throw new IllegalArgumentException(
						String.format(
								"Invalid TPAG offset 0x%08x at index %d for sprite at offset 0x%08x (%s)",
								tpagOffset,
								index,
								source.getOffset()+16+offset,
								this.getName() == null ? String.format("Missing String @ 0x%08x", nameOffset) : name.getString()
						)
				);
			}
			tpags[index++] = tpag;
		}
	}
	public StringResource getName() {
		if(name == null) {
			name = dataFile.getStringFromAbsoluteOffset(nameOffset);
		}
		return name;
	}
	public BufferedImage getTexture() {
		return this.getFrames()[0];
	}
	public BufferedImage[] getFrames() {
		BufferedImage[] out = frames == null ? null : frames.get();
		if(out == null) {
			TPAGResource tpag;
			out = new BufferedImage[tpags.length];
			frames = new WeakReference<>(out);
			
			for(int i = 0; i < tpags.length; i++) {
				tpag = tpags[i];
				out[i] = tpag.getSpriteSheet().getImage().getSubimage(tpag.getX(), tpag.getY(), tpag.getWidth(), tpag.getHeight());
			}
		}
		return Arrays.copyOf(out, out.length);
	}
	public byte[] getAsGIF() {
		byte[] bytes = animation == null ? null : animation.get();
		if(bytes == null) {
			bytes = this.getAsGIF(1);
			animation = new WeakReference<>(bytes);
		}
		return Arrays.copyOf(bytes, bytes.length);
	}
	public byte[] getAsGIF(int scale) {
		return GIFCreator.create(this, scale);
	}
	public TPAGResource[] getTPAGInfo() {
		return Arrays.copyOf(tpags, tpags.length);
	}
	@Override
	public byte[] getBytes() {
		if(bytes == null) {
			bytes = new byte[4*(tpags.length+2)];
			this.writeInt(bytes, nameOffset, 0);
			this.writeInt(bytes, tpags.length, 4);
			for(int i = 0; i < tpags.length; i++) {
				this.writeInt(bytes, tpagOffsets[i], 8+4*i);
			}
		}
		return Arrays.copyOf(bytes, bytes.length);
	}
	@Override
	public String toString() {
		return String.format("SpriteResource [offset=0x%08x, name=%s, frames=%d]", this.getSource().getOffset()+16+this.getOffset(), this.getName().getString(), tpags.length);
	}
	
	private void writeInt(byte[] buff, int data, int offset) {
		for(int i = 3; i >= 0; i++) {
			buff[offset+i] = (byte)(data&0xff);
			data>>=8;
		}
	}
}
