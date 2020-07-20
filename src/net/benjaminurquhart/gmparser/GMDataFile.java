package net.benjaminurquhart.gmparser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.benjaminurquhart.gmparser.resources.*;
import net.benjaminurquhart.gmparser.iff.*;

// New reference document: https://pcy.ulyssis.be/undertale/unpacking-corrected
public class GMDataFile {
	
	private File file, folder;
	
	private IFFFile resources;
	
	private IFFChunk main, audioChunk, textureChunk, spriteChunk, tpagChunk, fontChunk, stringChunk, audioMetaChunk, audioGroupChunk;
	
	private Map<Long, StringResource> stringOffsetTable;
	private Map<Long, TPAGResource> tpagOffsetTable;
	
	private Map<String, BufferedImage> rawValueCache;
	
	private Map<String, AudioGroupResource> audioGroupTable;
	private Map<String, SpriteResource> spriteTable;
	private Map<String, AudioResource> audioTable;
	private Map<String, FontResource> fontTable;
	private Map<Long, Resource> objectTable;
	
	private List<AudioGroupResource> audioGroups;
	private List<TextureResource> textures;
	private List<SpriteResource> sprites;
	private List<StringResource> strings;
	private List<AudioResource> audio;
	private List<FontResource> fonts;
	private List<TPAGResource> tpags;
	
	private long absoluteStringOffset, absoluteTextureOffset, absoluteSpriteOffset, absoluteAudioOffset, absoluteTPAGOffset, absoluteFontOffset, absoluteAudioMetaOffset;
	
	private int gameMakerVersion = 1, gameMakerMinor = 0, bytecodeVersion;
	private String game = "???";
	
	private Set<File> audioSupplements;
	private boolean autoAudioSearch;
	private boolean missingAudio;
	
	public GMDataFile(File file) {
		this(file, file.getParentFile());
	}
	public GMDataFile(File file, File assetsFolder) {
		this(file, assetsFolder, false);
	}
	public GMDataFile(File file, boolean autoAudioSearch) {
		this(file, null, autoAudioSearch);
	}
	public GMDataFile(File file, File assetsFolder, boolean autoAudioSearch) {
		if(file == null) {
			throw new IllegalArgumentException("file cannot be null");
		}
		if(!file.exists()) {
			throw new IllegalStateException("asset file not found: " + file.getAbsolutePath());
		}
		if(file.isDirectory()) {
			throw new IllegalArgumentException("asset file is a directory: " + file.getAbsolutePath());
		}
		if(assetsFolder == null) {
			if(file.getParent() == null) {
				throw new IllegalStateException("asset file does not have a parent and no asset folder provided");
			}
			assetsFolder = file.getParentFile();
		}
		else if(!assetsFolder.isDirectory()) {
			throw new IllegalArgumentException("asset folder is a file: " + assetsFolder.getAbsolutePath());
		}
		try {
			this.autoAudioSearch = autoAudioSearch;
			this.folder = assetsFolder;
			this.file = file;
			
			resources = new IFFFile(file);
			
			try {
				main = resources.getChunk("FORM");
				
				if(main.hasSubChunk("AGRP")) {
					audioGroupChunk = main.getSubChunk("AGRP");
				}
				
				audioMetaChunk = main.getSubChunk("SOND");
				textureChunk = main.getSubChunk("TXTR");
				spriteChunk = main.getSubChunk("SPRT");
				stringChunk = main.getSubChunk("STRG");
				audioChunk = main.getSubChunk("AUDO");
				fontChunk = main.getSubChunk("FONT");
				tpagChunk = main.getSubChunk("TPAG");
			}
			catch(IllegalArgumentException e) {
				System.err.println("Malformed GameMaker archive");
				System.err.println("Resource file structure:");
				System.err.print(buildRecursiveTree(resources.getChunks()));
				throw e;
			}
			
			absoluteAudioMetaOffset = getOffset(audioMetaChunk);
			absoluteTextureOffset = getOffset(textureChunk);
			absoluteSpriteOffset = getOffset(spriteChunk);
			absoluteStringOffset = getOffset(stringChunk);
			absoluteAudioOffset = getOffset(audioChunk);
			absoluteFontOffset = getOffset(fontChunk);
			absoluteTPAGOffset = getOffset(tpagChunk);
			
			objectTable = new HashMap<>();
			
			initStringTable();
			
			try {
				IFFChunk meta = main.getSubChunk("GEN8");
				gameMakerVersion = meta.readInt(44);
				bytecodeVersion = meta.readByte(1);
				gameMakerMinor = meta.readInt(48);
				
				
				StringResource name = getStringFromAbsoluteOffset(meta.readInt(40));
				game = name.getString();
			}
			
			catch(Exception e) {}
			
			initTextures();
			initTPAG();
			
			initSprites();
			initFonts();
			
			initAudio();
			initAudioGroups();
			initSoundMetadata();
			
			textures.forEach(TextureResource::allowGC);
		}
		catch(RuntimeException e) {
			System.err.printf("Error while processing game '%s' (GM Version %d.%d)\n", game, gameMakerVersion, gameMakerMinor);
			throw e;
		}
		catch(Throwable e) {
			System.err.printf("Error while processing game '%s' (GM Version %d.%d)\n", game, gameMakerVersion, gameMakerMinor);
			throw new RuntimeException(e);
		}
	}
	private long getOffset(IFFChunk chunk) {
		return chunk.getOffset()+16;
	}
	
