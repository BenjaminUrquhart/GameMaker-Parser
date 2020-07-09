package net.benjaminurquhart.gmparser.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import net.benjaminurquhart.gmparser.GMDataFile;
import net.benjaminurquhart.gmparser.iff.IFFChunk;

public class AudioResource extends Resource {
	
	public static enum Flag {
		REGULAR(0x64),
		EMBEDDED(0x01),
		COMPRESSED(0x02);
		
		private final int mask;
		
		private Flag(int mask) {
			this.mask = mask;
		}
		
		public int getMask() {
			return mask;
		}
		
		public static Set<Flag> parse(int flags) {
			Set<Flag> out = new HashSet<>();
			for(Flag flag : values()) {
				if((flag.mask&flags) == flag.mask) {
					out.add(flag);
				}
			}
			return out;
		}
	}
	
	private byte[] bytes;
	
	private AudioGroupResource group;
	private GMDataFile dataFile;
	private String filename;
	private String name;
	
	private Set<Flag> flags;
	
	public AudioResource(GMDataFile dataFile, IFFChunk source, int offset, int length) {
		super(source, offset, length);
		this.dataFile = dataFile;
	}
	
	public void setAudioGroup(AudioGroupResource group) {
		this.group = group;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public void setFlags(Set<Flag> flags) {
		this.flags = flags;
	}
	public void setFlags(int flags) {
		this.flags = Flag.parse(flags);
	}
	public void setName(String name) {
		this.name = name;
	}
	public void verify() {
		if(this.isEmbedded() && this.getSource() == null) {
			throw new IllegalStateException("resource marked as embedded but has no parent (Offending resource: " + this + ")");
		}
	}
	
	public String getName() {
		return name;
	}
	public String getFilename() {
		return filename;
	}
	public Set<Flag> getFlags() {
		return Collections.unmodifiableSet(flags);
	}
	public boolean isEmbedded() {
		return flags.contains(Flag.COMPRESSED) || flags.contains(Flag.EMBEDDED);
	}
	public AudioGroupResource getAudioGroup() {
		return group;
	}
	public AudioInputStream getAudio() throws UnsupportedAudioFileException, IOException {
		return AudioSystem.getAudioInputStream(this.getStream());
	}
	@Override
	public int getLength() {
		if(this.isEmbedded()) {
			return super.getLength();
		}
		return bytes == null ? this.getBytes().length : bytes.length;
	}
	@Override
	public byte[] getBytes() {
		if(this.isEmbedded()) {
			return super.getBytes();
		}
		if(bytes == null) {
			File file = new File(dataFile.getAssetsFolder(), filename);
			try(InputStream stream = new FileInputStream(file)) {
				bytes = new byte[(int)file.length()];
				
				int read = 0;
				
				while((read = stream.read(bytes, read, bytes.length-read)) != -1);
				stream.close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		if(bytes == null) {
			bytes = new byte[0];
		}
		return bytes.length > 0 ? Arrays.copyOf(bytes, bytes.length) : bytes;
	}
	@Override
	public String toString() {
		return String.format(
				"AudioResource [offset=0x%08x, name=%s, path=%s, flags=%s]",
				this.getOffset() + (this.getSource() == null ? 0 : this.getSource().getOffset()+12), 
				name, 
				filename, 
				flags
		);
	}
}
