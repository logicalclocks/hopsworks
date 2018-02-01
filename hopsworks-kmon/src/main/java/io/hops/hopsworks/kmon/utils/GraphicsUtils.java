/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.kmon.utils;

import java.awt.Color;
import java.awt.Font;
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

public class GraphicsUtils {

  private static final String IMAGE_TYPE = "png";

  public static byte[] errorImage(String msg) throws IOException {
    int w = 350;
    int h = 200;
    BufferedImage image = createImageWithText(w, h, msg);
    return convertBufferedImageToByteArray(image);
  }

  public static byte[] convertImageInputStreamToByteArray(
          InputStream imageInputStream) throws IOException {
    BufferedImage bufferedImage = ImageIO.read(imageInputStream);
    return convertBufferedImageToByteArray(bufferedImage);
  }

  private static byte[] convertBufferedImageToByteArray(BufferedImage image)
          throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ImageIO.write(image, IMAGE_TYPE, byteArrayOutputStream);
    return byteArrayOutputStream.toByteArray();
  }

  private static BufferedImage createImageWithText(int w, int h, String text) {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    int fontHeight = g.getFontMetrics().getHeight();
    g.setPaint(new Color(240, 240, 240)); // Light Gray
    g.fillRect(0, 0, w, h);
    g.setPaint(Color.GRAY);
    g.draw3DRect(0, 0, w - 1, h - 1, true);
    g.setPaint(Color.RED);
    g.drawString("Error", fontHeight, fontHeight * 2);
    g.setPaint(Color.BLACK);
    g.setFont(new Font("Arial", Font.PLAIN, 12));
    int x = fontHeight;
    int y = fontHeight * 3;
    GraphicsUtils.drawTextInBoundedArea(g, x, y, w - x, h, text);
    g.dispose();
    return img;
  }

  private static void drawTextInBoundedArea(Graphics2D g2d, int x1, int y1,
          int x2, int y2, String text) {
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
      y1 += tl.getDescent() + tl.getLeading() + (interline - 1.0f) * tl.
              getAscent();
      if (y1 > y2) {
        break;
      }
    }
  }
}
