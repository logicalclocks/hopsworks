package se.kth.bbc.jobs.cuneiform.model;

import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A workflow output parameter: can be queried or not. The value of queried
 * parameters will be included in the workflow output.
 * <p>
 * @author stig
 */
@XmlRootElement
public class OutputParameter {

  private String name;
  private boolean queried;
  
  public OutputParameter(){} //Needed by JAXB

  /**
   * Creates a new non-queried OutputParameter with the given name.
   * <p>
   * @param name
   */
  public OutputParameter(String name) {
    this(name, false);
  }

  /**
   * Creates a new OutputParameter with the given name and query status.
   * <p>
   * @param name
   * @param queried
   */
  public OutputParameter(String name, boolean queried) {
    this.name = name;
    this.queried = queried;
  }

  /**
   * Get the name of the OutputParameter.
   * <p>
   * @return
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the OutputParameter
   * @param name 
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Check if the OutputParameter is queried. If it is queried, its value will
   * be included in the output of the workflow.
   * <p>
   * @return
   */
  public boolean isQueried() {
    return queried;
  }

  /**
   * Set the query status of the OutputParameter. If it is queried, its value
   * will be included in the output of the workflow.
   * <p>
   * @param queried
   */
  public void setQueried(boolean queried) {
    this.queried = queried;
  }

  /**
   * Get a hashcode for the OutputParameter.
   * <p>
   * @return
   */
  @Override
  public int hashCode() {
    int hash = 3;
    hash = 53 * hash + Objects.hashCode(this.name);
    return hash;
  }

  /**
   * Check if the OutputParameter and obj are identical. They are so if they
   * have the same name.
   * <p>
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
    final OutputParameter other = (OutputParameter) obj;
    return this.name.equals(other.name);
  }

}
