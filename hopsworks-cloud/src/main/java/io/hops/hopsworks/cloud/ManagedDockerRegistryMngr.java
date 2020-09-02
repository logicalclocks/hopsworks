/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.cloud;

import io.hops.hopsworks.common.python.environment.DockerRegistryMngr;
import io.hops.hopsworks.exceptions.ServiceException;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;

@Stateless
@ManagedStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ManagedDockerRegistryMngr implements DockerRegistryMngr {

  @Override
  public void gc() throws IOException, ServiceException {
    // TODO: implement this
  }
}
