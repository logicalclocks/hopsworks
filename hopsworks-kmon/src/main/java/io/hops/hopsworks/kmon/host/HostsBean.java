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

package io.hops.hopsworks.kmon.host;

import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import org.primefaces.model.LazyDataModel;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

@ManagedBean
@ViewScoped
public class HostsBean implements Serializable {
  private static final Logger LOG = Logger.getLogger(HostsBean.class.getName());
  
  @EJB
  private HostsFacade hostsFacade;
  
  private LazyDataModel<Hosts> lazyHosts;
  private List<Hosts> selectedHosts;
  
  @PostConstruct
  public void init() {
    lazyHosts = new HostsLazyDataModel(hostsFacade);
  }
  
  public LazyDataModel<Hosts> getLazyHosts() {
    return lazyHosts;
  }
  
  public List<Hosts> getSelectedHosts() {
    return selectedHosts;
  }
  
  public void setSelectedHosts(List<Hosts> selectedHosts) {
    this.selectedHosts = selectedHosts;
  }
}
