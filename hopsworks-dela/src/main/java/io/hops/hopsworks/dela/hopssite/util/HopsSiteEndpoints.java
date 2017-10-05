package io.hops.hopsworks.dela.hopssite.util;

import javax.ejb.Stateless;

@Stateless
public class HopsSiteEndpoints {
  
  public static final String CLUSTER_SERVICE_ROLE = "cluster/role";
  
  public static final String DATASET_SERVICE_POPULAR = "dataset/popular";
  
  public static final String DATASET_SERVICE_GET_BY_PUBLIC_ID = "dataset/byPublicId";
  public static final String DATASET_SERVICE_GET = "dataset";
  
  public static final String DATASET_SERVICE_ADD_CATEGORY = "dataset/category";
  public static final String DATASET_SERVICE_BY_PUBLIC_ID = "dataset/byPublicId";
  public static final String DATASET_SERVICE_ISSUE = "dataset/issue";
  
  
  
  public static final String RATING_SERVICE = "rating";
  public static final String RATING_SERVICE_ALL_BY_PUBLICID = "rating/all/byPublicId";
  
  public static final String USER_SERVICE = "user";
  
}