	/* |----------------------CHUNK FORMAT----------------------|
	 * |    Offset    |   Size   |   Type   |    Description    |
	 * |--------------------------------------------------------|
	 * |       0      |    4     |  uint32  | Number of entries |
	 * |       4      |   4*N    | uint32[N]| Offsets to entries|
	 * |----------------------ENTRY FORMAT----------------------|
	 * |       0      |    4     |  uint32  |   String length   |
	 * |       4      |    N     |  char[N] |    Characters     |
	 * |      4+N     |    1     |   char   |  Null terminator  |
	 * |--------------------------------------------------------|
	 * |    Note: The string length ignores the terminator.     |
	 * |--------------------------------------------------------|
	 */
	private void initStringTable() {
		stringOffsetTable = new HashMap<>();
		strings = new ArrayList<>();
		
		int num = stringChunk.readInt(0);
		StringResource resource;
		
		List<Long> offsets = new ArrayList<>();
		for(int i = 0; i < num; i++) {
			offsets.add((long)stringChunk.readInt(4*(i+1)));
		}
		int relativeOffset, stringLength;
		
		for(long offset : offsets) {
			relativeOffset = (int)(offset-absoluteStringOffset);
			stringLength = stringChunk.readInt(relativeOffset);
			resource = new StringResource(stringChunk, relativeOffset+4, stringLength);
			stringOffsetTable.put(offset+4, resource);
			objectTable.put(offset+4, resource);
			strings.add(resource);
		}
	}
	
