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

package io.hops.hopsworks.common.dao.dataset;

import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.project.Project;
import org.apache.hadoop.fs.permission.FsPermission;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Collection;


@Entity
@Table(name = "dataset",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Dataset.findAll",
      query = "SELECT d FROM Dataset d")
  ,
    @NamedQuery(name = "Dataset.findById",
      query = "SELECT d FROM Dataset d WHERE d.id = :id")
  ,
    @NamedQuery(name = "Dataset.findByInode",
      query = "SELECT d FROM Dataset d WHERE d.inode = :inode")
  ,
    @NamedQuery(name = "Dataset.findByProject",
      query = "SELECT d FROM Dataset d WHERE d.project = :project")
  ,
    @NamedQuery(name = "Dataset.findByProjectAndInode",
      query = "SELECT d FROM Dataset d WHERE d.project = :project AND d.inode = :inode")
  ,
    @NamedQuery(name = "Dataset.findBySearchable",
      query = "SELECT d FROM Dataset d WHERE d.searchable = :searchable")
  ,
    @NamedQuery(name = "Dataset.findByPublicDs",
      query = "SELECT d FROM Dataset d WHERE d.publicDs = :publicDs")
  ,
    @NamedQuery(name = "Dataset.findByPublicDsId",
      query = "SELECT d FROM Dataset d WHERE d.publicDsId = :publicDsId")
  ,
    @NamedQuery(name = "Dataset.findByPublicDsIdProject",
      query = "SELECT d FROM Dataset d WHERE d.project = :project AND d.publicDsId = :publicDsId")
  ,
    @NamedQuery(name = "Dataset.findAllPublic",
      query = "SELECT d FROM Dataset d WHERE d.publicDs in (1,2)")
  ,
    @NamedQuery(name = "Dataset.findPublicByState",
      query = "SELECT d FROM Dataset d WHERE d.publicDs = :publicDs")
  ,
    @NamedQuery(name = "Dataset.findByDstype",
      query = "SELECT d FROM Dataset d WHERE d.dsType = :dstype")})
public class Dataset implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumns({
    @JoinColumn(name = "inode_pid",
      referencedColumnName = "parent_id"),
    @JoinColumn(name = "inode_name",
      referencedColumnName = "name"),
    @JoinColumn(name = "partition_id",
      referencedColumnName = "partition_id")})
  @ManyToOne(optional = false)
  private Inode inode;
  @Basic(optional = false)
  @Column(name = "inode_id")
  private Long inodeId;
  @Basic(optional = false)
  @Column(name = "inode_name",
    updatable = false,
    insertable = false)
  private String name;
  @Size(max = 2000)
  @Column(name = "description")
  private String description;
  @Basic(optional = false)
  @NotNull
  @Column(name = "searchable")
  private boolean searchable;
  @Basic(optional = false)
  @NotNull
  @Column(name = "public_ds")
  private int publicDs;
  @Size(max = 1000)
  @Column(name = "public_ds_id")
  private String publicDsId;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "dstype")
  private DatasetType dsType = DatasetType.DATASET;
  @OneToMany(cascade = CascadeType.ALL,
    mappedBy = "dataset")
  private Collection<DatasetRequest> datasetRequestCollection;
  @OneToMany(cascade = CascadeType.ALL,
    mappedBy = "dataset")
  private Collection<DatasetSharedWith> datasetSharedWithCollection;
  @JoinColumn(name = "feature_store_id",
      referencedColumnName = "id")
  @ManyToOne
  private Featurestore featureStore;
  @JoinColumn(name = "projectId",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;

  public Dataset() {
  }

  public Dataset(Integer id) {
    this.id = id;
  }
  
  public Dataset(Integer id, Inode inode) {
    this.id = id;
    this.inode = inode;
    this.name = inode.getInodePK().getName();
    this.inodeId = inode.getId();
  }
  
  public Dataset(Inode inode, Project project) {
    this.inode = inode;
    this.project = project;
    this.name = inode.getInodePK().getName();
    this.inodeId = inode.getId();
  }
  
  public Dataset(Dataset ds, Project project){
    this.inode = ds.getInode();
    this.project = project;
    this.name = ds.getInode().getInodePK().getName();
    this.inodeId = ds.getInode().getId();
    this.searchable = ds.isSearchable();
    this.description = ds.getDescription();
    this.publicDs = ds.getPublicDs();
    this.dsType = ds.getDsType();
    this.featureStore = ds.getFeatureStore();
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
  
  public Inode getInode() {
    return inode;
  }
  
  public void setInode(Inode inode) {
    this.inode = inode;
    this.inodeId = inode.getId();
    this.name = inode.getInodePK().getName();
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public boolean isSearchable() {
    return searchable;
  }
  
  public Project getProject() {
    return project;
  }
  
  public void setProject(Project project) {
    this.project = project;
  }
  
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean getSearchable() {
    return searchable;
  }

  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }

  public int getPublicDs() {
    return publicDs;
  }

  public void setPublicDs(int publicDs) {
    this.publicDs = publicDs;
  }

  public String getPublicDsId() {
    return publicDsId;
  }

  public void setPublicDsId(String publicDsId) {
    this.publicDsId = publicDsId;
  }

  public DatasetType getDsType() {
    return dsType;
  }

  public void setDsType(DatasetType dsType) {
    this.dsType = dsType;
  }

  public Featurestore getFeatureStore() {
    return featureStore;
  }

  public void setFeatureStore(Featurestore featureStoreId) {
    this.featureStore = featureStoreId;
  }
  
  @XmlTransient
  @JsonIgnore
  public Collection<DatasetRequest> getDatasetRequestCollection() {
    return datasetRequestCollection;
  }
  
  public void setDatasetRequestCollection(Collection<DatasetRequest> datasetRequestCollection) {
    this.datasetRequestCollection = datasetRequestCollection;
  }
  
  @XmlTransient
  @JsonIgnore
  public Collection<DatasetSharedWith> getDatasetSharedWithCollection() {
    return datasetSharedWithCollection;
  }
  
  public void setDatasetSharedWithCollection(Collection<DatasetSharedWith> datasetSharedWithCollection) {
    this.datasetSharedWithCollection = datasetSharedWithCollection;
  }
  
  public void setPublicDsState(SharedState sharedState) {
    this.publicDs = sharedState.state;
  }
  
  public boolean isPublicDs() {
    if(publicDs == 0 ) {
      return false;
    }
    return true;
  }
  
  public boolean isShared(Project project) {
    return !this.project.equals(project);
  }
  
  public Long getInodeId() {
    return this.getInode().getId();
  }
  
  public DatasetPermissions getPermissions() {
    FsPermission fsPermission = new FsPermission(this.inode.getPermission());
    return DatasetPermissions.fromFilePermissions(fsPermission);
  }
  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Dataset)) {
      return false;
    }
    Dataset other = (Dataset) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Dataset[ id=" + id + " ]";
  }
  
}
