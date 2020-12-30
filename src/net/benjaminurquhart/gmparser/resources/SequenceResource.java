package net.benjaminurquhart.gmparser.resources;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import net.benjaminurquhart.gmparser.GMDataFile;
import net.benjaminurquhart.gmparser.iff.IFFChunk;

// So, I understand why nobody wanted to do this now
// The fact that I got this far at 2 AM is a miracle
// Hopefully whatever comments I leave here are understandable
// If there's a discrepancy between code and comment, the code is probably correct?

// Maybe I'm over-complicating this?

// This is nowhere near complete, there seem to be some special cases where the general rule doesn't apply.
// Thanks YoYo.

// The SEQN chunk is a standard pointer list with an additional unknown value at the beginning (currently 1).
// Possibly a version number?

// All values in the following entries are 4 bytes long (little-endian) unless otherwise noted

/* SequenceResource 
 * Name (StringResource)
 * Unknown 0
 * Framerate? (float) - always 30 from testing, which is why I'm calling it framerate
 * Unknown 0
 * Unknown (Number of frames?) (float)
 * Unknown 0
 * Unknown 0
 * Unknown 1 (float)
 * Unknown 0 
 * Number of entries (int)
 * Entries (SequenceEntry[]) - variable length
 */
public class SequenceResource extends Resource {
	
	// TODO: Find more ids
	// This is all I could find from the game I tested (TS!UNDERSWAP)
	// I have to find more games or figure out how to use sequences
	static enum VariableID {
		
		X(2),
		Y(2),
		ROTATION(8),
		BLEND_MULTIPLY(10),
		POSITION(14),
		SCALE(15),
		ORIGIN(16),
		IMAGE_SPEED(17),
		IMAGE_INDEX(18);
		
		
		public final int ID;
	
		private VariableID(int id) {
			this.ID = id;
		}
	}
	
	/* SequenceEntry
	 * TrackType (StringResource)
	 * Name? (StringResource) - usually a sprite name, but not always
	 * Unknown (20 bytes)
	 * Number of entries (int)
	 * Entries (VariableChangeEntry[]) - variable length
	 */
	
	static class SequenceEntry {
		
		protected StringResource trackType; // Always GMGroupTrack?
		protected SpriteResource sprite;
		protected StringResource name; // The name of the sprite, except when it isn't. Confused? So am I.
		
		private int[] unknown = new int[5];
		
		protected List<VariableChangeEntry> entries = new ArrayList<>();
	}
	
	/* VariableChangeEntry
	 * TrackType (StringResource)
	 * VariableName (StringResource)
	 * VariableID (int) - see the enum for details
	 * Unknown (int[24])
	 * Number of entries (int)
	 * Entries (ValueUpdateEntry[])
	 */
	static class VariableChangeEntry {
		
		// Conbination of the fields variableName and variableID
		protected VariableID variable;
		
		protected StringResource trackType; // Always GMRealTrack?
		protected StringResource variableName;
		protected int variableID;
		
		protected int[] unknown = new int[6];
		protected List<ValueUpdateEntry> entries = new ArrayList<>();
		
	}
	
	/* ValueUpdateBlob
	 * Value A (float) - not sure what these are for
	 * Value B (float) - see above
	 * Unknown (int[2])
	 * Number of blobs? (int) - Subtract 1, which is why I'm wondering if it's a counter or some sort of type
	 * Blobs (UnknownBlob[])
	 * OPTIONAL: NaN (float) (huh?)
	 */
	static class ValueUpdateEntry {
		
		protected float valueA, valueB;
		protected int[] unknown1 = new int[2];
		protected int[] unknown2 = new int[3];
		
		protected List<UnknownBlob> blobs = new ArrayList<>();
		
	}
	
	/* UnknownBlob
	 * Unknown (int)
	 * Value (float)
	 * Unknown (int)
	 */
	static class UnknownBlob {
		protected int unknown1; // Always 1?
		protected int unknown2;
		protected float value;
	}

	private StringResource name;
	private float framerate, unknown, unknown2;
	
	private List<SequenceEntry> entries;
	
	protected SequenceResource(GMDataFile data, IFFChunk source, int offset, int length) {
		super(source, offset, length);
		
		ByteBuffer buff = ByteBuffer.wrap(this.getBytes());
		buff.order(ByteOrder.LITTLE_ENDIAN);
		
		this.name = data.getStringFromAbsoluteOffset(buff.getInt());
		buff.getInt(); // Always 0?
		this.framerate = buff.getFloat();
		buff.getInt(); // Always 0?
		this.unknown = buff.getFloat();
		buff.getInt(); // Always 0?
		buff.getInt(); // Always 0?
		this.unknown2 = buff.getFloat(); // Always 1?
		buff.getInt(); // Always 0?
		
		int numEntries = buff.getInt(), numVarEntries, numChanges, numBlobs;
		VariableChangeEntry varEntry;
		ValueUpdateEntry valEntry;
		SequenceEntry entry;
		UnknownBlob blob;
		
		while(numEntries-- > 0) {
			entry = new SequenceEntry();
			entry.trackType = data.getStringFromAbsoluteOffset(buff.getInt());
			entry.name = data.getStringFromAbsoluteOffset(buff.getInt());
			try {
				entry.sprite = data.getSprite(entry.name.getString()); // ???
			}
			catch(IllegalArgumentException e) {} // Invalid sprite
			for(int i = 0; i < entry.unknown.length; i++) {
				entry.unknown[i] = buff.getInt();
			}
			numVarEntries = buff.getInt();
			while(numVarEntries-- > 0) {
				varEntry = new VariableChangeEntry();
				varEntry.trackType = data.getStringFromAbsoluteOffset(buff.getInt());
				if(varEntry.trackType == null) {
					throw new IllegalStateException(String.format("Misaligned at 0x%08x", source.getOffset() + 16 + buff.position()));
				}
				varEntry.variableName = data.getStringFromAbsoluteOffset(buff.getInt());
				varEntry.variableID = buff.getInt();
				try {
					varEntry.variable = VariableID.valueOf(varEntry.variableName.getString().toUpperCase());
				}
				catch(IllegalArgumentException e) {
					System.err.printf("Warning: unknown variable %s with id %d\n", varEntry.variableName.getString(), varEntry.variableID);
				}
				for(int i = 0; i < varEntry.unknown.length; i++) {
					varEntry.unknown[i] = buff.getInt();
				}
				numChanges = buff.getInt();
				
				while(numChanges-- > 0) {
					valEntry = new ValueUpdateEntry();
					valEntry.valueA = buff.getFloat();
					valEntry.valueB = buff.getFloat();
					for(int i = 0; i < valEntry.unknown1.length; i++) {
						valEntry.unknown1[i] = buff.getInt();
					}
					numBlobs = buff.getInt() - 1;
					for(int i = 0; i < valEntry.unknown2.length; i++) {
						valEntry.unknown2[i] = buff.getInt();
					}
					
					while(numBlobs-- > 0) {
						buff.getFloat(); // Always NaN?
						blob = new UnknownBlob();
						blob.unknown1 = buff.getInt();
						blob.value = buff.getFloat();
						blob.unknown2 = buff.getInt();
						valEntry.blobs.add(blob);
					}
					// I don't know either
					// I'm not here to ask questions, only answer them
					if(Float.isNaN(buff.getFloat(buff.position()))) {
						buff.getFloat();
					}
 					varEntry.entries.add(valEntry);
				}
				entry.entries.add(varEntry);
			}
			entries.add(entry);
		}
	}

}
