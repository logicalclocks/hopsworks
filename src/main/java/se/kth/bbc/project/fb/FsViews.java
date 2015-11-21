package se.kth.bbc.project.fb;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public final class FsViews {

  private List<FsView> fsViews;

  public FsViews(List<FsView> fsViews) {
    this.fsViews = fsViews;
  }

  public FsViews() {
  }

  public List<FsView> getFsViews() {
    return fsViews;
  }

  public void setFsViews(List<FsView> fsViews) {
    this.fsViews = fsViews;
  }
  
}
