package se.kth.bbc.cuneiform;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author stig
 */
public class CuneiformParameter implements Serializable {

    private String name;
    private String value;

    public CuneiformParameter() {
    }

    public CuneiformParameter(String name, String value) {
        this.name = name;
        this.value = value;
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
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Objects.hashCode(this.value);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CuneiformParameter other = (CuneiformParameter) obj;

        if ((this.name == null || name.isEmpty()) && (this.value == null || value.isEmpty())) {
            return (other.name == null || other.name.isEmpty()) && (other.value == null || other.value.isEmpty());
        }
        if (this.name == null || name.isEmpty()) {
            return (other.name == null || other.name.isEmpty()) && this.value.equals(other.value);
        }
        if (this.value == null || value.isEmpty()) {
            return (other.value == null || other.value.isEmpty()) && this.name.equals(other.name);
        }
        return value.equals(other.value) && name.equals(other.name);
    }

}
