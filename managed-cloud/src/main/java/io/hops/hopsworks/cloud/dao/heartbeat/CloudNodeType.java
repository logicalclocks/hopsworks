package io.hops.hopsworks.cloud.dao.heartbeat;

import com.google.gson.annotations.SerializedName;

public enum CloudNodeType {

  @SerializedName("localhost")
  Localhost("localhost"),

  @SerializedName("master")
  Master("master"),

  @SerializedName("worker")
  Worker("worker"),

  @SerializedName("ndb_mgm")
  NDB_MGM("ndb_mgm"),

  @SerializedName("ndb_ndbd")
  NDB_NDBD("ndb_ndbd"),

  @SerializedName("mysqld")
  MYSQLD("mysqld"),

  @SerializedName("ndb_api")
  NDB_API("ndb_api"),

  @SerializedName("rondb_all-in-one")
  RONDB_ALL_IN_ONE("rondb_all-in-one"),

  @SerializedName("secondary")
  Secondary("secondary");

  private final String value;

  CloudNodeType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
