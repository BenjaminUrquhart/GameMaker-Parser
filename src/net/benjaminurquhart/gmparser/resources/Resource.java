package net.benjaminurquhart.gmparser.resources;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import net.benjaminurquhart.gmparser.iff.IFFChunk;

public abstract class Resource {

	private int offset, length;
	private IFFChunk source;
	
	protected Resource(IFFChunk source, int offset, int length) {
		if(length < 0) {
			throw new IllegalArgumentException("invalid length: " + length);
		}
		if(offset < 0) {
			throw new IllegalArgumentException("invalid offset: " + offset);
		}
		this.length = length;
		this.offset = offset;
		this.source = source;
	}
	protected Resource(IFFChunk source, int offset) {
		this(source, offset, 0);
		this.length = -1;
	}
	
	protected void updateLength(int length) {
		if(this.length < 0) {
			this.length = length;
		}
		else {
			throw new IllegalStateException("cannot set length more than once");
		}
	}
	
	public IFFChunk getSource() {
		return source;
	}
	public byte[] getBytes() {
		if(source == null) {
			throw new IllegalStateException("resources coming from external locations MUST override getBytes() (Offending resource: " + this + ")");
		}
		return Arrays.copyOfRange(source.getContents(), offset, offset+length);
	}
	public InputStream getStream() {
		return new ByteArrayInputStream(this.getBytes());
	}
	public int getOffset() {
		return offset;
	}
	public int getLength() {
		return length;
	}
	@Override
	public String toString() {
		if(source == null) {
			return String.format(
					"%s [source=EXTERNAL, 0x%08x (%d) bytes]", 
					this.getClass().getSimpleName(),
					this.getLength(),
					this.getLength()
			);
		}
		return String.format(
				"%s [source=%s, bounds=0x%08x -> 0x%08x (0x%08x (%d) bytes)]",
				this.getClass().getSimpleName(),
				source.getTypeID(),
				source.getOffset()+16+offset,
				source.getOffset()+16+offset+length,
				length,
				length
		);
	}
}
