/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;


@Entity
@Table(name = "yarn_app_heuristic_result", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "YarnAppHeuristicResult.findAll", query = "SELECT y FROM YarnAppHeuristicResult y"),
    @NamedQuery(name = "YarnAppHeuristicResult.findById", query = "SELECT y FROM YarnAppHeuristicResult y WHERE y.id = :id"),
    @NamedQuery(name = "YarnAppHeuristicResult.findByYarnAppResultId", query = "SELECT y FROM YarnAppHeuristicResult y WHERE y.yarnAppResultId = :yarnAppResultId"),
    @NamedQuery(name = "YarnAppHeuristicResult.findByHeuristicName", query = "SELECT y FROM YarnAppHeuristicResult y WHERE y.heuristicName = :heuristicName"),
    @NamedQuery(name = "YarnAppHeuristicResult.findBySeverity", query = "SELECT y FROM YarnAppHeuristicResult y WHERE y.severity = :severity"),
    @NamedQuery(name = "YarnAppHeuristicResult.findByIdAndHeuristicClass", query = "SELECT y FROM YarnAppHeuristicResult y WHERE y.yarnAppResultId = :yarnAppResultId AND y.heuristicClass = :heuristicClass"),
    @NamedQuery(name = "YarnAppHeuristicResult.findByScore", query = "SELECT y FROM YarnAppHeuristicResult y WHERE y.score = :score")})
public class YarnAppHeuristicResult implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "yarn_app_result_id")
    private String yarnAppResultId;
    @Lob
    @Size(max = 65535)
    @Column(name = "heuristic_class")
    private String heuristicClass;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "heuristic_name")
    private String heuristicName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "severity")
    private short severity;
    @Column(name = "score")
    private Integer score;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "yarnAppHeuristicResult")
    private Collection<YarnAppHeuristicResultDetails> yarnAppHeuristicResultDetailsCollection;

    public YarnAppHeuristicResult() {
    }

    public YarnAppHeuristicResult(Integer id) {
        this.id = id;
    }

    public YarnAppHeuristicResult(Integer id, String yarnAppResultId, String heuristicName, short severity) {
        this.id = id;
        this.yarnAppResultId = yarnAppResultId;
        this.heuristicName = heuristicName;
        this.severity = severity;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getYarnAppResultId() {
        return yarnAppResultId;
    }

    public void setYarnAppResultId(String yarnAppResultId) {
        this.yarnAppResultId = yarnAppResultId;
    }

    public String getHeuristicClass() {
        return heuristicClass;
    }

    public void setHeuristicClass(String heuristicClass) {
        this.heuristicClass = heuristicClass;
    }

    public String getHeuristicName() {
        return heuristicName;
    }

    public void setHeuristicName(String heuristicName) {
        this.heuristicName = heuristicName;
    }

    public short getSeverity() {
        return severity;
    }

    public void setSeverity(short severity) {
        this.severity = severity;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    @XmlTransient
    @JsonIgnore
    public Collection<YarnAppHeuristicResultDetails> getYarnAppHeuristicResultDetailsCollection() {
        return yarnAppHeuristicResultDetailsCollection;
    }

    public void setYarnAppHeuristicResultDetailsCollection(Collection<YarnAppHeuristicResultDetails> yarnAppHeuristicResultDetailsCollection) {
        this.yarnAppHeuristicResultDetailsCollection = yarnAppHeuristicResultDetailsCollection;
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
        if (!(object instanceof YarnAppHeuristicResult)) {
            return false;
        }
        YarnAppHeuristicResult other = (YarnAppHeuristicResult) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.jobs.jobhistory.YarnAppHeuristicResult[ id=" + id + " ]";
    }
    
}
