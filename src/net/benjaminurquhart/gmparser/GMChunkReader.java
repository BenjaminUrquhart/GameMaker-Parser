package net.benjaminurquhart.gmparser;

import java.nio.ByteBuffer;

import net.benjaminurquhart.gmparser.iff.IFFChunk;
import net.benjaminurquhart.gmparser.resources.Resource;

public class GMChunkReader {

	private GMDataFile dataFile;
	private ByteBuffer reader;
	private IFFChunk chunk;
	
	public GMChunkReader(IFFChunk chunk, GMDataFile context) {
		this.dataFile = context;
		this.chunk = chunk;
		
		this.reader = chunk.asByteBuffer();
	}
	
	public <T extends Resource> T read(Class<T> clazz) {
		if(clazz == null) {
			throw new IllegalArgumentException("clazz == null");
		}
		
		int pos = reader.position();
		int ptr = reader.getInt();
		Resource resource = dataFile.getResource(ptr);
		
		if(resource == null) {
			throw new IllegalStateException(String.format("no object found at 0x%08x (referenced at 0x%08x)", ptr, chunk.getOffset()+16+pos));
		}
		if(!clazz.isAssignableFrom(resource.getClass())) {
			throw new IllegalArgumentException(String.format("found %s at 0x%08x, not %s", resource.getClass().getSimpleName(), ptr, clazz.getSimpleName()));
		}
		return clazz.cast(resource);
	}
	public int readInt() {
		return reader.getInt();
	}
	public long readLong() {
		return reader.getLong();
	}
	public short readShort() {
		return reader.getShort();
	}
	public float readFloat() {
		return reader.getFloat();
	}
	public double readDouble() {
		return reader.getDouble();
	}
}
