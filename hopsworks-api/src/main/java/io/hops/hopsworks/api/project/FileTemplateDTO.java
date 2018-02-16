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

package io.hops.hopsworks.api.project;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FileTemplateDTO {

  private String inodePath;
  private int templateId;

  public FileTemplateDTO() {
  }

  public FileTemplateDTO(String inodePath, int templateId) {
    this.inodePath = inodePath;
    this.templateId = templateId;
  }

  public void setInodePath(String inodePath) {
    this.inodePath = inodePath;
  }

  public void setTemplateId(int templateId) {
    this.templateId = templateId;
  }

  public String getInodePath() {
    return this.inodePath;
  }

  public int getTemplateId() {
    return this.templateId;
  }

  @Override
  public String toString() {
    return "FileTemplateDTO{" + "inodePath=" + this.inodePath + ", templateId="
            + this.templateId + "}";
  }
}
