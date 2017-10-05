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
