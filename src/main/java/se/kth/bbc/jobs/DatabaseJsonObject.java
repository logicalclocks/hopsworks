package se.kth.bbc.jobs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Represents a mutable JSON object with only String values.
 * <p>
 * @author stig
 */
public class DatabaseJsonObject {

  private final Map<String, String> internalStrings;
  private final Map<String, DatabaseJsonObject> internalJsons;

  /**
   * Create a new mutable JSON object.
   */
  public DatabaseJsonObject() {
    internalStrings = new HashMap<>();
    internalJsons = new HashMap<>();
  }

  /**
   * Add a String key-value pair. If the key already exists, the value is
   * overwritten.
   * <p>
   * @param key
   * @param value
   */
  public void set(String key, String value) {
    internalStrings.put(key, value);
  }

  /**
   * Add a String-JSON object pair. If the key already exists, the value is
   * overwritten. Changes in the original value are also reflected in this
   * object.
   * <p>
   * @param key
   * @param value
   */
  public void set(String key, DatabaseJsonObject value) {
    internalJsons.put(key, value);
  }

  /**
   * Get the string value for this key.
   * <p>
   * @param key
   * @return
   * @throws IllegalArgumentException If the object does not contain a string
   * value for this key.
   */
  public String getString(String key) throws IllegalArgumentException {
    try {
      return internalStrings.get(key);
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot find String for key " + key
              + ".", e);
    }
  }

  /**
   * Get the JSON object value for this key.
   * <p>
   * @param key
   * @return
   * @throws IllegalArgumentException If the object does not contain a string
   * value for this key.
   */
  public DatabaseJsonObject getJsonObject(String key) throws
          IllegalArgumentException {
    try {
      return internalJsons.get(key);
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot find JSON object for key "
              + key
              + ".", e);
    }
  }

  /**
   * Check if this JSON object contains the given key on the highest level. This
   * means that no recursive checking is performed.
   * <p>
   * @param key
   * @return
   */
  public boolean containsKey(String key) {
    return internalStrings.containsKey(key) || internalJsons.containsKey(key);
  }

  /**
   * Return the number of top-level elements in this object.
   * <p>
   * @return
   */
  public int size() {
    return internalStrings.size() + internalJsons.size();
  }

  /**
   * Get a view on the keys in this JSON object. The keyset is not backed by the
   * map, nor vice versa.
   * <p>
   * @return
   */
  public Set<String> keySet() {
    Set<String> keys = new HashSet<>(internalStrings.size() + internalJsons.
            size());
    keys.addAll(internalStrings.keySet());
    keys.addAll(internalJsons.keySet());
    return keys;
  }

  /**
   * Get a String representation in JSON format of this object.
   * <p>
   * @return
   */
  public String toJson() {
    return toJsonObject().toString();
  }

  private JsonObject toJsonObject() {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    for (String s : internalStrings.keySet()) {
      builder.add(s, internalStrings.get(s));
    }
    for (String s : internalJsons.keySet()) {
      builder.add(s, internalJsons.get(s).toJsonObject());
    }
    return builder.build();
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 89 * hash + this.internalStrings.hashCode();
    hash = 89 * hash + this.internalJsons.hashCode();
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
    final DatabaseJsonObject other = (DatabaseJsonObject) obj;
    if (this.internalStrings.equals(other.internalStrings)) {
      return false;
    }
    if (!this.internalJsons.equals(other.internalJsons)) {
      return false;
    }
    return true;
  }

}
