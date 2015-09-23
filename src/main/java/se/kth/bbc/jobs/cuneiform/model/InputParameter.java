package se.kth.bbc.jobs.cuneiform.model;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A workflow input parameter. Has a name and value, and can be bound. Binding a
 * parameter means setting its value. If the value is null, the parameter is
 * unbound.
 * <p/>
 * @author stig
 */
@XmlRootElement
public class InputParameter implements Serializable {

  private String name;
  private String value;

  public InputParameter() {
  }//Needed by JAXB

  /**
   * Create a new InputParameter with given name and bound value.
   * <p/>
   * @param name
   * @param value
   */
  public InputParameter(String name, String value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Create a new InputParameter with given name.
   * <p/>
   * @param name
   */
  public InputParameter(String name) {
    this(name, null);
  }

  /**
   * Get the name of the InputParameter.
   * <p/>
   * @return
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the InputParameter.
   * <p/>
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the bound value of the InputParameter. If the returned value is null,
   * this parameter is unbound.
   * <p/>
   * @return
   */
  public String getValue() {
    return value;
  }

  /**
   * Set the bound value for the InputParameter. Passing a null argument will
   * unbind the parameter.
   * <p/>
   * @param value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Utility method for checking if the InputParameter is bound. Returns true if
   * the bound value is null.
   * <p/>
   * @return
   */
  public boolean isBound() {
    return value != null;
  }

  /**
   * Utility method for unbinding the InputParameter. Equivalent to
   * setValue(null).
   */
  public void unBind() {
    value = null;
  }

  /**
   * Bind the InputParameter to the given value. Alias for setValue().
   * <p/>
   * @param value
   */
  public void bind(String value) {
    setValue(value);
  }

  /**
   * Get a hashcode for the given InputParameter.
   * <p/>
   * @return
   */
  @Override
  public int hashCode() {
    int hash = 5;
    hash = 89 * hash + Objects.hashCode(this.name);
    return hash;
  }

  /**
   * Check if the InputParameter and the obj are identical. They are so if they
   * have the same name.
   * <p/>
   * @param obj
   * @return
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final InputParameter other = (InputParameter) obj;
    return other.name.equals(this.name);
  }

}
