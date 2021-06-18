/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat;

public class Version {
  public static final Version V010 = new Version(0, 1, 0);
  public static final Version V020 = new Version(0, 2, 0);

  public static final Version CURRENT = V020;

  private final Integer major;
  private final Integer minor;
  private final Integer patch;

  public Version(Integer major, Integer minor, Integer patch) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
  }

  public Integer getMajor() {
    return major;
  }

  public Integer getMinor() {
    return minor;
  }

  public Integer getPatch() {
    return patch;
  }

  @Override
  public String toString() {
    return major + "." + minor + "." + patch;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof Version) {
      Version vother = (Version) other;
      return major.equals(vother.getMajor()) && minor.equals(vother.getMinor()) && patch.equals(vother.getPatch());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + major;
    result = 31 * result + minor;
    result = 31 * result + patch;
    return result;
  }
}
