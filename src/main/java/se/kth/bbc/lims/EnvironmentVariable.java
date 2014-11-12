package se.kth.bbc.lims;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "Env_vars")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "EnvironmentVariable.findAll", query = "SELECT e FROM EnvironmentVariable e"),
    @NamedQuery(name = "EnvironmentVariable.findByName", query = "SELECT e FROM EnvironmentVariable e WHERE e.name = :name")})
public class EnvironmentVariable implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "name")
    private String name;
    @Size(max = 1024)
    @Column(name = "value")
    private String value;

    public EnvironmentVariable() {
    }

    public EnvironmentVariable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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
        if (!(object instanceof EnvironmentVariable)) {
            return false;
        }
        EnvironmentVariable other = (EnvironmentVariable) object;
        if ((this.name == null && other.name != null) || (this.name != null && !this.name.equals(other.name))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.lims.EnvironmentVariable[ name=" + name + " ]";
    }
    
}
