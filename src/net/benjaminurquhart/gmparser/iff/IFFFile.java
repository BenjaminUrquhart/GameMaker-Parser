package net.benjaminurquhart.gmparser.iff;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// https://en.wikipedia.org/wiki/Interchange_File_Format
public class IFFFile {
	
	private Map<String, IFFChunk> chunkMap;
	private List<IFFChunk> chunks;
	
	public IFFFile(byte[] bytes) {
		this(new ByteArrayInputStream(bytes), null);
	}
	public IFFFile(File file) throws FileNotFoundException {
		this(new FileInputStream(file), null);
	}
	public IFFFile(InputStream stream) {
		this(stream, null);
	}
	protected IFFFile(InputStream stream, IFFChunk parent) {
		try {
			this.chunkMap = new HashMap<>();
			this.chunks = new ArrayList<>();
			
			byte[] smallBuff = new byte[4], buff;
			long totalRead = 0, offset;
			int chunkSize, read, total;
			String chunkID;
			
			IFFChunk chunk;
			while(stream.available() > 0) {
				total = 0;
				offset = totalRead;
				read = stream.read(smallBuff, 0, 4);
				if(read != 4) {
					throw new IllegalStateException("invalid chunk ID, expected 4 bytes, got " + read);
				}
				totalRead+=read;
				chunkID = new String(smallBuff);
				
				// Technically, a FourCC with unprintable characters is allowed.
				// However, Game Maker archives only have printable characters.
				// Since that's what we're interested in, we reject otherwise
				// valid files with nonprintable chunk IDs.
				// https://en.wikipedia.org/wiki/FourCC
				if(!chunkID.matches("\\w{4}")) {
					throw new IllegalStateException(
							String.format("invalid chunk ID, contains illegal bytes (Bytes: 0x%02x 0x%02x 0x%02x 0x%02x)", 
									smallBuff[0],
									smallBuff[1],
									smallBuff[2],
									smallBuff[3]
							)
					);
				}
				read = stream.read(smallBuff, 0, 4);
				if(read != 4) {
					throw new IllegalStateException("invalid chunk size for chunk '" + chunkID + ",' expected 4 bytes, got " + read);
				}
				totalRead+=read;
				chunkSize = ((smallBuff[3]&0xff)<<24)|((smallBuff[2]&0xff)<<16)|((smallBuff[1]&0xff)<<8)|(smallBuff[0]&0xff);
				if(chunkSize < 0) {
					chunkSize = ((smallBuff[0]&0xff)<<24)|((smallBuff[1]&0xff)<<16)|((smallBuff[2]&0xff)<<8)|(smallBuff[3]&0xff);
				}
				if(chunkSize < 0) {
					throw new IllegalStateException(
							String.format("invalid chunk size for chunk '%s,' size is negative regardless of endianess (0x%02x%02x%02x%02x, 0x%02x%02x%02x%02x)",
									chunkID,
									smallBuff[0],
									smallBuff[1],
									smallBuff[2],
									smallBuff[3],
									smallBuff[3],
									smallBuff[2],
									smallBuff[1],
									smallBuff[0]
							)
					);
				}
				buff = new byte[chunkSize];
				while(total < chunkSize) {
					read = stream.read(buff, total, chunkSize-total);
					if(read == -1) {
						throw new IllegalStateException("unexpected end of data while processing chunk '" + chunkID + ",' expected " + chunkSize + " bytes, got " + total);
					}
					totalRead+=read;
					total += read;
				}
				chunk = new IFFChunk(chunkID, buff, parent, offset);
				chunkMap.put(chunkID, chunk);
				chunks.add(chunk);
			}
		}
		catch(IllegalStateException e) {
			throw e;
		}
		catch(Exception e) {
			throw new IllegalStateException("internal exception while parsing data", e);
		}
		finally {
			try {
				stream.close();
			}
			catch(Exception e) {}
		}
	}
	public List<IFFChunk> getChunks() {
		return Collections.unmodifiableList(chunks);
	}
	public IFFChunk getChunk(String typeID) {
		if(!chunkMap.containsKey(typeID)) {
			throw new IllegalArgumentException("Unknown chunk: " + typeID);
		}
		return chunkMap.get(typeID);
	}
	@Override
	public String toString() {
		return String.format("IFFFile (%d bytes and %d children)");
	}
}
