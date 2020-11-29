/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.common.featurestore.xattr.dto;

import io.hops.hopsworks.common.provenance.core.ProvXAttrs;

public class FeaturestoreXAttrsConstants {
  public static final String ELASTIC_XATTR = "xattr";
  public static final String PROJECT_ID = "project_id";
  public static final String PROJECT_NAME = "project_name";
  public static final String DATASET_INODE_ID = "dataset_iid";
  public static final String FEATURESTORE = "featurestore";
  public static final String FEATURESTORE_ID = "featurestore_id";
  public static final String NAME = "name";
  public static final String VERSION = "version";
  public static final String FG_FEATURES = "fg_features";
  public static final String TD_FEATURES = "td_features";
  public static final String DESCRIPTION = "description";
  public static final String CREATE_DATE = "create_date";
  public static final String CREATOR = "creator";
  public static final String KEYWORDS = "keywords";
  
  public static final String TAGS = "tags";
  
  public static String getFeaturestoreXAttrKey() {
    return ProvXAttrs.PROV_XATTR
      + "." + FEATURESTORE;
  }
  public static String getFeaturestoreElasticKey() {
    return ELASTIC_XATTR
      + "." + FEATURESTORE;
  }
  
  public static String getFeaturestoreElasticKey(String... key) {
    StringBuilder sb = new StringBuilder(getFeaturestoreElasticKey());
    for(String part: key) {
      sb.append( "." + part);
    }
    return sb.toString();
  }
  
  public static String getTagsElasticKey() {
    return ELASTIC_XATTR
      + "." + TAGS
      + ".key";
  }
  
  public static String getTagsElasticValue() {
    return ELASTIC_XATTR
      + "." + TAGS
      + ".value";
  }
}
