/*
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
 *
 */

package io.hops.hopsworks.common.dataset;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Provides information on the previewed file in Datasets.
 * <p>
 */
@XmlRootElement
public class FilePreviewDTO {

  private String type;
  private String content;
  private String extension;

  public FilePreviewDTO() {
  }

  public FilePreviewDTO(String type, String extension, String content) {
    this.type = type;
    this.extension = extension;
    this.content = content;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  @Override
  /**
   * Formats a JSON to be displayed by the browser.
   */
  public String toString() {
    return "{\"filePreviewDTO\":[{\"type\":\"" + type
            + "\", \"extension\":\"" + extension
            + "\", \"content\":\"" + content.replace("\\", "\\\\'").
            replace("\"", "\\\"").replace("\n", "\\n").
            replace("\r", "\\r").replace("\t", "\\t")
            + "\"}]}";
  }

}
