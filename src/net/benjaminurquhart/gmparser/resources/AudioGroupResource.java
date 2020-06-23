package net.benjaminurquhart.gmparser.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AudioGroupResource extends Resource {

	private int index;
	private String name;
	private List<AudioResource> members;
	
	public AudioGroupResource(StringResource name, int index) {
		this(name.getString(), index);
	}
	public AudioGroupResource(String name, int index) {
		super(null, 0, 0);
		
		this.members = new ArrayList<>();
		this.index = index;
		this.name = name;
	}
	
	@Override
	public byte[] getBytes() {
		return new byte[0];
	}
	@Override
	public int getLength() {
		return members.size();
	}
	
	public int getIndex() {
		return index;
	}
	public String getName() {
		return name;
	}
	public List<AudioResource> getMembers() {
		return Collections.unmodifiableList(members);
	}
	
	public void addMember(AudioResource member) {
		member.setAudioGroup(this);
		members.add(member);
	}
	
	@Override
	public String toString() {
		return String.format("AudioGroupResource [name=%s, length=%d]", name, members.size());
	}
}
