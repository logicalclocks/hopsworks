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
