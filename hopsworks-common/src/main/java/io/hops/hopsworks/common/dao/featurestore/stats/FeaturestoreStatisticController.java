/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore.stats;

import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.dao.featurestore.stats.cluster_analysis.ClusterAnalysisDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.desc_stats.DescriptiveStatsDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_correlation.FeatureCorrelationMatrixDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_distributions.FeatureDistributionsDTO;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDataset;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.List;

/**
 * Class controlling the interaction with the featurestore_statistics table and required business logic
 */
@Stateless
public class FeaturestoreStatisticController {
  @EJB
  private FeaturestoreStatisticFacade featurestoreStatisticFacade;

  /**
   * A method for updating the statistics of a featuregroup or training dataset,
   * it will overwrite the existing statistics with the new data
   *
   * @param featuregroup             the featuregroup to update
   * @param trainingDataset          the training dataset to update
   * @param featureCorrelationMatrix the new featurecorrelation matrix
   * @param descriptiveStatistics    the new descriptive statistics
   * @param featuresHistogram        the new feature histograms
   * @param clusterAnalysis          the new cluster analysis
   */
  public void updateFeaturestoreStatistics(
      Featuregroup featuregroup, TrainingDataset trainingDataset, FeatureCorrelationMatrixDTO featureCorrelationMatrix,
      DescriptiveStatsDTO descriptiveStatistics, FeatureDistributionsDTO featuresHistogram,
      ClusterAnalysisDTO clusterAnalysis) {
    if (featuregroup != null) {
      removeFeaturestoreStatistics((List) featuregroup.getStatistics());
    }
    if (trainingDataset != null) {
      removeFeaturestoreStatistics((List) trainingDataset.getStatistics());
    }
    insertFeaturestoreStatistics(featuregroup, trainingDataset, featureCorrelationMatrix, descriptiveStatistics,
        featuresHistogram, clusterAnalysis);
  }


  /**
   * A method for removing a list of statistic entries from the database
   *
   * @param featurestoreStatistics the list of featurestore statistics to remove
   */
  private void removeFeaturestoreStatistics(List<FeaturestoreStatistic> featurestoreStatistics) {
    featurestoreStatistics.stream().forEach(fs -> featurestoreStatisticFacade.remove(fs));
  }

  /**
   * A method for inserting new statistics into the database
   *
   * @param featuregroup             the featuregroup of the statistic
   * @param trainingDataset          the training dataset of the statistic
   * @param featureCorrelationMatrix the feature correlation data
   * @param descriptiveStatistics    the descriptive statistics data
   * @param featuresHistogram        the feature histograms data
   * @param clusterAnalysis          the cluster analysis data
   */
  private void insertFeaturestoreStatistics(
      Featuregroup featuregroup, TrainingDataset trainingDataset, FeatureCorrelationMatrixDTO featureCorrelationMatrix,
      DescriptiveStatsDTO descriptiveStatistics, FeatureDistributionsDTO featuresHistogram,
      ClusterAnalysisDTO clusterAnalysis) {
    List<FeaturestoreStatistic> featurestoreStatistics = new ArrayList();
    if (descriptiveStatistics != null)
      featurestoreStatistics.addAll(
          convertDescriptiveStatstoFeaturestoreStatistic(descriptiveStatistics, featuregroup, trainingDataset));
    if (featuresHistogram != null)
      featurestoreStatistics.addAll(
          convertFeatureDistributionsToFeaturestoreStatistics(featuresHistogram, featuregroup, trainingDataset));
    if (featureCorrelationMatrix != null)
      featurestoreStatistics.addAll(
          convertFeatureMatrixToFeaturestoreStatistics(featureCorrelationMatrix, featuregroup, trainingDataset));
    if (clusterAnalysis != null)
      featurestoreStatistics.add(
          convertClusterAnalysisToFeaturestoreStatistic(clusterAnalysis, featuregroup, trainingDataset));
    featurestoreStatistics.stream().forEach(fs -> featurestoreStatisticFacade.persist(fs));
  }

