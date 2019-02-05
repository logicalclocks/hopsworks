/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
