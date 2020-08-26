package net.benjaminurquhart.gmparser.resources;

import net.benjaminurquhart.gmparser.GMDataFile;
import net.benjaminurquhart.gmparser.iff.IFFChunk;

public class ObjectResource extends Resource {
	
	public static enum CollisionShape {
		CIRCLE,
		BOX,
		CUSTOM;
	}
	
	private GMDataFile dataFile;
	
	private StringResource name;
	private CollisionShape shape;
	private SpriteResource sprite;
	private ObjectResource parent;
	
	private int depth, parentID, mask;
	private boolean visible, solid, persistent, physics, sensor;
	private float density, restitution, group, linearDampening, angularDampening, unknown, friction, unknown2, kinematic;

	public ObjectResource(GMDataFile dataFile, IFFChunk source, int offset) {
		super(source, offset, 80);
		this.dataFile = dataFile;
		
		name = dataFile.getStringFromAbsoluteOffset(source.readInt(offset));
		
		int spriteIndex = source.readInt(offset+4);
		if(spriteIndex >= 0) {
			sprite = dataFile.getSprites().get(spriteIndex);
		}
		visible = source.readInt(offset+8) != 0;
		solid = source.readInt(offset+12) != 0;
		depth = source.readInt(offset+16);
		persistent = source.readInt(offset+20) != 0;
		parentID = source.readInt(offset+24);
		mask = source.readInt(offset+28);
		physics = source.readInt(offset+32) != 0;
		sensor = source.readInt(offset+36) != 0;
		
		shape = CollisionShape.values()[source.readInt(offset+40)];
		
		density = Float.intBitsToFloat(source.readInt(offset+44));
		restitution = Float.intBitsToFloat(source.readInt(offset+48));
		group = Float.intBitsToFloat(source.readInt(offset+52));
		linearDampening = Float.intBitsToFloat(source.readInt(offset+56));
		angularDampening = Float.intBitsToFloat(source.readInt(offset+60));
		unknown = Float.intBitsToFloat(source.readInt(offset+64));
		friction = Float.intBitsToFloat(source.readInt(offset+68));
		unknown2 = Float.intBitsToFloat(source.readInt(offset+72));
		kinematic = Float.intBitsToFloat(source.readInt(offset+76));
	}
	
	public ObjectResource getParent() {
		if(parent == null && parentID > -1) {
			parent = dataFile.getObjects().get(parentID);
		}
		return parent;
	}
	public SpriteResource getSprite() {
		return sprite;
	}
	public CollisionShape getShape() {
		return shape;
	}
	public StringResource getName() {
		return name;
	}
	
	
	public float getAngularDampening() {
		return angularDampening;
	}
	public float getLinearDampening() {
		return linearDampening;
	}
	public float getRestitution() {
		return restitution;
	}
	// What is this???
	public float getKinematic() {
		return kinematic;
	}
	public float getUnknown2() {
		return unknown2;
	}
	public float getFriction() {
		return friction;
	}
	public float getUnknown() {
		return unknown;
	}
	public float getDensity() {
		return density;
	}
	public float getGroup() {
		return group;
	}
	
	public boolean isPersistent() {
		return persistent;
	}
	public boolean hasPhysics() {
		return physics;
	}
	public boolean isVisible() {
		return visible;
	}
	public boolean isSensor() {
		return sensor;
	}
	public boolean isSolid() {
		return solid;
	}
	
	public int getDepth() {
		return depth;
	}
	public int getMask() {
		return mask;
	}
	
	@Override
	public String toString() {
		return String.format(
				"%s [name=%s, sprite=%s, parent=%s, visible=%s, solid=%s, persistent=%s]",
				this.getClass().getSimpleName(),
				name.getString(),
				sprite == null ? null : sprite.getName().getString(),
				this.getParent() == null ? null : parent.getName().getString(),
				visible,
				solid,
				persistent
		);
	}

}
