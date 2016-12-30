package io.hops.hopsworks.common.project;

import java.io.File;
import java.util.Date;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.util.Settings;

@XmlRootElement
public class MoreInfoDTO {

  private Integer inodeid;
  private String user;
  private double size;
  private Date createDate;
  private Date uploadDate;
  private int votes;
  private long downloads;
  private String path;

  public MoreInfoDTO() {
  }

  public MoreInfoDTO(Project proj) {
    this.inodeid = proj.getInode().getId();
    this.user = proj.getOwner().getFname() + " " + proj.getOwner().getFname();
    this.size = 0;
    this.createDate = proj.getCreated();
    this.uploadDate = null;
    this.path = File.separator + Settings.DIR_ROOT + File.separator
            + proj.getName();
  }

  public MoreInfoDTO(Inode inode) {
    this.inodeid = inode.getId();
    this.user = inode.getHdfsUser().getUsername();
    this.size = inode.getSize();
    this.createDate = new Date(inode.getModificationTime().longValue());
    this.uploadDate = null;
  }

  public MoreInfoDTO(Dataset ds, String user) {
    this.inodeid = ds.getInode().getId();
    this.user = user;
    this.size = ds.getInode().getSize();
    this.createDate = new Date(ds.getInode().getModificationTime().longValue());
    this.uploadDate = null;
  }

  public MoreInfoDTO(Integer inodeid, String user, double size, Date createDate,
          Date uploadDate, int votes, long downloads) {
    this.inodeid = inodeid;
    this.user = user;
    this.size = size;
    this.createDate = createDate;
    this.uploadDate = uploadDate;
    this.votes = votes;
    this.downloads = downloads;
  }

  public String getUser() {
    return user;
  }

  public Integer getInodeid() {
    return inodeid;
  }

  public void setInodeid(Integer inodeid) {
    this.inodeid = inodeid;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public double getSize() {
    return size;
  }

  public void setSize(double size) {
    this.size = size;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  public Date getUploadDate() {
    return uploadDate;
  }

  public void setUploadDate(Date uploadDate) {
    this.uploadDate = uploadDate;
  }

  public int getVotes() {
    return votes;
  }

  public void setVotes(int votes) {
    this.votes = votes;
  }

  public long getDownloads() {
    return downloads;
  }

  public void setDownloads(long downloads) {
    this.downloads = downloads;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 79 * hash + Objects.hashCode(this.inodeid);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final MoreInfoDTO other = (MoreInfoDTO) obj;
    if (!Objects.equals(this.inodeid, other.inodeid)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "MoreInfoDTO{" + "inodeid=" + inodeid + ", user=" + user + ", size="
            + size + ", createDate=" + createDate + ", uploadDate=" + uploadDate
            + ", votes=" + votes + ", downloads=" + downloads + '}';
  }
}
