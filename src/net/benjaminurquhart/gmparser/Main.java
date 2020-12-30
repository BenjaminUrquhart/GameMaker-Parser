package net.benjaminurquhart.gmparser;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import net.benjaminurquhart.gmparser.iff.IFFChunk;
import net.benjaminurquhart.gmparser.resources.*;

public class Main {

	public static void main(String[] args) throws Exception {
		GMDataFile data = new GMDataFile(new File(args[0]), null, true, true);
		System.out.println(data);
		

		IFFChunk seqn = data.getPrimaryChunk().getSubChunk("SEQN");
		
		PointerList list = new PointerList(seqn);
		
		list.forEach(entry -> {
			System.out.printf("--------------------------------------- %s ---------------------------------------\n", entry);
			ByteBuffer buff = ByteBuffer.wrap(entry.getBytes());
			buff.order(ByteOrder.LITTLE_ENDIAN);
			int ptr = 0, val;
			while(buff.hasRemaining()) {
				val = buff.getInt();
				System.out.printf("0x%08x (0x%04x): 0x%08x (%.2f, %d)", entry.getSource().getOffset() + 16 + entry.getOffset() + ptr, ptr, val, Float.intBitsToFloat(val), val);
				testPointer(data, data.getPrimaryChunk().getSubChunks(), seqn, val);
				System.out.println();
				ptr += 4;
			}
		});
	}
	
	public static void scanChunk(GMDataFile data, IFFChunk chunk) {
		List<IFFChunk> chunks = data.getPrimaryChunk().getSubChunks();
		
		if(!chunks.contains(chunk)) {
			throw new IllegalArgumentException(chunk + " is not a child of " + data.getPrimaryChunk());
		}
		
		int val;
		System.out.printf("Scanning chunk %s...\n", chunk.getTypeID());
		for(int ptr = 0; ptr < chunk.getLength(); ptr += 4) {
			val = chunk.readInt(ptr);
			System.out.printf("0x%08x (0x%04x): 0x%08x (%.2f)", chunk.getOffset() + 16 + ptr, ptr, val, Float.intBitsToFloat(val));
			testPointer(data, chunks, chunk, val);
			System.out.println();
		}
	}
	
	public static void scanChunk(GMDataFile data, String chunk) {
		scanChunk(data, data.getPrimaryChunk().getSubChunk(chunk));
	}
	
	public static void testPointer(GMDataFile data, List<IFFChunk> chunks, IFFChunk chunk, long val) {
		Resource resource = data.getResource(val);
		long chunkOffset;
		
		StringResource str;
		
		if(resource != null) {
			System.out.printf(" (%s)", resource);
			if(resource instanceof StringResource) {
				str = (StringResource) resource;
				
				if(str.getString().startsWith("spr_")) {
					try {
						System.out.printf(" -> %s", data.getSprite(str.getString()));
					}
					catch(Exception e) {}
				}
			}
		}
		else {
			for(IFFChunk c : chunks) {
				chunkOffset = c.getOffset() + 16;
				if(c == chunk) continue;
				if(c.getTypeID().equals("GEN8") || c.getTypeID().equals("EXTN")) continue;
				if(val >= chunkOffset && val < chunkOffset + c.getLength()) {
					System.out.printf(" (Possible %s entry?)", c.getTypeID());
				}
			}
		}
	}
}
