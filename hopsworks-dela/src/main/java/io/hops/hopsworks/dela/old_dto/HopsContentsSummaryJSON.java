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

package io.hops.hopsworks.dela.old_dto;

import java.util.Map;
import java.util.TreeMap;
import javax.xml.bind.annotation.XmlRootElement;

public class HopsContentsSummaryJSON {

  @XmlRootElement
  public static class JsonWrapper {

    //<projectId, torrentSummaryList>
    private ContentsElement[] contents = new ContentsElement[0];

    public JsonWrapper() {
    }

    public ContentsElement[] getContents() {
      return contents;
    }

    public void setContents(ContentsElement[] contents) {
      if(contents == null) {
        this.contents = new ContentsElement[0];
        return;
      }
      this.contents = contents;
    }

    public Contents resolve() {
      Map<Integer, ElementSummaryJSON[]> c = new TreeMap<>();
      for (ContentsElement ce : contents) {
        c.put(ce.projectId, ce.projectContents);
      }
      return new Contents(c);
    }
  }

  @XmlRootElement
  public static class ContentsElement {

    public Integer projectId;
    public ElementSummaryJSON[] projectContents;

    private ContentsElement() {
    }

    public ContentsElement(Integer projectId, ElementSummaryJSON[] projectContents) {
      this.projectId = projectId;
      this.projectContents = projectContents;
    }
  }

  @XmlRootElement
  public static class Contents {

    private Map<Integer, ElementSummaryJSON[]> contents;

    public Contents(Map<Integer, ElementSummaryJSON[]> contents) {
      this.contents = contents;
    }
    
    public Contents() {
    }

    public Map<Integer, ElementSummaryJSON[]> getContents() {
      return contents;
    }

    public void setContents(Map<Integer, ElementSummaryJSON[]> contents) {
      this.contents = contents;
    }
  }
}
