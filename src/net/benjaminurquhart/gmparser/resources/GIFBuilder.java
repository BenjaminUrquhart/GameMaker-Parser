package net.benjaminurquhart.gmparser.resources;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

// Based off of https://github.com/ha-shine/Giffer
// and https://memorynotfound.com/generate-gif-image-java-delay-infinite-loop-example/
public class GIFBuilder {
	
	private BufferedImage[] background, frames;
	private SpriteResource sprite;
	private int scale, fps;
	
	public GIFBuilder() {
		this((SpriteResource) null);
	}
	public GIFBuilder(SpriteResource sprite) {
		this.sprite = sprite;
		this.scale = 1;
		this.fps = 10;
	}
	public GIFBuilder(BufferedImage... frames) {
		this.frames = frames;
		this.scale = 1;
		this.fps = 10;
	}
	public GIFBuilder setBackground(SpriteResource background) {
		return this.setBackground(background.getFrames());
	}
	public GIFBuilder setBackground(BufferedImage... frames) {
		this.checkDimensions(frames);
		this.background = frames;
		return this;
	}
	public GIFBuilder setFrames(BufferedImage... frames) {
		this.checkDimensions(frames);
		this.frames = frames;
		return this;
	}
	public GIFBuilder setFrames(SpriteResource sprite) {
		this.sprite = sprite;
		return this;
	}
	public GIFBuilder setScale(int scale) {
		if(scale < 1) {
			throw new IllegalArgumentException("scale " + scale + " < 1");
		}
		this.scale = scale;
		return this;
	}
	public GIFBuilder setFrameRate(int fps) {
		if(fps < 1) {
			throw new IllegalArgumentException("fps " + fps + " < 1");
		}
		if(fps > 100) {
			throw new IllegalArgumentException("fps " + fps + " > 100");
		}
		this.fps = fps;
		return this;
	}
	public byte[] build() {
		
		if((sprite == null || sprite.getFrames().length == 0) && (frames == null || frames.length == 0)) {
			throw new IllegalStateException("no frames provided");
		}
		
		TPAGResource[] tpags = sprite == null ? null : sprite.getTPAGInfo();
		BufferedImage[] frames = sprite == null ? this.frames : sprite.getFrames();
		
		Iterator<ImageWriter> itr = ImageIO.getImageWritersByFormatName("gif");
		if(!itr.hasNext()) {
			throw new IllegalStateException("no GIF writers found!");
		}
		int maxW = sprite == null ? frames[0].getWidth() : sprite.getWidth();
		int maxH = sprite == null ? frames[0].getHeight() : sprite.getHeight();
		
		int offsetX = 0;
		int offsetY = 0;
		
		if(background != null && background.length > 0) {
			BufferedImage bg = background[0];
			
			if(bg.getWidth() > maxW) {
				//System.out.println(bg.getWidth() + " > " + maxW);
				offsetX = bg.getWidth()-maxW;
				maxW = bg.getWidth();
			}
			if(bg.getHeight() > maxH) {
				offsetY = bg.getHeight()-maxH;
				maxH = bg.getHeight();
			}
		}
		maxW*=scale;
		maxH*=scale;
		
		offsetX*=scale;
		offsetY*=scale;
		
		BufferedImage canvas = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics;
		
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		
		try(ImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
			
			ImageWriter writer = itr.next();
			ImageWriteParam param = writer.getDefaultWriteParam();
			ImageTypeSpecifier specifier = ImageTypeSpecifier.createFromRenderedImage(canvas);
			
			IIOMetadata meta = writer.getDefaultImageMetadata(specifier, param);
			
	        initMeta(meta);
	        
	        writer.setOutput(output);
	        writer.prepareWriteSequence(null);
	        
	        int x, y;
	        TPAGResource tpag;
	        BufferedImage frame, bg = null;
	        for(int i = 0, len = Math.max(frames.length, background == null ? 0 : background.length); i < len; i++) {
	        	//System.out.println("Writing frame " + (i+1) + "/" + len);
	        	frame = frames[i%frames.length];
	        	
	        	if(frame == null) {
	        		throw new IllegalStateException("encountered null frame at index " + i);
	        	}
	        	
	        	if(background != null && background.length > 0) {
	        		bg = background[i%background.length];
	        	}
	        	
	        	if(tpags != null && tpags.length > 0) {
	        		tpag = tpags[i%tpags.length];
		        	x = tpag.getRenderX()*scale+offsetX;
		        	y = tpag.getRenderY()*scale+offsetY;
	        	}
	        	else {
	        		x = offsetX;
	        		y = offsetY;
	        	}
	        	
	        	canvas = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
	        	graphics = canvas.createGraphics();
	        	
	        	if(scale > 1) {
	        		if(bg != null) {
	        			graphics.drawImage(bg.getScaledInstance(
	        					bg.getWidth()*scale,
	        					bg.getHeight()*scale,
	        					BufferedImage.SCALE_AREA_AVERAGING
	        			), 0, 0, null);
	        		}
	        		graphics.drawImage(frame.getScaledInstance(
	        			frame.getWidth()*scale, 
	        			frame.getHeight()*scale, 
	        			BufferedImage.SCALE_AREA_AVERAGING
	        		), x, y, null);
	        	}
	        	else {
	        		graphics.drawImage(bg, 0, 0, null);
	        		graphics.drawImage(frame, x, y, null);
	        	}
	        	
	        	graphics.dispose();
	        	
	        	writer.writeToSequence(new IIOImage(canvas, null, meta), param);
	        	output.flush();
	        }
	        writer.endWriteSequence();
	        writer.dispose();
	        
	        return bytes.toByteArray();
		}
		catch(RuntimeException e) {
			throw e;
		}
		catch(Throwable e) {
			throw new RuntimeException(e);
		}
	}
	private void checkDimensions(BufferedImage... frames) {
		int dimX = -1, dimY = -1;
		boolean set = false;
		
		for(BufferedImage frame : frames) {
			if(frame == null) {
				throw new IllegalArgumentException("background frame cannot be null");
			}
			if(set) {
				if(frame.getWidth() != dimX || frame.getHeight() != dimY) {
					throw new IllegalArgumentException("all background frames must have the same dimensions");
				}
			}
			else {
				dimX = frame.getWidth();
				dimY = frame.getHeight();
				set = true;
			}
		}
	}
	private void initMeta(IIOMetadata meta) throws IOException {
        String metaFormatName = meta.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(metaFormatName);

        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
        graphicsControlExtensionNode.setAttribute("disposalMethod", "restoreToBackgroundColor");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "TRUE");
        graphicsControlExtensionNode.setAttribute("delayTime", String.valueOf(100/this.fps));
        //graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");
        IIOMetadataNode child = getNode(appExtensionsNode, "ApplicationExtension");
        child.setAttribute("applicationID", "NETSCAPE");
        child.setAttribute("authenticationCode", "2.0");
        
        child.setUserObject(new byte[]{1,0,0});
        appExtensionsNode.appendChild(child);
        root.appendChild(appExtensionsNode);
        
        meta.setFromTree(metaFormatName, root);
	}
    private IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int num = rootNode.getLength();
        for(int i = 0; i < num; i++){
            if(rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return node;
    }
}
