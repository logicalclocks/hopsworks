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
package io.hops.hopsworks.common.provenance.app;

import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateElastic;
import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateDTO;
import io.hops.hopsworks.exceptions.ProvenanceException;

import java.util.Map;

public class ProvAppHelper {
  public static ProvAppStateDTO buildAppState(Map<Provenance.AppState, ProvAppStateElastic> appStates)
    throws ProvenanceException {
    ProvAppStateDTO mlAssetAppState = new ProvAppStateDTO();
    //app states is an ordered map
    //I assume values will still be ordered based on keys
    //if this is the case, the correct progression is SUBMITTED->RUNNING->FINISHED/KILLED/FAILED
    //as such just iterating over the states will provide us with the correct current state
    for (ProvAppStateElastic appState : appStates.values()) {
      mlAssetAppState.setAppState(appState.getAppState(), appState.getAppStateTimestamp());
    }
    return mlAssetAppState;
  }
}
