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
package io.hops.hopsworks.common.provenance.core;

import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.common.provenance.core.dto.ProvTypeDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;

import java.util.logging.Level;

public class Provenance {
  public enum Type {
    DISABLED(new ProvTypeDTO(Inode.MetaStatus.DISABLED, OpStore.NONE)),
    META(new ProvTypeDTO(Inode.MetaStatus.META_ENABLED, OpStore.NONE)),
    MIN(new ProvTypeDTO(Inode.MetaStatus.MIN_PROV_ENABLED, OpStore.STATE)),
    FULL(new ProvTypeDTO(Inode.MetaStatus.FULL_PROV_ENABLED, OpStore.ALL));
    
    public ProvTypeDTO dto;
    Type(ProvTypeDTO dto) {
      this.dto = dto;
    }
  }
  
  public static Type getProvType(ProvTypeDTO aux) throws ProvenanceException {
    switch(aux.getMetaStatus()) {
      case DISABLED: return Type.DISABLED;
      case META_ENABLED: return Type.META;
      case MIN_PROV_ENABLED: return Type.MIN;
      case FULL_PROV_ENABLED: return Type.FULL;
      default: throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.UNSUPPORTED, Level.INFO,
        "malformed type dto");
    }
  }
  
  public enum OpStore {
    NONE,
    STATE,
    ALL
  }
  
  public enum FileOps {
    CREATE,
    DELETE,
    ACCESS_DATA,
    MODIFY_DATA;
  }
  public enum MLType {
    FEATURE,
    TRAINING_DATASET,
    EXPERIMENT,
    MODEL,
    HIVE,
    DATASET,
    NONE
  }
  public enum AppState {
    SUBMITTED,
    RUNNING,
    FINISHED,
    KILLED,
    FAILED,
    UNKNOWN;
    
    public boolean isFinalState() {
      switch(this) {
        case FINISHED:
        case KILLED:
        case FAILED:
          return true;
        default: 
          return false;
      } 
    }
  }
  
  public enum FootprintType {
    ALL,
    INPUT,            //files read by the application
    OUTPUT_MODIFIED,  //existing files modified by application (not created or deleted)
    OUTPUT_ADDED,     //files newly created by application
    OUTPUT_TMP,              //files created and deleted by application
    OUTPUT_REMOVED
  }
  
  public static String getProjectIndex(Project project) {
    return project.getInode().getId() + Settings.PROV_FILE_INDEX_SUFFIX;
  }
}
