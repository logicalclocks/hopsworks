/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.activity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.fb.Inode;
import se.kth.hopsworks.hdfsUsers.model.HdfsUsers;

/**
 *
 * @author jdowling
 */
@Entity
@Table(name = "file_activity", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "FileActivity.findAll", query = "SELECT f FROM FileActivity f"),
    @NamedQuery(name = "FileActivity.findById", query = "SELECT f FROM FileActivity f WHERE f.id = :id"),
    @NamedQuery(name = "FileActivity.findByActivity", query = "SELECT f FROM FileActivity f WHERE f.activity = :activity"),
    @NamedQuery(name = "FileActivity.findByCreated", query = "SELECT f FROM FileActivity f WHERE f.created = :created"),
    @NamedQuery(name = "FileActivity.findByParentId", query = "SELECT f FROM FileActivity f WHERE f.parentId = :parentId"),
    @NamedQuery(name = "FileActivity.findByName", query = "SELECT f FROM FileActivity f WHERE f.name = :name")})
public class FileActivity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "activity")
    private String activity;
    @Basic(optional = false)
    @NotNull
    @Column(name = "created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @JoinColumns({
        @JoinColumn(name = "parent_id",
            referencedColumnName = "parent_id"),
        @JoinColumn(name = "name",
            referencedColumnName = "name")
    })
    @OneToOne(optional = false)
    private Inode inode;

    @JoinColumn(name = "hdfs_user_id",
        referencedColumnName = "id")
    @OneToOne(optional = false)
    private HdfsUsers user;

    public FileActivity() {
    }

    public FileActivity(Integer id) {
        this.id = id;
    }

    public FileActivity(Integer id, String activity, HdfsUsers user, Date created, Inode inode) {
        this.id = id;
        this.activity = activity;
        this.user = user;
        this.created = created;
        this.inode = inode;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public void setInode(Inode inode) {
        this.inode = inode;
    }

    public void setUser(HdfsUsers user) {
        this.user = user;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    public Inode getInode() {
        return inode;
    }

    public HdfsUsers getUser() {
        return user;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof FileActivity)) {
            return false;
        }
        FileActivity other = (FileActivity) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.activity.FileActivity[ id=" + id + " ]";
    }

}