	/* |----------------------CHUNK FORMAT----------------------|
	 * |    Offset    |   Size   |   Type   |    Description    |
	 * |--------------------------------------------------------|
	 * |       0      |    4     |  uint32  | Number of entries |
	 * |       4      |   4*N    | uint32[N]| Offsets to entries|
	 * |     4+8*N    |   8*N    |  Info[N] |      Entries      |
	 * |    4+12*N    |    ?     |   N/A    |      Padding      |
	 * |      ???     |    ?     | uint8[?] |     File data     |
	 * |----------------------ENTRY FORMAT----------------------|
	 * |       0      |    4     |  uint32  |      Unknown      |
	 * |       4      |    4     |  uint32  |   Offset of data  |
	 * |--------------------------------------------------------|
	 * | Notes: GameMaker packs files together without padding. |
	 * | This means we can infer the sizes of the files by      |
	 * | comparing the current pointer to the previous one. The |
	 * | difference between the two is the file size. Tada.     |
	 * |--------------------------------------------------------|
	 */
	private void initTextures() {
		textures = new ArrayList<>();
		
		int num = textureChunk.readInt(0);
		int fileOffset = num*4;
		TextureResource resource;
		
		int objectLength = gameMakerVersion == 2 ? 12 : 8;
		
		List<Long> offsets = new ArrayList<>(), lengths = new ArrayList<>();
		for(int i = 0; i < num; i++) {
			offsets.add((long)textureChunk.readInt(fileOffset+objectLength*(i+1)));
		}
		for(int i = 1; i < num; i++) {
			lengths.add(offsets.get(i)-offsets.get(i-1));
		}
		lengths.add((textureChunk.getOffset()+textureChunk.getLength())-offsets.get(offsets.size()-1));
		long offset, length;
		int relativeOffset;
		
		for(int i = 0; i < num; i++) {
			offset = offsets.get(i);
			length = lengths.get(i);
			relativeOffset = (int)(offset-absoluteTextureOffset);
			resource = new TextureResource(textureChunk, relativeOffset, (int)length);
			objectTable.put(offset, resource);
			textures.add(resource);
		}
	}
	
	/* |----------------------CHUNK FORMAT----------------------|
	 * |    Offset    |   Size   |   Type   |    Description    |
	 * |--------------------------------------------------------|
	 * |       0      |    4     |  uint32  | Number of entries |
	 * |       4      |   4*N    | uint32[N]| Offsets to entries|
	 * |----------------------ENTRY FORMAT----------------------|
	 * |       0      |    2     |  uint16  |         x         |
	 * |       2      |    2     |  uint16  |         y         |
	 * |       4      |    2     |  uint16  |       width       |
	 * |       6      |    2     |  uint16  |       height      |
	 * |       8      |    2     |  uint16  |        ???        |
	 * |       10     |    2     |  uint16  |        ???        |
	 * |       12     |    2     |  uint16  |        ???        |
	 * |       14     |    2     |  uint16  |        ???        |
	 * |       16     |    2     |  uint16  |        ???        |
	 * |       18     |    2     |  uint16  |        ???        |
	 * |       20     |    2     |  uint16  |    TXTR index     |
	 * |--------------------------------------------------------|
	 * |  Notes: Contains coords for sprites in texture files.  |
	 * |--------------------------------------------------------|
	 */
	private void initTPAG() {
		tpagOffsetTable = new HashMap<>();
		tpags = new ArrayList<>();
		
		int num = tpagChunk.readInt(0), offset, relativeOffset;
		
		TPAGResource resource;
		for(int i = 0; i < num; i++) {
			offset = tpagChunk.readInt(4*(i+1));
			relativeOffset = (int)(offset-absoluteTPAGOffset);
			resource = new TPAGResource(this, tpagChunk, relativeOffset);
			tpagOffsetTable.put((long)offset, resource);
			objectTable.put((long)offset, resource);
			tpags.add(resource);
		}
	}
	
