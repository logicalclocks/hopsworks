package se.kth.bbc.jobs.cuneiform.model;

import java.util.Objects;

/**
 * A workflow output parameter: can be queried or not. The value of queried
 * parameters will be included in the workflow output.
 * <p>
 * @author stig
 */
public class OutputParameter {

  private final String name;
  private boolean queried;

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
