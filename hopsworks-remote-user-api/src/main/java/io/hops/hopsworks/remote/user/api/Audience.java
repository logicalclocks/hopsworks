/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api;


public class Audience {
  public static final String API = "api"; // Used by UI. Can access any rest endpoint
  public static final String JOB ="job"; // Used by internal services to start or stop jobs
  public static final String DATASET ="dataset";
  public static final String SERVING = "serving";
  public static final String EMAIL ="email";
  public static final String SERVICES ="services";
  public static final String GIT = "git";
  public static final String PROXY = "proxy";
}