	/* |----------------------CHUNK FORMAT----------------------|
	 * |    Offset    |   Size   |   Type   |    Description    |
	 * |--------------------------------------------------------|
	 * |       0      |    4     |  uint32  | Number of entries |
	 * |       4      |   4*N    | uint32[N]| Offsets to entries|
	 * |----------------------ENTRY FORMAT----------------------|
	 * |       0      |    4     |  uint32  | Name (ptr to STRG)|
	 * |       4      |    52    |    ???   |        ???        |
	 * |       56     |    4     |  uint32  | Number of frames  |
	 * |     56+4*N   |   4*N    | uint32[N]| Offsets into TPAG |
	 * |--------------------------------------------------------|
	 * |    Note: All sprites will have at least one frame.     |
	 * |--------------------------------------------------------|
	 */
	private void initSprites() {
		spriteTable = new HashMap<>();
		sprites = new ArrayList<>();
		
		int num = spriteChunk.readInt(0), offset, relativeOffset, width, height, nameOffset = -1;
		int[] tpagOffsets;
		
		int tpagTableOffset = 56;
		boolean foundRealTPAG = gameMakerVersion == 1;
		
		SpriteResource resource;
		for(int i = 0; i < num; i++) {
			offset = spriteChunk.readInt(4*(i+1));
			relativeOffset = (int)(offset-absoluteSpriteOffset);
			
			try {
				nameOffset = spriteChunk.readInt(relativeOffset);
				
				width = spriteChunk.readInt(relativeOffset+4);
				height = spriteChunk.readInt(relativeOffset+8);
				// GameMaker 2.3 puts an extra byte in the sprite data, meaning we have to find
				// a new TPAG offset. Thanks.
				if(!foundRealTPAG) {
					int tmp = spriteChunk.readInt(relativeOffset+tpagTableOffset);
					if(tmp == -1) {
						tpagTableOffset += 16 + spriteChunk.readInt(relativeOffset+tpagTableOffset+4)*4;
					}
					else {
						tpagTableOffset += 20;
					}
					foundRealTPAG = true;
				}
				tpagOffsets = new int[spriteChunk.readInt(relativeOffset+tpagTableOffset)];
				
				//System.out.printf("Num offsets: %d (0x%08x)\n", tpagOffsets.length, tpagOffsets.length);
				
				for(int j = 0; j < tpagOffsets.length; j++) {
					tpagOffsets[j] = spriteChunk.readInt(relativeOffset+tpagTableOffset+4*(j+1));
					//System.out.printf("0x%08x\n", tpagOffsets[j]);
				}
				resource = new SpriteResource(this, spriteChunk, nameOffset, tpagOffsets, width, height, relativeOffset);
				spriteTable.put(resource.getName().getString(), resource);
				objectTable.put((long)offset, resource);
				sprites.add(resource);
				
				nameOffset = -1;
			}
			catch(RuntimeException e) {
				StringResource name = getStringFromAbsoluteOffset(nameOffset);
				
				System.err.printf(
						"Error while processing sprite at offset 0x%08x (0x%08x) (%s)\n", 
						offset, 
						relativeOffset, 
						name == null ? String.format("Missing String @ 0x%08x", nameOffset) : name.getString()
				);
				
				throw e;
			}
		}
	}
	/* |----------------------CHUNK FORMAT----------------------|
	 * |    Offset    |   Size   |   Type   |    Description    |
	 * |--------------------------------------------------------|
	 * |       0      |    4     |  uint32  | Number of entries |
	 * |      4*N     |    N     | uint32[N]|      Offsets      |
	 * |      8*N     |    N     | uint32[N]| Group Names (STRG)|
	 * |--------------------------------------------------------|
	 * | Note: Can be empty. A dummy group is used in that case.|
	 * |--------------------------------------------------------|
	 */
	private void initAudioGroups() {
		audioGroupTable = new HashMap<>();
		audioGroups = new ArrayList<>();
		
		if(audioGroupChunk == null) {
			return;
		}
		
		AudioGroupResource resource;
		
		int num = audioGroupChunk.readInt(0);
		
		if(num == 0) { 
			resource = new AudioGroupResource("DEFAULT", 0);
			audioGroupTable.put("DEFAULT", resource);
			audioGroups.add(resource);
			return;
		}
		StringResource name;
		int nameOffset;
		for(int i = 0; i < num; i++) {
			name = getStringFromAbsoluteOffset(nameOffset = audioGroupChunk.readInt(8+4*(i+1)));
			resource = new AudioGroupResource(name == null ? String.format("Missing String @ 0x%08x", nameOffset) : name.getString(), i);
			audioGroupTable.put(resource.getName(), resource);
			audioGroups.add(resource);
		}
	}
	
