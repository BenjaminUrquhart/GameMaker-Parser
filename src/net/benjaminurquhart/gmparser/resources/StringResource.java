package net.benjaminurquhart.gmparser.resources;

import net.benjaminurquhart.gmparser.iff.IFFChunk;

public class StringResource extends Resource {

	private String string, parsed;
	
	public StringResource(IFFChunk source, int offset, int length) {
		super(source, offset, length);
	}
	
	public String getString() {
		if(string == null) {
			string = new String(this.getBytes());
		}
		return string;
	}
	public String getParsedString() {
		if(parsed == null) {
			String out = this.getString();
			parsed = out.replaceAll("\\\\[A-Z][0-9]?", "")
						.replaceAll("\\\\\\[.\\]", "???")
						.replaceAll("\\^\\d", "")
						.replaceAll("/%{0,}$", "")
						.replace("&", "\n");
		}
		return parsed;
	}
	@Override
	public String toString() {
		long absoluteOffset = this.getSource().getOffset()+this.getOffset()+16;
		return String.format("StringResource [bounds=%d -> %d, text=%s]", absoluteOffset, absoluteOffset+this.getLength(), this.getString());
	}
}
