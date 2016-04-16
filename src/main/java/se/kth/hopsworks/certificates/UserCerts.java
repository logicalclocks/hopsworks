package se.kth.hopsworks.certificates;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "user_certs", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "UserCerts.findAll", query = "SELECT u FROM UserCerts u"),
    @NamedQuery(name = "UserCerts.findByProjectId", query = "SELECT u FROM UserCerts u WHERE u.userCertsPK.projectId = :projectId"),
    @NamedQuery(name = "UserCerts.findByUserId", query = "SELECT u FROM UserCerts u WHERE u.userCertsPK.userId = :userId"),
    @NamedQuery(name = "UserCerts.findUserProjectCert", query = "SELECT u FROM UserCerts u WHERE u.userCertsPK.projectId = :projectId AND u.userCertsPK.userId = :userId")})
public class UserCerts implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected UserCertsPK userCertsPK;
    @Lob
    @Column(name = "user_key")
    private byte[] userKey;
    @Lob
    @Column(name = "user_cert")
    private byte[] userCert;
    @JoinColumn(name = "user_id", referencedColumnName = "uid", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Users users;
    @JoinColumn(name = "project_id", referencedColumnName = "id", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Project project;

    public UserCerts() {
    }

    public UserCerts(UserCertsPK userCertsPK) {
        this.userCertsPK = userCertsPK;
    }

    public UserCerts(int projectId, int userId) {
        this.userCertsPK = new UserCertsPK(projectId, userId);
    }

    public UserCertsPK getUserCertsPK() {
        return userCertsPK;
    }

    public void setUserCertsPK(UserCertsPK userCertsPK) {
        this.userCertsPK = userCertsPK;
    }

    public byte[] getUserKey() {
        return userKey;
    }

    public void setUserKey(byte[] userKey) {
        this.userKey = userKey;
    }

    public byte[] getUserCert() {
        return userCert;
    }

    public void setUserCert(byte[] userCert) {
        this.userCert = userCert;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (userCertsPK != null ? userCertsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UserCerts)) {
            return false;
        }
        UserCerts other = (UserCerts) object;
        if ((this.userCertsPK == null && other.userCertsPK != null) || (this.userCertsPK != null && !this.userCertsPK.equals(other.userCertsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.hopsworks.certificates.UserCerts[ userCertsPK=" + userCertsPK + " ]";
    }
    
}
