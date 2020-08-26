package net.benjaminurquhart.gmparser.resources;

import net.benjaminurquhart.gmparser.GMDataFile;
import net.benjaminurquhart.gmparser.iff.IFFChunk;

public class RoomResource extends Resource {

	public RoomResource(GMDataFile dataFile, IFFChunk source, int offset) {
		super(source, offset);
	}

}
