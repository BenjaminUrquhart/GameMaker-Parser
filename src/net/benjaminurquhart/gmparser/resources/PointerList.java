package net.benjaminurquhart.gmparser.resources;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.benjaminurquhart.gmparser.iff.IFFChunk;

public class PointerList extends AbstractList<Resource> {

	private Map<Long, Resource> pointerMap;
	private List<Resource> resources;
	private IFFChunk chunk;
	private int size;
	
	public PointerList(IFFChunk chunk) {
		this.chunk = chunk;
		
		pointerMap = new HashMap<>();
		resources = new ArrayList<>();
		
		int offset = 0;
		
		if(chunk.getTypeID().equals("SEQN")) {
			offset = 4;
		}
		
		int numItems = chunk.readInt(offset);
		int itemPtr = -1, previous = -1;
		
		this.size = numItems;
		
		if(numItems == 0) {
			return;
		}
		
		List<Integer> pointers = new ArrayList<>();
		List<Integer> lengths = new ArrayList<>();
		
		for(int i = 0; i < numItems; i++) {
			itemPtr = chunk.readInt(offset + 4 + i*4);
			pointers.add(itemPtr);
			if(previous > 0) {
				lengths.add(itemPtr-previous);
			}
			if(i != numItems-1) {
				previous = itemPtr;
			}
		}
		
		lengths.add(itemPtr-previous);
		
		int relativePtr;
		
		RawResource resource;
		
		//System.out.println(pointers);
		//System.out.println(lengths);
		
		for(int i = 0; i < numItems; i++) {
			itemPtr = pointers.get(i);
			relativePtr = (int)(itemPtr-(chunk.getOffset()+16));
			resource = new RawResource(chunk, relativePtr, lengths.get(i));
			pointerMap.put((long)itemPtr, resource);
			resources.add(resource);
		}
	}
	
	@Override
	public Resource get(int index) {
		return resources.get(index);
	}
	
	public Resource getAtPointer(long pointer) {
		return pointerMap.get(pointer);
	}
	
	public IFFChunk getChunk() {
		return chunk;
	}

	@Override
	public int size() {
		return size;
	}
}