	/* |----------------------CHUNK FORMAT----------------------|
	 * |    Offset    |   Size   |   Type   |    Description    |
	 * |--------------------------------------------------------|
	 * |       0      |    4     |  uint32  | Number of entries |
	 * |       4      |   4*N    | uint32[N]| Offsets to entries|
	 * |----------------------ENTRY FORMAT----------------------|
	 * |       0      |    4     |  uint32  | Name (ptr to STRG)|
	 * |       4      |    4     |  uint32  | Flags (See Notes) |
	 * |       8      |    4     |  uint32  |FileEx(ptr to STRG)|
	 * |       12     |    4     |  uint32  | Path (ptr to STRG)|
	 * |       16     |    4     |  uint32  |        ???        |
	 * |       20     |    4     |  float   |       Volume      |
	 * |       24     |    4     |  float   |       Pitch       |
	 * |       28     |    4     |  uint32  |     AGRP index    |
	 * |       32     |    4     |  uint32  |     AUDO index    |
	 * |--------------------------------------------------------|
	 * | UPDATE: I found a new reference doc that actually      |
	 * | documents this chunk. I was mostly correct about it.   |
	 * |                                                        |
	 * | UPDATE 2: I HAVE CRACKED THE MYSTERY YES! NOW I ONLY   |
	 * | NEED TO FIND OUT HOW STUFF IN THE 'mus' FOLDER IS      |
	 * | REFERENCED.                                            |
	 * |                                                        |
	 * | Notes: This was by FAR the most infuriating chunk to   |
	 * | parse. My previous reference document made no mention  |
	 * | of it.                                                 |
	 * |                                                        |
	 * | I went in with nothing but a hex editor and optimism.  |
	 * |                                                        |
	 * | I left with crippling depression and an unhealthy      |
	 * | desire to assassinate whoever thought THIS was a good  |
	 * | way to store audio metadata. Really? You couldn't just,|
	 * | you know, store all of this WITH THE AUDIO???          |
	 * |                                                        |
	 * | I did this so you wouldn't have to. It's too late for  |
	 * | me, save yourself.                                     |
	 * |                                                        |
	 * | FLAGS:                                                 |
	 * | 0x01 -> EMBEDDED   (contained within the data file)    |
	 * | 0x02 -> COMPRESSED (READ: embedded OGG instead of WAV) |
	 * | 0x64 -> REGULAR    (all the entries are "regular")     |
	 * |                                                        |
	 * | Fun fact: any COMPRESSED files will have their FileEx  |
	 * | as ".mp3" despite them being OGG Vorbis. Go figure.    |
	 * |--------------------------------------------------------|
	 */
	private void initSoundMetadata() {
		
		// Yell at me later
		class SoundInfo implements Comparable<SoundInfo> {
			
			protected int offset, relativeOffset, index;
			protected Set<AudioResource.Flag> flags;
			protected StringResource name, path;
			protected AudioGroupResource group;

			@Override
			public int compareTo(SoundInfo o) {
				if(o.group != group) {
					return group.getIndex()-o.group.getIndex();
				}
				return index-o.index;
			}
			
			@Override
			public String toString() {
				return String.format(
						"SoundInfo [name=(%s, %s), flags=%s, offset=0x%08x, index=%d, group=%s]", 
						name.getString(), 
						path.getString(),
						flags,
						offset,
						index,
						group
				);
			}
			
		}
		
		StringResource name, path;
		boolean embedded;
		
		int num = audioMetaChunk.readInt(0), offset, relativeOffset, index;
		Set<AudioResource> processed = new HashSet<>();
		AudioResource audio;
		
		Set<AudioResource.Flag> flags;
		
		List<SoundInfo> soundInfo = new ArrayList<>();
		SoundInfo tmp;
		
		for(int i = 0, grp; i < num; i++) {
			tmp = new SoundInfo();
			tmp.offset = offset = audioMetaChunk.readInt(4*(i+1));
			tmp.relativeOffset = relativeOffset = (int)(offset-absoluteAudioMetaOffset);
			tmp.name = getStringFromAbsoluteOffset(audioMetaChunk.readInt(relativeOffset));
			tmp.flags = AudioResource.Flag.parse(audioMetaChunk.readInt(relativeOffset+4));
			tmp.path = getStringFromAbsoluteOffset(audioMetaChunk.readInt(relativeOffset+12));
			
			grp = audioMetaChunk.readInt(relativeOffset+28);
			if(grp >= audioGroups.size()) {
				for(int j = audioGroups.size(); j <= grp; j++) {
					audioGroups.add(new AudioGroupResource("ANONYMOUS_"+j, 0));
				}
			}
			tmp.group = audioGroups.get(grp);
			
			tmp.index = audioMetaChunk.readInt(relativeOffset+32);
			soundInfo.add(tmp);
		}
		Collections.sort(soundInfo);
		AudioGroupResource group;
		index = 0;
		
		for(SoundInfo info : soundInfo) {
			offset = info.offset;
			relativeOffset = info.relativeOffset;
			name = info.name;
			flags = info.flags;
			path = info.path;
			group = info.group;
			embedded = flags.contains(AudioResource.Flag.EMBEDDED) || flags.contains(AudioResource.Flag.COMPRESSED);
			
			if(index >= this.audio.size()) {
				missingAudio = true;
				break;
			}
			
			if(embedded) {
				audio = this.audio.get(index++);
			}
			else {
				audio = new AudioResource(this, null, 0, 0);
				this.audio.add(index++, audio);
			}
			
			// Sanity check
			if(!processed.add(audio)) {
				throw new IllegalStateException(String.format("encountered the same audio resource twice??? (Index: %d, Resource: %s)", index, audio));
			}
			
			audio.setFilename(path.getString());
			audio.setName(name.getString());
			
			audio.setFlags(flags);
			audio.verify();
			
			audioTable.put(path.getString(), audio);
			audioTable.put(name.getString(), audio);
			group.addMember(audio);
		}
	}
	
