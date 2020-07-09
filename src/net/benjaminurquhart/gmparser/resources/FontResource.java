package net.benjaminurquhart.gmparser.resources;

import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.benjaminurquhart.gmparser.iff.IFFChunk;

public class FontResource extends Resource {
	
	private StringResource codeName, displayName;
	private boolean italic, bold;
	private char start, end;
	
	private int size, antialiasing, charset;
	private double scaleX, scaleY;
	private TPAGResource tpag;
	private Glyph[] glyphs;
	
	private Map<Character, Glyph> charMap;
	
	public static class Glyph {
		
		private WeakReference<BufferedImage> image;
		private FontResource font;
		
		private char chr;
		private int x, y, width, height;
		
		private int shift, offset;
		private GlyphKerning kerning;
		
		protected Glyph(FontResource font, char chr, int x, int y, int width, int height, int shift, int offset, GlyphKerning kerning) {
			this.kerning = kerning;
			this.offset = offset;
			this.height = height;
			this.width = width;
			this.shift = shift;
			this.font = font;
			this.chr = chr;
			this.x = x;
			this.y = y;
		}
		
		public GlyphKerning getKerning() {
			return kerning;
		}
		public BufferedImage getImage() {
			BufferedImage out = image == null ? null : image.get();
			if(out == null) {
				out = font.getSheet().getSubimage(x, y, width, height);
				image = new WeakReference<>(out);
			}
			return out;
		}
		public FontResource getFont() {
			return font;
		}
		public int getOffset() {
			return offset;
		}
		public int getHeight() {
			return height;
		}
		public int getWidth() {
			return width;
		}
		public int getShift() {
			return shift;
		}
		public char getChar() {
			return chr;
		}
		public int getX() {
			return x;
		}
		public int getY() {
			return y;
		}
		
		@Override
		public String toString() {
			return String.format("Glyph [font=[%s, %s], char=%c, kerning=%s]", font.displayName.getString(), font.codeName.getString(), chr, kerning);
		}
	}
	
	public static class GlyphKerning {
		
		int amount, other;
		
		protected GlyphKerning(int value) {
			this.amount = value>>16;
			this.other = value&0xffff;
		}
		
		public int getAmount() {
			return amount;
		}
		public int getOther() {
			return other;
		}
		@Override
		public String toString() {
			return String.format("GlyphKerning [amount=%d, other=%d]", amount, other);
		}
	}

	public FontResource(IFFChunk source, int offset, StringResource codeName, StringResource displayName, int size, boolean bold, boolean italic, char start, char end, int charset, int antialiasing, TPAGResource tpag, double scaleX, double scaleY, int[] glyphOffsets) {
		super(source, offset, 44+4*glyphOffsets.length+24*glyphOffsets.length);
		
		if(tpag == null) {
			throw new IllegalArgumentException("tpag == null");
		}
		
		this.antialiasing = antialiasing;
		this.displayName = displayName;
		this.codeName = codeName;
		this.charset = charset;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.italic = italic;
		this.start = start;
		this.tpag = tpag;
		this.end = end;
		
		this.glyphs = new Glyph[glyphOffsets.length];
		this.charMap = new HashMap<>();
		
		int relativeOffset, width, height, x, y, shift, off;
		char chr;
		
		for(int i = 0; i < glyphs.length; i++) {
			relativeOffset = glyphOffsets[i]-(int)(source.getOffset()+16);
			
			chr = (char)source.readInt16(relativeOffset);
			x = source.readInt16(relativeOffset+2);
			y = source.readInt16(relativeOffset+4);
			
			width = source.readInt16(relativeOffset+6);
			height = source.readInt16(relativeOffset+8);
			
			shift = source.readInt16(relativeOffset+10);
			off = source.readInt16(relativeOffset+12);
			
			glyphs[i] = new Glyph(this, chr, x, y, width, height, shift, off, new GlyphKerning(source.readInt(relativeOffset+14)));
			charMap.put(chr, glyphs[i]);
		}
	}
	
	public StringResource getDisplayName() {
		return displayName;
	}
	public StringResource getCodeName() {
		return codeName;
	}
	public BufferedImage getSheet() {
		return tpag.getImage();
	}
	public TPAGResource getTPAG() {
		return tpag;
	}
	public int getAntiAliasing() {
		return antialiasing;
	}
	public double getScaleX() {
		return scaleX;
	}
	public double getScaleY() {
		return scaleY;
	}
	public boolean isItalic() {
		return italic;
	}
	public boolean isBold() {
		return bold;
	}
	public int getCharset() {
		return charset;
	}
	public char getStart() {
		return start;
	}
	public char getEnd() {
		return end;
	}
	public int getSize() {
		return size;
	}
	
	public boolean canDisplay(char character) {
		return charMap.containsKey(character);
	}
	public Glyph getGlyph(char character) {
		if(!this.canDisplay(character)) {
			throw new IllegalArgumentException(String.format(
					"font '%s' (%s) cannot display the character '%c' (0x%04x)",
					displayName.getString(),
					codeName.getString(),
					character,
					character
			));
		}
		return charMap.get(character);
	}
	public Glyph[] getGlyphs() {
		return Arrays.copyOf(glyphs, glyphs.length);
	}
	
	@Override
	public String toString() {
		return String.format(
				"FontResource [name=[%s, %s], size=%d, bold=%s, italic=%s, antialiasing=%d range=[%c (0x%04x), %c (0x%04x)], scale=[%.2f, %.2f], charset=%d, glyphs=%d]",
				displayName.getString(),
				codeName.getString(),
				size,
				bold,
				italic,
				antialiasing,
				start,
				(int)start,
				end,
				(int)end,
				scaleX,
				scaleY,
				charset,
				glyphs.length
		);
	}
}
