/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

import fish.payara.nucleus.cluster.PayaraCluster;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import javax.inject.Inject;
import javax.inject.Provider;

//com.sun.enterprise.iiop.security.Lookups
public class HK2Lookups {
  @Inject
  private Provider<PayaraCluster> payaraCluster;
  
  private static final ServiceLocator habitat = Globals.getDefaultHabitat();
  
  private static HK2Lookups singleton;
  
  private HK2Lookups() {
  }
  
  private static synchronized boolean checkSingleton() {
    if (singleton == null && habitat != null) {
      // Obtaining the singleton through the habitat will cause the injections to occur.
      singleton = habitat.create(HK2Lookups.class);
      habitat.inject(singleton);
      habitat.postConstruct(singleton);
    }
    return singleton != null;
  }
  
  static PayaraCluster getCluster() {
    return checkSingleton() ? singleton.payaraCluster.get() : null;
  }
}
