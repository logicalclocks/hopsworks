package io.hops.hopsworks.common.dao.jobs;

import io.hops.hopsworks.common.jobs.MutableJsonObject;

/**
 * Signifies that this object can be translated in a more compact JSON format.
 * Mainly used to store JSON objects in the DB.
 * <p/>
 */
public interface JsonReduceable {

  /**
   * Get the contents of this instance in a compact JSON format.
   * <p/>
   * @return
   */
  public MutableJsonObject getReducedJsonObject();

  /**
   * Update the contents of the current object from the given JSON object.
   * <p/>
   * @param json
   * @throws IllegalArgumentException If the given JSON object cannot be
   * converted to the current class.
   */
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException;
}
