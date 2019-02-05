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

package io.hops.hopsworks.common.util;

import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.imageio.ImageIO;

public class QRCodeGenerator {

  private final int width = 200;
  private final int height = 200;

  private final String qrURL;
  private final Writer qrw;

  private QRCodeGenerator(String qrURL) {
    this.qrURL = qrURL;
    qrw = new QRCodeWriter();
  }

  /**
   * Generate the QRCode URL.
   * <p/>
   * @param qrURL
   * @return
   */
  public static QRCodeGenerator qrCodeURLFormat(String qrURL) {
    return new QRCodeGenerator(qrURL);
  }

  /**
   * Generate the QRCode stream to be shown to user.
   * <p/>
   * @return @throws IOException
   * @throws WriterException
   */
  public ByteArrayOutputStream qrcodeStream() throws IOException,
          WriterException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    writeToStream(stream);
    return stream;
  }

  private BitMatrix generateMatrix() throws WriterException {
    return qrw.encode(qrURL, com.google.zxing.BarcodeFormat.QR_CODE, width,
            height);
  }

  private void writeToStream(OutputStream stream) throws IOException,
          WriterException {
    MatrixToImageWriter.writeToStream(generateMatrix(), "PNG", stream);
  }

  /**
   * Generates the QRcode image to be scanned by the user.
   *
   * @param user
   * @param host
   * @param secret
   * @return
   * @throws UnsupportedEncodingException
   * @throws IOException
   * @throws com.google.zxing.WriterException
   */
  public static ByteArrayInputStream getQRCode(String user, String host,
          String secret) throws UnsupportedEncodingException, IOException,
          WriterException {

    // Format the qr code
    String chl = "otpauth://totp/" + user + "?secret=" + secret + "&issuer="
            + host;

    // Build a stream content to be loaded by user mobile    
    ByteArrayOutputStream stream = QRCodeGenerator.qrCodeURLFormat(chl).
            qrcodeStream();
    BufferedImage bufferedImg = new BufferedImage(100, 25,
            BufferedImage.TYPE_INT_RGB);
    // Build an image to be sent to user
    Graphics2D g2 = bufferedImg.createGraphics();
    ImageIO.write(bufferedImg, "png", stream);

    return new ByteArrayInputStream(stream.toByteArray());
//    return new DefaultStreamedContent(new ByteArrayInputStream(stream.toByteArray()), "image/png");
  }

  /**
   * Generates the QRcode image to be scanned by the user.
   *
   * @param user
   * @param host
   * @param secret
   * @return
   * @throws UnsupportedEncodingException
   * @throws IOException
   * @throws com.google.zxing.WriterException
   */
  public static byte[] getQRCodeBytes(String user, String host,
          String secret) throws UnsupportedEncodingException, IOException,
          WriterException {

    // Format the qr code
    String chl = "otpauth://totp/" + user + "?secret=" + secret + "&issuer="
            + host;

    // Build a stream content to be loaded by user mobile    
    ByteArrayOutputStream stream = QRCodeGenerator.qrCodeURLFormat(chl).
            qrcodeStream();
    BufferedImage bufferedImg = new BufferedImage(100, 25,
            BufferedImage.TYPE_INT_RGB);
    // Build an image to be sent to user
    Graphics2D g2 = bufferedImg.createGraphics();
    ImageIO.write(bufferedImg, "png", stream);

    return stream.toByteArray();

  }

}
