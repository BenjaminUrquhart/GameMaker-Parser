package net.benjaminurquhart.gmparser.resources;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
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
class GIFCreator {

	protected static byte[] create(SpriteResource sprite) {
		return create(1, sprite.getFrames());
	}
	protected static byte[] create(SpriteResource sprite, int scale) {
		return create(scale, sprite.getFrames());
	}
	protected static byte[] create(BufferedImage... frames) {
		return create(1, frames);
	}
	protected static byte[] create(int scale, BufferedImage... frames) {
		if(scale < 1) {
			scale = 1;
		}
		try {
			Iterator<ImageWriter> itr = ImageIO.getImageWritersByFormatName("gif");
			if(!itr.hasNext()) {
				throw new IllegalStateException("no GIF writers found!");
			}
			int maxW = Arrays.stream(frames).mapToInt(BufferedImage::getWidth).max().orElse(0)*scale;
			int maxH = Arrays.stream(frames).mapToInt(BufferedImage::getHeight).max().orElse(0)*scale;
			
			BufferedImage canvas = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics;
			
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			
			ImageWriter writer = itr.next();
			ImageWriteParam param = writer.getDefaultWriteParam();
			ImageOutputStream output = new MemoryCacheImageOutputStream(bytes);
			ImageTypeSpecifier specifier = ImageTypeSpecifier.createFromRenderedImage(canvas);
			IIOMetadata meta = writer.getDefaultImageMetadata(specifier, param);
			
	        initMeta(meta);
	        
	        writer.setOutput(output);
	        writer.prepareWriteSequence(null);
	        
	        int x, y;
	        for(BufferedImage frame : frames) {
	        	x = maxW-frame.getWidth()*scale;
	        	y = maxH-frame.getHeight()*scale;
	        	
	        	canvas = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
	        	graphics = canvas.createGraphics();
	        	
	        	if(scale > 1) {
	        		graphics.drawImage(frame.getScaledInstance(
	        			frame.getWidth()*scale, 
	        			frame.getHeight()*scale, 
	        			BufferedImage.SCALE_AREA_AVERAGING
	        		), x, y, null);
	        	}
	        	else {
	        		graphics.drawImage(frame, x, y, null);
	        	}
	        	
	        	graphics.dispose();
	        	
	        	writer.writeToSequence(new IIOImage(canvas, null, meta), param);
	        	output.flush();
	        }
	        writer.endWriteSequence();
	        writer.dispose();
	        output.close();
	        
	        return bytes.toByteArray();
		}
		catch(RuntimeException e) {
			throw e;
		}
		catch(Throwable e) {
			throw new RuntimeException(e);
		}
	}
	private static void initMeta(IIOMetadata meta) throws IOException {
        String metaFormatName = meta.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(metaFormatName);

        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
        graphicsControlExtensionNode.setAttribute("disposalMethod", "restoreToBackgroundColor");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "TRUE");
        graphicsControlExtensionNode.setAttribute("delayTime", "10");
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
    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
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
