/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.featuregroup;

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;

public class TestFeatureGroupInputValidation {

    private FeatureGroupInputValidation featureGroupInputValidation;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        featureGroupInputValidation = new FeatureGroupInputValidation();
    }

    @Test
    public void testVerifyUserInputFeatureGroup() throws Exception {
        CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
        featuregroupDTO.setTimeTravelFormat(TimeTravelFormat.HUDI);

        // timestamp type camel case
        List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
        newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
        newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false , false));
        newSchema.add(new FeatureGroupFeatureDTO("part_param3", "Timestamp", "", false , true));
        featuregroupDTO.setFeatures(newSchema);
        thrown.expect(FeaturestoreException.class);
        featureGroupInputValidation.verifyPartitionKeySupported(featuregroupDTO);
    }

}
