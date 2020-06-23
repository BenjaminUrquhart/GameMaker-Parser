package net.benjaminurquhart.gmparser.resources;

import net.benjaminurquhart.gmparser.iff.IFFChunk;

public class StringResource extends Resource {

	private String string;
	
	public StringResource(IFFChunk source, int offset, int length) {
		super(source, offset, length);
	}
	
	public String getString() {
		if(string == null) {
			string = new String(this.getBytes());
		}
		return string;
	}
	@Override
	public String toString() {
		long absoluteOffset = this.getSource().getOffset()+this.getOffset()+16;
		return String.format("StringResource [bounds=%d -> %d, text=%s]", absoluteOffset, absoluteOffset+this.getLength(), this.getString());
	}
}
