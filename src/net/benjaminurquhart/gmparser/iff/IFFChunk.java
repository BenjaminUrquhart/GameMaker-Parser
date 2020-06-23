package net.benjaminurquhart.gmparser.iff;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IFFChunk {

	private String typeID;
	private byte[] contents;
	private boolean hasSubChunks;
	
	private IFFFile subChunks, origin;
	private IFFChunk parent;
	private long offset;
	
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
		return subChunks == null ? null : subChunks.getChunk(typeID);
	}
	public int readInt(int offset) {
		byte[] bytes = Arrays.copyOfRange(contents, offset, offset+4);
		return ((bytes[3]&0xff)<<24)|((bytes[2]&0xff)<<16)|((bytes[1]&0xff)<<8)|(bytes[0]&0xff);
	}
	public void read(int offset, int length, byte[] dest) {
		this.read(offset, length, dest, 0);
	}
	public void read(int offset, int length, byte[] dest, int destOffset) {
		System.arraycopy(contents, offset, dest, destOffset, length);
	}
	@Override
	public String toString() {
		return String.format("%s (0x%08x bytes, found 0x%08x bytes from start of parent (%d children))", typeID, contents.length, offset, this.getSubChunks().size());
	}
}
