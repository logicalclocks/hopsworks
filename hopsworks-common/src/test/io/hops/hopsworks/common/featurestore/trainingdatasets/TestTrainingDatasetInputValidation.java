/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

import io.hops.hopsworks.common.featurestore.trainingdatasets.split.TrainingDatasetSplitDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split.SplitName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TestTrainingDatasetInputValidation {
  
  private TrainingDatasetInputValidation trainingDatasetInputValidation = new TrainingDatasetInputValidation();
  
  private List<TrainingDatasetSplitDTO> splits;

  @Before
  public void setup() {
    splits = new ArrayList<>();
    splits.add(new TrainingDatasetSplitDTO("train", 0.8f));
    splits.add(new TrainingDatasetSplitDTO("test", 0.2f));
  }
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Test
  public void testValidateTrainSplit_nullSplits() throws Exception {
    thrown.expect(FeaturestoreException.class);
    trainingDatasetInputValidation.validateTrainSplit("train", null);
  }
  
  @Test
  public void testValidateTrainSplit_emptySplits() throws Exception {
    thrown.expect(FeaturestoreException.class);
    trainingDatasetInputValidation.validateTrainSplit("train", Collections.emptyList());
  }
  
  @Test
  public void testValidateTrainSplit_nullTrainSplit() throws Exception {
    thrown.expect(FeaturestoreException.class);
    trainingDatasetInputValidation.validateTrainSplit(null, splits);
  }
  
  @Test
  public void testValidateTrainSplit_wrongTrainSplit() throws Exception {
    thrown.expect(FeaturestoreException.class);
    trainingDatasetInputValidation.validateTrainSplit("val", splits);
  }

  @Test
  public void testValidateSplits_correctTimeSeriesSplit() throws Exception {
    List<TrainingDatasetSplitDTO> timeSeriesSplits = new ArrayList<>();
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TRAIN.getName(), new Date(10000), new Date(11000)));
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.VALIDATION.getName(), new Date(20000), new Date(21000)));
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TEST.getName(), new Date(30000), new Date(31000)));
    trainingDatasetInputValidation.validateSplits(timeSeriesSplits, "ts");
  }

  @Test
  public void testValidateSplits_timeSeriesSplitEventTimeNotAvailable() throws Exception {
    thrown.expect(FeaturestoreException.class);
    List<TrainingDatasetSplitDTO> timeSeriesSplits = new ArrayList<>();
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TRAIN.getName(), new Date(10000), new Date(11000)));
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.VALIDATION.getName(), new Date(20000), new Date(21000)));
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TEST.getName(), new Date(30000), new Date(31000)));
    trainingDatasetInputValidation.validateSplits(timeSeriesSplits, null);
  }

  @Test
  public void testValidateSplits_correctTimeSeriesSplit_sameStartAndEndTIme() throws Exception {
    List<TrainingDatasetSplitDTO> timeSeriesSplits = new ArrayList<>();
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TRAIN.getName(), new Date(10000), new Date(10000)));
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.VALIDATION.getName(), new Date(20000), new Date(20000)));
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TEST.getName(), new Date(30000), new Date(30000)));
    trainingDatasetInputValidation.validateSplits(timeSeriesSplits, "ts");
  }

  @Test
  public void testValidateSplits_nullTimeSeriesTrainSplit() throws Exception {
    thrown.expect(FeaturestoreException.class);
    List<TrainingDatasetSplitDTO> timeSeriesSplits = new ArrayList<>();
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TRAIN.getName(), null, null));
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TEST.getName(), new Date(30000), new Date(31000)));
    trainingDatasetInputValidation.validateSplits(timeSeriesSplits, "ts");
  }

  @Test
  public void testValidateSplits_nullTimeSeriesTestSplit() throws Exception {
    thrown.expect(FeaturestoreException.class);
    List<TrainingDatasetSplitDTO> timeSeriesSplits = new ArrayList<>();
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TRAIN.getName(), new Date(30000), new Date(31000)));
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TEST.getName(), null, null));
    trainingDatasetInputValidation.validateSplits(timeSeriesSplits, "ts");
  }

  @Test
  public void testValidateSplits_nullTimeSeriesValidationSplit() throws Exception {
    thrown.expect(FeaturestoreException.class);
    List<TrainingDatasetSplitDTO> timeSeriesSplits = new ArrayList<>();
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TRAIN.getName(), new Date(10000), new Date(11000)));
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.VALIDATION.getName(), null, null));
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TEST.getName(), new Date(30000), new Date(31000)));
    trainingDatasetInputValidation.validateSplits(timeSeriesSplits, "ts");
  }

  @Test
  public void testValidateSplits_wrongTimeSeriesTestSplit() throws Exception {
    thrown.expect(FeaturestoreException.class);
    List<TrainingDatasetSplitDTO> timeSeriesSplits = new ArrayList<>();
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TRAIN.getName(), new Date(10000), new Date(11000)));
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TEST.getName(), new Date(10000), new Date(31000)));
    trainingDatasetInputValidation.validateSplits(timeSeriesSplits, "ts");
  }

  @Test
  public void testValidateSplits_wrongTimeSeriesValidationSplit() throws Exception {
    thrown.expect(FeaturestoreException.class);
    List<TrainingDatasetSplitDTO> timeSeriesSplits = new ArrayList<>();
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TRAIN.getName(), new Date(10000), new Date(11000)));
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.VALIDATION.getName(), new Date(11000), new Date(31000)));
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TEST.getName(), new Date(30000), new Date(31000)));
    trainingDatasetInputValidation.validateSplits(timeSeriesSplits, "ts");
  }

  @Test
  public void testValidateSplits_timeSeriesTrainSplitWrongStartEndTIme() throws Exception {
    thrown.expect(FeaturestoreException.class);
    List<TrainingDatasetSplitDTO> timeSeriesSplits = new ArrayList<>();
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TRAIN.getName(), new Date(20000), new Date(11000)));
    trainingDatasetInputValidation.validateSplits(timeSeriesSplits, "ts");
  }

  @Test
  public void testValidateSplits_timeSeriesValidationSplitWrongStartEndTIme() throws Exception {
    thrown.expect(FeaturestoreException.class);
    List<TrainingDatasetSplitDTO> timeSeriesSplits = new ArrayList<>();
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.VALIDATION.getName(), new Date(20000), new Date(11000)));
    trainingDatasetInputValidation.validateSplits(timeSeriesSplits, "ts");
  }

  @Test
  public void testValidateSplits_timeSeriesTestSplitWrongStartEndTIme() throws Exception {
    thrown.expect(FeaturestoreException.class);
    List<TrainingDatasetSplitDTO> timeSeriesSplits = new ArrayList<>();
    timeSeriesSplits.add(new TrainingDatasetSplitDTO(SplitName.TEST.getName(), new Date(20000), new Date(11000)));
    trainingDatasetInputValidation.validateSplits(timeSeriesSplits, "ts");
  }
}
