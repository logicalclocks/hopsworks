/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore;

import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetFacade;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.ops.ProvOps;
import io.hops.hopsworks.common.provenance.ops.ProvOpsAggregations;
import io.hops.hopsworks.common.provenance.ops.ProvOpsControllerIface;
import io.hops.hopsworks.common.provenance.ops.ProvOpsParamBuilder;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivity;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JobActivityBean {

  private final static Logger LOGGER = Logger.getLogger(JobActivityBean.class.getName());

  @Resource
  private TimerService timerService;

  @EJB
  private Settings settings;
  @Inject
  private ProvOpsControllerIface provOpsController;
  @EJB
  private FeaturegroupFacade featuregroupFacade;
  @EJB
  private TrainingDatasetFacade trainingDatasetFacade;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private FeaturestoreActivityFacade featurestoreActivityFacade;
  @EJB
  private ExecutionFacade executionFacade;


  @PostConstruct
  public void init() {
    Long time = settings.getConfTimeValue(settings.getFsJobActivityTime());
    TimeUnit unit = settings.getConfTimeTimeUnit(settings.getFsJobActivityTime());

    long timer = Math.max(TimeUnit.MILLISECONDS.convert(time, unit), 10000L);
    timerService.createIntervalTimer(10L, timer,
        new TimerConfig("Feature store job activity monitor", false));
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  @Timeout
  public void updateJobActivities() {
    try {
      List<Featuregroup> featureGroupList = featuregroupFacade.findAll();
      for (Featuregroup featureGroup : featureGroupList) {
        updateFeatureGroup(featureGroup);
      }

      List<TrainingDataset> trainingDatasetList = trainingDatasetFacade.findAll();
      for (TrainingDataset trainingDataset : trainingDatasetList) {
        updateTrainingDataset(trainingDataset);
      }
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error processing featuregroup: ", ex);
    }
  }

  private void updateFeatureGroup(Featuregroup featureGroup) throws Exception {
    // Build the params for the opensearch query. Filter for feature group (name_version) in a feature store (inodeId)
    // with the timestamp greater than last known application register for this feature group.
    // Look for create file activities.
    ProvOpsParamBuilder params = buildOpsParams(
        Utils.getFeaturegroupName(featureGroup),
        // TODO(Fabio): this is a bit lame, maybe add a FK between featurestore and dataset directly
        featurestoreController.getProjectFeaturestoreDataset(featureGroup.getFeaturestore().getProject()).getInodeId()
    );

    Optional<FeaturestoreActivity> execActivity = featurestoreActivityFacade.getMostRecentExecution(featureGroup);
    if (execActivity.isPresent()) {
      params.filterByField(ProvOps.FieldsPF.TIMESTAMP_GT, execActivity.get().getExecutionLastEventTime());
    }

    ProvOpsDTO aggProvOpsDTO = provOpsController.provFileOpsAggs(featureGroup.getFeaturestore().getProject(), params);
    // this is lame but the issue is down in the provenance code.
    // Everything in the provenance code returns a provOpsDTO which is problematic as it's not clear what it is
    // In this case the first level of provOpsDTO represents the different aggregations.
    // as in our case there is a single aggregation we call get(0).
    // get(0) always works because in the parser in the worst case, we return an empty string
    ProvOpsDTO provOpsDTO = aggProvOpsDTO.getItems().get(0);
    for (ProvOpsDTO dto : provOpsDTO.getItems()) {
      executionFacade.findByAppId(dto.getAppId())
          .ifPresent(execution -> featurestoreActivityFacade
              .logExecutionActivity(featureGroup, execution, dto.getTimestamp()));
    }
  }

  private void updateTrainingDataset(TrainingDataset trainingDataset) throws Exception {
    // Build the params for the opensearch query. Filter for feature group (name_version) in a feature store (inodeId)
    // with the timestamp greater than last known application register for this feature group.
    // Look for create file activities.
    ProvOpsParamBuilder params = buildOpsParams(
        Utils.getTrainingDatasetName(trainingDataset),
        // TODO(Fabio): this is a bit lame, maybe add a FK between featurestore and dataset directly
        featurestoreController.getProjectFeaturestoreDataset(trainingDataset.getFeaturestore().getProject())
            .getInodeId()
    );

    Optional<FeaturestoreActivity> execActivity = featurestoreActivityFacade.getMostRecentExecution(trainingDataset);
    if (execActivity.isPresent()) {
      params.filterByField(ProvOps.FieldsPF.TIMESTAMP_GT, execActivity.get().getExecutionLastEventTime());
    }

    ProvOpsDTO aggProvOpsDTO =
        provOpsController.provFileOpsAggs(trainingDataset.getFeaturestore().getProject(), params);
    // this is lame but the issue is down in the provenance code.
    // Everything in the provenance code returns a provOpsDTO which is problematic as it's not clear what it is
    // In this case the first level of provOpsDTO represents the different aggregations.
    // as in our case there is a single aggregation we call get(0).
    // get(0) always works because in the parser in the worst case, we return an empty string
    ProvOpsDTO provOpsDTO = aggProvOpsDTO.getItems().get(0);
    for (ProvOpsDTO dto : provOpsDTO.getItems()) {
      executionFacade.findByAppId(dto.getAppId())
          .ifPresent(execution -> featurestoreActivityFacade
              .logExecutionActivity(trainingDataset, execution, dto.getTimestamp()));
    }
  }

  private ProvOpsParamBuilder buildOpsParams(String entityName, Long featureStoreInodeId)
      throws ProvenanceException {
    return new ProvOpsParamBuilder()
        .filterByField(ProvOps.FieldsP.ML_ID, entityName)
        .filterByField(ProvOps.FieldsP.DATASET_I_ID, featureStoreInodeId)
        .filterByField(ProvOps.FieldsP.FILE_OPERATION, Provenance.FileOps.CREATE)
        .filterByField(ProvOps.FieldsP.FILE_OPERATION, Provenance.FileOps.DELETE)
        .filterByField(ProvOps.FieldsP.FILE_OPERATION, Provenance.FileOps.MODIFY_DATA)
        .withAggregation(ProvOpsAggregations.APP_IDS);
  }
}
