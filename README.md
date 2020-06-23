# GameMaker-Parser
Library to parse GameMaker archives

# What can be parsed so far
- Strings
- Audio (mostly complete)
- Textures/Spritesheets
- Sprites (no collision masks)

# Usage
```java
/* 
 * This snippet prints all the parsed assets
 */
GMDataFile data = new GMDataFile(new File("path/to/archive"));

System.out.println(data.getObject(0x004400)); // Prints whatever object happens to be at that address, if any.

for(Resource resource : data.getAudio()) {
    System.out.println(resource);
}

for(Resource resource : data.getTextures()) {
    System.out.println(resource);
}

for(Resource resource : data.getSprites()) {
    System.out.println(resource);
}

for(Resource resource : data.getStrings()) {
    System.out.println(resource);
}
```

# Todo
- Code (will have to make a decompiler, ew)
- Rooms
- Backgrounds
- Everything else
