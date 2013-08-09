package se.kth.kthfsdashboard.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import javax.imageio.ImageIO;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class GraphicsUtils {

    private static final String IMAGE_TYPE = "png";

    public static byte[] errorImage(String msg) throws IOException {
        int w = 350;
        int h = 200;
        BufferedImage image = createImageWithText(w, h, msg);
        return convertBufferedImageToByteArray(image);
    }
    
    public static byte[] convertImageInputStreamToByteArray(InputStream imageInputStream) throws IOException { 
        BufferedImage bufferedImage = ImageIO.read(imageInputStream);
        return convertBufferedImageToByteArray(bufferedImage);        
    }

    private static byte[] convertBufferedImageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, IMAGE_TYPE, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private static BufferedImage createImageWithText(int w, int h, String text) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setPaint(Color.black);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int x = 5;
        int y = fm.getHeight();
        GraphicsUtils.drawText(g2d, x, y, w, h, text);
        g2d.dispose();
        return img;
    }

    private static void drawText(Graphics2D g2d, int x1, int y1, int x2, int y2, String text) {
        float interline = 1;
        float width = x2 - x1;
        AttributedString as = new AttributedString(text);
        as.addAttribute(TextAttribute.FOREGROUND, g2d.getPaint());
        as.addAttribute(TextAttribute.FONT, g2d.getFont());
        AttributedCharacterIterator aci = as.getIterator();
        FontRenderContext frc = new FontRenderContext(null, true, false);
        LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
        while (lbm.getPosition() < text.length()) {
            TextLayout tl = lbm.nextLayout(width);
            y1 += tl.getAscent();
            tl.draw(g2d, x1, y1);
            y1 += tl.getDescent() + tl.getLeading() + (interline - 1.0f) * tl.getAscent();
            if (y1 > y2) {
                break;
            }
        }
    }
}
