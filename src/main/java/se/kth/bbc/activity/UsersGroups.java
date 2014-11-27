/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.activity;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.kthfsdashboard.user.Username;

/**
 *
 * @author jdowling
 */
@Entity
@Table(name = "USERS_GROUPS")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "UsersGroups.findAll", query = "SELECT u FROM UsersGroups u"),
    @NamedQuery(name = "UsersGroups.findByEmail", query = "SELECT u FROM UsersGroups u WHERE u.usersGroupsPK.email = :email"),
    @NamedQuery(name = "UsersGroups.findByGroupname", query = "SELECT u FROM UsersGroups u WHERE u.usersGroupsPK.groupname = :groupname"),
    @NamedQuery(name = "UsersGroups.deleteGroupsForEmail", query = "DELETE FROM UsersGroups u WHERE u.usersGroupsPK.email = :email AND u.usersGroupsPK.groupname = 'GUEST'"),
    @NamedQuery(name = "UsersGroups.findARecord", query = "SELECT COUNT(u.usersGroupsPK.email) FROM UsersGroups u WHERE u.usersGroupsPK.email = :email")})
public class UsersGroups implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected UsersGroupsPK usersGroupsPK;
    @JoinColumn(name = "email", referencedColumnName = "EMAIL", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Username username;

    public UsersGroups() {
    }

    public UsersGroups(UsersGroupsPK usersGroupsPK) {
        this.usersGroupsPK = usersGroupsPK;
    }

    public UsersGroups(String email, String groupname) {
        this.usersGroupsPK = new UsersGroupsPK(email, groupname);
    }

    public UsersGroupsPK getUsersGroupsPK() {
        return usersGroupsPK;
    }

    public void setUsersGroupsPK(UsersGroupsPK usersGroupsPK) {
        this.usersGroupsPK = usersGroupsPK;
    }

    public Username getUsername() {
        return username;
    }

    public void setUsername(Username username) {
        this.username = username;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (usersGroupsPK != null ? usersGroupsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UsersGroups)) {
            return false;
        }
        UsersGroups other = (UsersGroups) object;
        return !((this.usersGroupsPK == null && other.usersGroupsPK != null) || (this.usersGroupsPK != null && !this.usersGroupsPK.equals(other.usersGroupsPK)));
    }

    @Override
    public String toString() {
        return "se.kth.bbc.activity.UsersGroups[ usersGroupsPK=" + usersGroupsPK + " ]";
    }
    
}