package net.benjaminurquhart.gmparser.iff;

import java.util.Collections;
import java.util.List;

public class IFFChunk {

	private String typeID;
	private byte[] contents;
	private boolean hasSubChunks;
	
	private IFFFile subChunks, origin;
	private IFFChunk parent;
	private long offset;
	
	public static boolean isValidOffset(long offset) {
		return (offset&3) == 0;
	}
	
	protected IFFChunk(String typeID, byte[] contents, IFFFile origin, long offset) {
		this(typeID, contents, (IFFChunk) null, offset);
		this.origin = origin;
	}
	protected IFFChunk(String typeID, byte[] contents, IFFChunk parent, long offset) {
		this.hasSubChunks = true;
		this.contents = contents;
		this.parent = parent;
		this.typeID = typeID;
		this.offset = offset;
		
		this.origin = parent == null ? null : parent.origin;
		
		try {
			this.subChunks = new IFFFile(contents);
		}
		catch(Exception e) {
			//e.printStackTrace();
			this.hasSubChunks = false;
		}
	}
	public IFFChunk getParent() {
		return parent;
	}
	public IFFFile getOrigin() {
		return origin;
	}
	public byte[] getContents() {
		return contents;
	}
	public String getTypeID() {
		return typeID;
	}
	public long getOffset() {
		return offset;
	}
	public int getLength() {
		return contents.length;
	}
	public List<IFFChunk> getSubChunks() {
		if(this.hasSubChunks) {
			return subChunks.getChunks();
		}
		return Collections.emptyList();
	}
	public IFFChunk getSubChunk(String typeID) {
		if(subChunks == null || !this.hasSubChunks) {
			throw new IllegalStateException("chunk " + this.getTypeID() + " does not contain any subchunks");
		}
		return subChunks.getChunk(typeID);
	}
	public boolean hasSubChunk(String typeID) {
		return subChunks != null && hasSubChunks && subChunks.hasChunk(typeID);
	}
	public int readInt(int offset) {
		byte[] bytes = this.read(offset, 4, new byte[4]);
		
		return ((bytes[3]&0xff)<<24)|((bytes[2]&0xff)<<16)|((bytes[1]&0xff)<<8)|(bytes[0]&0xff);
	}
	public byte readByte(int offset) {
		return this.read(offset, 1, new byte[1])[0];
	}
	public int readInt16(int offset) {
		byte[] bytes = this.read(offset, 2, new byte[2]);
		
		return ((bytes[1]&0xff)<<8)|(bytes[0]&0xff);
	}
	public byte[] read(int offset, int length, byte[] dest) {
		return this.read(offset, length, dest, 0);
	}
	public byte[] read(int offset, int length, byte[] dest, int destOffset) {
		if(offset < 0) {
			throw new IllegalArgumentException(String.format("Relative offset %d (0x%08x) < 0", offset, offset));
		}
		if(length < 1) {
			throw new IllegalArgumentException("Length " + length + " < 1");
		}
		if(!this.isWithinBounds(offset) || !this.isWithinBounds(offset+length-1)) {
			throw new IllegalArgumentException(String.format(
					"provided offset and length extends beyond chunk boundaries (%d (0x%08x) + %d > %d, chunk = %s)",
					offset,
					offset,
					length,
					contents.length,
					typeID
			));
		}
		System.arraycopy(contents, offset, dest, destOffset, length);
		return dest;
	}
	public boolean isWithinBounds(long offset) {
		return offset >= 0 && offset < contents.length;
	}
	@Override
	public String toString() {
		return String.format("%s (0x%08x bytes, found 0x%08x bytes from start of parent (%d children))", typeID, contents.length, offset, this.getSubChunks().size());
	}
}
