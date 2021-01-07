/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.featuregroup.ondemand;

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandDataFormat;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO containing the human-readable information of an on-demand featuregroup in the feature store, can be
 * converted to JSON or XML representation using jaxb.
 */
@XmlRootElement
public class OnDemandFeaturegroupDTO extends FeaturegroupDTO {

  private FeaturestoreStorageConnectorDTO storageConnector;
  private String query;

  private OnDemandDataFormat dataFormat;
  private String path;

  // This is useful if, for instance, we have a feature group defined over a set of CSV files
  // we'll always have to configure elements like `header` and `inferSchema`, also when querying the
  // on-demand feature group.
  // In the future we should consider expanding the concept of options also to cached feature groups.
  private List<OnDemandOptionDTO> options;

  public OnDemandFeaturegroupDTO() {
    super();
  }

  public OnDemandFeaturegroupDTO(String featureStoreName, Featuregroup featuregroup,
                                 FeaturestoreStorageConnectorDTO storageConnectorDTO) {
    super(featuregroup);
    this.query = featuregroup.getOnDemandFeaturegroup().getQuery();
    this.storageConnector = storageConnectorDTO;
    this.dataFormat = featuregroup.getOnDemandFeaturegroup().getDataFormat();
    this.path = featuregroup.getOnDemandFeaturegroup().getPath();

    this.options = featuregroup.getOnDemandFeaturegroup().getOptions().stream()
        .map(o -> new OnDemandOptionDTO(o.getName(), o.getValue()))
        .collect(Collectors.toList());

    setFeaturestoreName(featureStoreName);
    setDescription(featuregroup.getOnDemandFeaturegroup().getDescription());
    setFeatures(featuregroup.getOnDemandFeaturegroup().getFeatures().stream().map(fgFeature ->
        new FeatureGroupFeatureDTO(fgFeature.getName(),
            fgFeature.getType(),
            fgFeature.getDescription(), featuregroup.getId())).collect(Collectors.toList()));
  }

  public OnDemandFeaturegroupDTO(Featuregroup featuregroup, FeaturestoreStorageConnectorDTO storageConnectorDTO) {
    super(featuregroup);
    this.query = featuregroup.getOnDemandFeaturegroup().getQuery();
    this.storageConnector = storageConnectorDTO;
    this.dataFormat = featuregroup.getOnDemandFeaturegroup().getDataFormat();
    this.path = featuregroup.getOnDemandFeaturegroup().getPath();
    this.options = featuregroup.getOnDemandFeaturegroup().getOptions().stream()
        .map(o -> new OnDemandOptionDTO(o.getName(), o.getValue()))
        .collect(Collectors.toList());
  }

  public FeaturestoreStorageConnectorDTO getStorageConnector() {
    return storageConnector;
  }

  public void setStorageConnector(FeaturestoreStorageConnectorDTO storageConnector) {
    this.storageConnector = storageConnector;
  }

  @XmlElement
  public String getQuery() {
    return query;
  }
  
  public void setQuery(String query) {
    this.query = query;
  }

  public OnDemandDataFormat getDataFormat() {
    return dataFormat;
  }

  public void setDataFormat(OnDemandDataFormat dataFormat) {
    this.dataFormat = dataFormat;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<OnDemandOptionDTO> getOptions() {
    return options;
  }

  public void setOptions(List<OnDemandOptionDTO> options) {
    this.options = options;
  }
}
