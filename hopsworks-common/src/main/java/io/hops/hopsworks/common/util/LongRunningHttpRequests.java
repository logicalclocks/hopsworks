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
package io.hops.hopsworks.common.util;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

@Singleton
public class LongRunningHttpRequests {
  // In HA each node will have its own activeRequestCount. If one node exceeds maximum number of long-running http
  // requests the user is prompted to try again. If the retry is served by a node with activeRequestCount < maximum
  // number of long-running http requests, it will succeed.
  private int activeRequestCount = 0;
  
  @Lock(LockType.READ)
  public int get() {
    return activeRequestCount;
  }
  
  public void increment() {
    activeRequestCount++;
  }
  
  public void decrement() {
    activeRequestCount--;
  }
}
