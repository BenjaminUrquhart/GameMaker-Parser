package net.benjaminurquhart.gmparser.resources;

import net.benjaminurquhart.gmparser.iff.IFFChunk;

public class RawResource extends Resource {

	public RawResource(IFFChunk source, int offset, int length) {
		super(source, offset, length);
	}

}
