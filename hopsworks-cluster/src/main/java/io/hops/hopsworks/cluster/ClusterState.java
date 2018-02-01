/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.cluster;

import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Startup
@Singleton
public class ClusterState {
  private final static Logger LOG = Logger.getLogger(ClusterState.class.getName());
  
  //TODO Alex - make it read this from the variables tables
  public static final boolean bypassActivationLink = true;

  public boolean bypassActivationLink() {
    return bypassActivationLink;
  }
}
