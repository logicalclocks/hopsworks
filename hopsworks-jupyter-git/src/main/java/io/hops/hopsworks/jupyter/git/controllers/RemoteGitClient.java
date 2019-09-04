/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jupyter.git.controllers;

import io.hops.hopsworks.common.dao.user.security.secrets.SecretPlaintext;
import io.hops.hopsworks.exceptions.ServiceException;

import java.io.IOException;
import java.util.Set;

public interface RemoteGitClient {
  Set<String> fetchBranches(SecretPlaintext apiKey, String repository)
      throws ServiceException, IOException;
}
