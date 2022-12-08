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
package io.hops.hopsworks.api.provenance.explicit.dto.featurestore;

import io.hops.hopsworks.api.provenance.explicit.dto.ProvNodeDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;

/**
 * when using JsonTypeInfo.As.PROPERTY as we do in the {@see FeaturegroupDTO.class}
 * java type erasure doesn't let jackson know the exact type of an artifact in the {@see ProvNodeDTO.class} generic
 * object
 * https://groups.google.com/g/jackson-user/c/dY4iFZyeAX8
 * We need the explicit class to disambiguate
 */
public class ProvOnDemandFeatureGroupDTO extends ProvNodeDTO<OnDemandFeaturegroupDTO> {
}