	/* |----------------------CHUNK FORMAT----------------------|
	 * |    Offset    |   Size   |   Type   |    Description    |
	 * |--------------------------------------------------------|
	 * |       0      |    4     |  uint32  | Number of entries |
	 * |       4      |   4*N    | uint32[N]| Offsets to entries|
	 * |       8      |    ?     |  File[N] |    Audio Files    |
	 * |----------------------ENTRY FORMAT----------------------|
	 * |       0      |    4     |  uint32  |     File size     |
	 * |       4      |    N     | uint8[N] |     File data     |
	 * |--------------------------------------------------------|
	 * | Notes: Why even HAVE the SOND chunk when you can just  |
	 * | put the metadata here??? Just WHY???                   |
	 * |--------------------------------------------------------|
	 */
	private void initAudio() {
		audioTable = new HashMap<>();
		audio = new ArrayList<>();
		
		int num = audioChunk.readInt(0), offset, relativeOffset, length;
		
		AudioResource resource;
		for(int i = 0; i < num; i++) {
			offset = audioChunk.readInt(4*(i+1));
			relativeOffset = (int)(offset-absoluteAudioOffset);
			length = audioChunk.readInt(relativeOffset);
			resource = new AudioResource(this, audioChunk, relativeOffset+4, length);
			objectTable.put((long)offset, resource);
			audio.add(resource);
			
			// I'm not sure if this will ever be true
			if(resource.getSource() == null) {
				throw new IllegalStateException("null source for newly-created audio object (" + resource + ")");
			}
		}
		
		if(!this.autoAudioSearch && !this.isMissingAudio()) {
			return;
		}
		
		try {
			File[] files = null;
			if(audioSupplements == null || audioSupplements.isEmpty()) {
				files = folder.listFiles(file -> !file.isDirectory() && !file.equals(this.file));
			}
			if(this.autoAudioSearch) {
				files = new File[audioSupplements.size()];
				files = audioSupplements.toArray(files);
			}
			if(files == null) {
				return;
			}
			IFFChunk chunk;
			IFFFile data;
			
			long absoluteOffset;
			for(File file : files) {
				try {
					data = new IFFFile(file);
					
					try {
						chunk = data.getChunk("FORM").getSubChunk("AUDO");
					}
					catch(IllegalArgumentException e) {
						continue;
					}
					absoluteOffset = getOffset(chunk);
					
					num = chunk.readInt(0);
					
					for(int i = 0; i < num; i++) {
						offset = chunk.readInt(4*(i+1));
						relativeOffset = (int)(offset-absoluteOffset);
						length = chunk.readInt(relativeOffset);
						resource = new AudioResource(this, chunk, relativeOffset+4, length);
						audio.add(resource);
					}
					
				}
				catch(Exception e) {
					
				}
			}
		}
		catch(Exception e) {
			
		}
	}
	/*
	 * 
	 * 
	 * SoonTM
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	private void initFonts() {
		fontTable = new HashMap<>();
		fonts = new ArrayList<>();
		
		int num = fontChunk.readInt(0), offset, relativeOffset, fontSize, charset, antialiasing;
		double scaleX, scaleY;
		boolean italic, bold;
		char start, end;
		
		StringResource codeName, displayName;
		int[] glyphOffsets;
		TPAGResource tpag;
		FontResource font;
		
		for(int i = 0, tmp; i < num; i++) {
			offset = fontChunk.readInt(4*(i+1));
			relativeOffset = (int)(offset-absoluteFontOffset);
			
			codeName = this.getStringFromAbsoluteOffset(fontChunk.readInt(relativeOffset));
			displayName = this.getStringFromAbsoluteOffset(fontChunk.readInt(relativeOffset+4));
			
			fontSize = fontChunk.readInt(relativeOffset+8);
			
			bold = fontChunk.readInt(relativeOffset+12) != 0;
			italic = fontChunk.readInt(relativeOffset+16) != 0;
			
			tmp = fontChunk.readInt(relativeOffset+20);
			start = (char)(tmp&0xffff);
			charset = (tmp>>16)&0xff;
			antialiasing = tmp>>24;
			
			end = (char)fontChunk.readInt(relativeOffset+24);
			tpag = this.getTPAGFromAbsoluteOffset(fontChunk.readInt(relativeOffset+28));
			
			scaleX = Float.intBitsToFloat(fontChunk.readInt(relativeOffset+32));
			scaleY = Float.intBitsToFloat(fontChunk.readInt(relativeOffset+36));
			
			glyphOffsets = new int[fontChunk.readInt(relativeOffset+40)];
			
			for(int j = 0; j < glyphOffsets.length; j++) {
				glyphOffsets[j] = fontChunk.readInt(relativeOffset+44+4*j);
			}
			font = new FontResource(fontChunk, offset, codeName, displayName, fontSize, bold, italic, start, end, charset, antialiasing, tpag, scaleX, scaleY, glyphOffsets);
			fontTable.put(displayName.getString(), font);
			fontTable.put(codeName.getString(), font);
			fonts.add(font);
			
			objectTable.put((long)offset, font);
		}
		
	}
	public StringResource getStringFromAbsoluteOffset(long offset) {
		return stringOffsetTable.get(offset);
	}
	public TPAGResource getTPAGFromAbsoluteOffset(long offset) {
		return tpagOffsetTable.get(offset);
	}
	public StringResource getStringFromOffset(long offset) {
		return getStringFromAbsoluteOffset(offset+absoluteStringOffset);
	}
	public TPAGResource getTPAGFromOffset(long offset) {
		return getTPAGFromAbsoluteOffset(offset+absoluteTPAGOffset);
	}
	public BufferedImage getRawSprite(String sprite) {
		if(rawValueCache == null) {
			rawValueCache = new HashMap<>();
		}
		if(rawValueCache.containsKey(sprite)) {
			return rawValueCache.get(sprite);
		}
		SpriteResource source = getSprite(sprite);
		if(!sprite.matches("^.+_\\d+$")) {
			return source.getTexture();
		}
		int frame = Integer.parseInt(sprite.replaceAll("^.+_(\\d+)$", "$1"));
		BufferedImage out = source.getFrames()[frame];
		rawValueCache.put(sprite, out);
		return out;
	}
	public SpriteResource getSprite(String sprite) {
		if(sprite.contains(".")) {
			sprite = sprite.substring(0, sprite.lastIndexOf("."));
		}
		if(!spriteTable.containsKey(sprite)) {
			sprite = sprite.replaceAll("_\\d+$", "");
			if(!spriteTable.containsKey(sprite)) {
				throw new IllegalArgumentException("Unknown sprite: " + sprite);
			}
		}
		return spriteTable.get(sprite);
	}
	public AudioResource getAudio(String name) {
		if(!audioTable.containsKey(name)) {
			throw new IllegalArgumentException("Unknown audio track: " + name);
		}
		return audioTable.get(name);
	}
	public Resource getObject(long offset) {
		return objectTable.get(offset);
	}
	public List<TextureResource> getTextures() {
		return Collections.unmodifiableList(textures);
	}
	public List<StringResource> getStrings() {
		return Collections.unmodifiableList(strings);
	}
	public List<SpriteResource> getSprites() {
		return Collections.unmodifiableList(sprites);
	}
	public List<AudioResource> getAudio() {
		return Collections.unmodifiableList(audio); 
	}
	public List<FontResource> getFonts() {
		return Collections.unmodifiableList(fonts);
	}
	public List<TPAGResource> getTPAGs() {
		return Collections.unmodifiableList(tpags);
	}
	public IFFChunk getPrimaryChunk() {
		return main;
	}
	public IFFFile getResources() {
		return resources;
	}
	public String getGameTitle() {
		return game;
	}
	
	public boolean addAudioResourceFile(File file) {
		if(!this.isMissingAudio()) {
			throw new IllegalStateException("no missing audio files were reported");
		}
		if(file == null) {
			throw new IllegalArgumentException("file cannot be null");
		}
		if(!file.exists()) {
			throw new IllegalArgumentException("file not found: " + file.getAbsolutePath());
		}
		if(file.isDirectory()) {
			throw new IllegalArgumentException("provided file is a directory");
		}
		if(audioSupplements == null) {
			audioSupplements = new HashSet<>();
		}
		return audioSupplements.add(file);
	}
	public void setAutoAudioSearch(boolean value) {
		this.autoAudioSearch = value;
	}
	public boolean willAutoSearchAudio() {
		return autoAudioSearch;
	}
	public void reloadAudio() {
		this.initAudioGroups();
		this.initAudio();
		
		this.initSoundMetadata();
	}
	public boolean isMissingAudio() {
		return missingAudio;
	}
	public File getAssetsFolder() {
		return folder;
	}
	public File getSourceFile() {
		return file;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format(
				"File: %s\nAssets Folder: %s\nGame: %s\nGame Maker Version: %d.%d\nBytecode Version: 0x%02x (%d)\n%sSummary:\n",
				file.getAbsolutePath(),
				folder.getAbsolutePath(),
				game, 
				gameMakerVersion,
				gameMakerMinor,
				bytecodeVersion,
				bytecodeVersion,
				buildRecursiveTree(resources.getChunks())
		));
		sb.append(String.format("Audio Tracks: %5d\n", audio.size()));

		sb.append(String.format("Textures:     %5d\n", textures.size()));
		sb.append(String.format("Sprites:      %5d\n", sprites.size()));
		sb.append(String.format("Strings:      %5d\n", strings.size()));
		return sb.toString();
	}
	
	private static String buildRecursiveTree(List<IFFChunk> chunks) {
		return buildRecursiveTree(chunks, "", new StringBuilder());
	}
	private static String buildRecursiveTree(List<IFFChunk> chunks, String pad, StringBuilder sb) {
		List<IFFChunk> children;
		for(IFFChunk chunk : chunks) {
			sb.append(String.format("%s%s\n", pad.isEmpty() ? "" : pad + " ", chunk));
			children = chunk.getSubChunks();
			if(!children.isEmpty()) {
				buildRecursiveTree(children, pad+"-", sb);
			}
		}
		return sb.toString();
	}
}
