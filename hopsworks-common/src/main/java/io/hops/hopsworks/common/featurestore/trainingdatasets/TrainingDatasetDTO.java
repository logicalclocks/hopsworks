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

package io.hops.hopsworks.common.featurestore.trainingdatasets;

import io.hops.hopsworks.common.featurestore.FeaturestoreEntityDTO;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.keyword.KeywordDTO;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsConfigDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.split.TrainingDatasetSplitDTO;
import io.hops.hopsworks.common.tags.TagsDTO;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO containing the human-readable information of a trainingDataset, can be converted to JSON or XML representation
 * using jaxb.
 */
@XmlRootElement
public class TrainingDatasetDTO extends FeaturestoreEntityDTO<TrainingDatasetDTO> {
  
  private String dataFormat;
  private Boolean coalesce;
  private TrainingDatasetType trainingDatasetType;
  // set defaults so old clients don't get broken
  private List<TrainingDatasetSplitDTO> splits = new ArrayList<>();
  private Long seed = null;
  private String trainSplit = null;

  private FeaturestoreStorageConnectorDTO storageConnector;

  // This is here for the frontend. The frontend uses a rest call to get the total size of
  // a subdirectory - the rest call requires the inode id.
  private Long inodeId;

  private QueryDTO queryDTO;

  private Boolean fromQuery;
  private List<TrainingDatasetFeatureDTO> features;

  private Double sampleRatio;
  @XmlJavaTypeAdapter(DateAdapter.class)
  private Date eventStartTime;
  @XmlJavaTypeAdapter(DateAdapter.class)
  private Date eventEndTime;

  // Below fields do not need to provide
  private KeywordDTO keywords;
  private TagsDTO tags;
  private Boolean dataAvailable;

  public TrainingDatasetDTO() {
  }

  public TrainingDatasetDTO(TrainingDataset trainingDataset) {
    super(trainingDataset.getFeaturestore().getId(), trainingDataset.getName(), trainingDataset.getCreated(),
      trainingDataset.getCreator(), trainingDataset.getVersion(), trainingDataset.getId(),
        new StatisticsConfigDTO(trainingDataset.getStatisticsConfig()));
    setDescription(trainingDataset.getDescription());
    this.dataFormat = trainingDataset.getDataFormat();
    this.coalesce = trainingDataset.getCoalesce();
    this.trainingDatasetType = trainingDataset.getTrainingDatasetType();
    this.splits =
      trainingDataset.getSplits().stream().map(tds -> new TrainingDatasetSplitDTO(tds.getName(), tds.getPercentage()))
        .collect(Collectors.toList());
    this.seed = trainingDataset.getSeed();
    this.fromQuery = trainingDataset.isQuery();
    this.trainSplit = trainingDataset.getTrainSplit();
    this.eventStartTime = trainingDataset.getStartTime();
    this.eventEndTime = trainingDataset.getEndTime();
    this.sampleRatio = trainingDataset.getSampleRatio();
  }
  
  @XmlElement
  public String getDataFormat() {
    return dataFormat;
  }
  
  public void setDataFormat(String dataFormat) {
    this.dataFormat = dataFormat;
  }

  public Boolean getCoalesce() {
    return coalesce;
  }

  public void setCoalesce(Boolean coalesce) {
    this.coalesce = coalesce;
  }

  public FeaturestoreStorageConnectorDTO getStorageConnector() {
    return storageConnector;
  }

  public void setStorageConnector(FeaturestoreStorageConnectorDTO storageConnector) {
    this.storageConnector = storageConnector;
  }

  public Long getInodeId() {
    return inodeId;
  }

  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }

  @XmlElement
  public TrainingDatasetType getTrainingDatasetType() {
    return trainingDatasetType;
  }
  
  public void setTrainingDatasetType(
    TrainingDatasetType trainingDatasetType) {
    this.trainingDatasetType = trainingDatasetType;
  }

  @XmlElement
  public List<TrainingDatasetSplitDTO> getSplits() {
    return splits;
  }
  
  public void setSplits(
    List<TrainingDatasetSplitDTO> splits) {
    this.splits = splits;
  }
  
  @XmlElement
  public Long getSeed() {
    return seed;
  }
  
  public void setSeed(Long seed) {
    this.seed = seed;
  }

  public QueryDTO getQueryDTO() {
    return queryDTO;
  }

  public void setQueryDTO(QueryDTO queryDTO) {
    this.queryDTO = queryDTO;
  }

  public List<TrainingDatasetFeatureDTO> getFeatures() {
    return features;
  }

  public void setFeatures(List<TrainingDatasetFeatureDTO> features) {
    this.features = features;
  }

  public Boolean getFromQuery() {
    return fromQuery;
  }

  public void setFromQuery(Boolean fromQuery) {
    this.fromQuery = fromQuery;
  }
  
  public String getTrainSplit() {
    return trainSplit;
  }
  
  public void setTrainSplit(String trainSplit) {
    this.trainSplit = trainSplit;
  }

  public Double getSampleRatio() {
    return sampleRatio;
  }

  public void setSampleRatio(Double sampleRatio) {
    this.sampleRatio = sampleRatio;
  }

  public Date getEventStartTime() {
    return eventStartTime;
  }

  public void setEventStartTime(Date eventStartTime) {
    this.eventStartTime = eventStartTime;
  }

  public Date getEventEndTime() {
    return eventEndTime;
  }

  public void setEventEndTime(Date eventEndTime) {
    this.eventEndTime = eventEndTime;
  }

  public KeywordDTO getKeywords() {
    return keywords;
  }

  public void setKeywords(KeywordDTO keywords) {
    this.keywords = keywords;
  }

  public TagsDTO getTags() {
    return tags;
  }

  public void setTags(TagsDTO tags) {
    this.tags = tags;
  }

  public Boolean isDataAvailable() {
    return dataAvailable;
  }

  public void setDataAvailable(Boolean dataAvailable) {
    this.dataAvailable = dataAvailable;
  }

  @Override
  public String toString() {
    return "TrainingDatasetDTO{" +
      "dataFormat='" + dataFormat + '\'' +
      ", trainingDatasetType=" + trainingDatasetType +
      ", splits=" + splits +
      ", seed=" + seed +
      ", trainSplit=" + trainSplit +
      ", inodeId=" + inodeId +
      '}';
  }
}
