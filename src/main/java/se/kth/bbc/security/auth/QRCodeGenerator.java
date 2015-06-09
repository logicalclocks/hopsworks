package se.kth.bbc.security.auth;

import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.imageio.ImageIO;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
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
   * <p>
   * @param qrURL
   * @return
   */
  public static QRCodeGenerator qrCodeURLFormat(String qrURL) {
    return new QRCodeGenerator(qrURL);
  }

  /**
   * Generate the QRCode stream to be shown to user.
   * <p>
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
  public static StreamedContent getQRCode(String user, String host,
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

    return new DefaultStreamedContent(new ByteArrayInputStream(stream.
            toByteArray()), "image/png");

  }

}
