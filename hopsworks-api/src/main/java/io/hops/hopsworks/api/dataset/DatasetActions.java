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
package io.hops.hopsworks.api.dataset;

public class DatasetActions {

  public enum Get {
    BLOB(),// file content
    LISTING(),// ls file
    STAT();// stat file
  
    public static Get fromString(String param) {
      return valueOf(param.toUpperCase());
    }
  }
  
  public enum Post {
    CREATE(),
    COPY(),
    MOVE(),
    REJECT(),
    SHARE(),
    ACCEPT(),
    UNZIP(),
    ZIP();
  
    public static Post fromString(String param) {
      return valueOf(param.toUpperCase());
    }
  }
  
  public enum Put {
    PERMISSION(),
    DESCRIPTION();
  
    public static Put fromString(String param) {
      return valueOf(param.toUpperCase());
    }
  }
  
  public enum Delete {
    CORRUPTED(),
    UNSHARE(); // unshare will delete the shared reference
  
    public static Delete fromString(String param) {
      return valueOf(param.toUpperCase());
    }
  }
}