  /**
   * Utility for converting a DescriptiveStatsDTO into a FeaturestoreStatistic
   *
   * @param descriptiveStatsDTO the descriptive stats data
   * @param featuregroup        the featuregroup of the featurestoreStatistic
   * @param trainingDataset     the trainingDataset of the featurestoreStatistic
   * @return The featurestore statistic
   */
  private List<FeaturestoreStatistic> convertDescriptiveStatstoFeaturestoreStatistic(
      DescriptiveStatsDTO descriptiveStatsDTO, Featuregroup featuregroup, TrainingDataset trainingDataset) {
    List<FeaturestoreStatistic> featurestoreStatistics = new ArrayList<>();
    descriptiveStatsDTO.getDescriptiveStats().stream().forEach(ds -> {
      FeaturestoreStatistic featurestoreStatistic = new FeaturestoreStatistic();
      featurestoreStatistic.setName(ds.getFeatureName());
      featurestoreStatistic.setTrainingDataset(trainingDataset);
      featurestoreStatistic.setFeaturegroup(featuregroup);
      featurestoreStatistic.setValue(ds);
      featurestoreStatistic.setStatisticType(ds.getStatisticType());
      featurestoreStatistics.add(featurestoreStatistic);
    });
    return featurestoreStatistics;
  }

  /**
   * Utility for converting a clusterAnalysisDTO into a FeaturestoreStatistic
   *
   * @param clusterAnalysisDTO the cluster-analysis data
   * @param featuregroup       the featuregroup of the featurestoreStatistic
   * @param trainingDataset    the trainingDataset of the featurestoreStatistic
   * @return The featurestore statistic
   */
  private FeaturestoreStatistic convertClusterAnalysisToFeaturestoreStatistic(
      ClusterAnalysisDTO clusterAnalysisDTO, Featuregroup featuregroup, TrainingDataset trainingDataset) {
    FeaturestoreStatistic featurestoreStatistic = new FeaturestoreStatistic();
    featurestoreStatistic.setName("cluster analysis");
    featurestoreStatistic.setFeaturegroup(featuregroup);
    featurestoreStatistic.setTrainingDataset(trainingDataset);
    featurestoreStatistic.setValue(clusterAnalysisDTO);
    featurestoreStatistic.setStatisticType(clusterAnalysisDTO.getStatisticType());
    return featurestoreStatistic;
  }

  /**
   * Utility for converting a featureDistributionsDTO into a FeaturestoreStatistic
   *
   * @param featureDistributionsDTO the feature distributions data
   * @param featuregroup            the featuregroup of the featurestoreStatistic
   * @param trainingDataset         the trainingDataset of the featurestoreStatistic
   * @return The featurestore statistic
   */
  private List<FeaturestoreStatistic> convertFeatureDistributionsToFeaturestoreStatistics(
      FeatureDistributionsDTO featureDistributionsDTO, Featuregroup featuregroup,
      TrainingDataset trainingDataset) {
    List<FeaturestoreStatistic> featurestoreStatistics = new ArrayList<>();
    featureDistributionsDTO.getFeatureDistributions().stream().forEach(fd -> {
      FeaturestoreStatistic featurestoreStatistic = new FeaturestoreStatistic();
      featurestoreStatistic.setName(fd.getFeatureName());
      featurestoreStatistic.setTrainingDataset(trainingDataset);
      featurestoreStatistic.setFeaturegroup(featuregroup);
      featurestoreStatistic.setValue(fd);
      featurestoreStatistic.setStatisticType(fd.getStatisticType());
      featurestoreStatistics.add(featurestoreStatistic);
    });
    return featurestoreStatistics;
  }

  /**
   * Utility for converting a FeatureCorrelationMatrixDTO into a FeaturestoreStatistic
   *
   * @param featureCorrelationMatrixDTO the featurecorrelation data
   * @param featuregroup                the featuregroup of the featurestoreStatistic
   * @param trainingDataset             the trainingDataset of the featurestoreStatistic
   * @return The featurestore statistic
   */
  private List<FeaturestoreStatistic> convertFeatureMatrixToFeaturestoreStatistics(
      FeatureCorrelationMatrixDTO featureCorrelationMatrixDTO, Featuregroup featuregroup,
      TrainingDataset trainingDataset) {
    List<FeaturestoreStatistic> featurestoreStatistics = new ArrayList<>();
    featureCorrelationMatrixDTO.getFeatureCorrelations().stream().forEach(fc ->
        fc.getCorrelationValues().stream().forEach(cv -> {
          FeaturestoreStatistic featurestoreStatistic = new FeaturestoreStatistic();
          featurestoreStatistic.setFeaturegroup(featuregroup);
          featurestoreStatistic.setTrainingDataset(trainingDataset);
          featurestoreStatistic.setName(fc.getFeatureName());
          featurestoreStatistic.setValue(cv);
          featurestoreStatistic.setStatisticType(cv.getStatisticType());
          featurestoreStatistics.add(featurestoreStatistic);
        }));
    return featurestoreStatistics;
  }

}
