/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "StudyTeam")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "TeamMembers.findAll", query = "SELECT t FROM TeamMembers t"),
    @NamedQuery(name = "TeamMembers.findByName", query = "SELECT t FROM TeamMembers t WHERE t.name = :name"),
    @NamedQuery(name = "TeamMembers.findByTeamMember", query = "SELECT t FROM TeamMembers t WHERE t.teamMember = :teamMember"),
    @NamedQuery(name = "TeamMembers.findByTeamRole", query = "SELECT t FROM TeamMembers t WHERE t.teamRole = :teamRole"),
    @NamedQuery(name = "TeamMembers.countMastersByStudy", query = "SELECT COUNT(DISTINCT t.teamMember) FROM TeamMembers t WHERE t.name=:name AND t.teamRole = :teamRole"),
    @NamedQuery(name = "TeamMembers.countAllMembers", query = "SELECT t.teamMember FROM TeamMembers t WHERE t.name = :name"),
    @NamedQuery(name = "TeamMembers.findMembersByRole", query = "SELECT t FROM TeamMembers t WHERE t.name=:name AND t.teamRole = :teamRole"),
    @NamedQuery(name = "TeamMembers.findMembersByName", query = "SELECT t FROM TeamMembers t WHERE t.name=:name")})
public class TeamMembers implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name")
    private String name;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "team_member")
    private String teamMember;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 10)
    @Column(name = "team_role")
    private String teamRole;

    public TeamMembers() {
    }

    public TeamMembers(String name) {
        this.name = name;
    }

    public TeamMembers(String name, String teamMember, String teamRole) {
        this.name = name;
        this.teamMember = teamMember;
        this.teamRole = teamRole;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeamMember() {
        return teamMember;
    }

    public void setTeamMember(String teamMember) {
        this.teamMember = teamMember;
    }

    public String getTeamRole() {
        return teamRole;
    }

    public void setTeamRole(String teamRole) {
        this.teamRole = teamRole;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (name != null ? name.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TeamMembers)) {
            return false;
        }
        TeamMembers other = (TeamMembers) object;
        if ((this.name == null && other.name != null) || (this.name != null && !this.name.equals(other.name))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.study.TeamMembers[ name=" + name + " ]";
    }
  

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
}
