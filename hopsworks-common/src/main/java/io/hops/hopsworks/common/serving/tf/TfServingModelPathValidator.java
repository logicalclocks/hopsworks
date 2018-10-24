/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.serving.tf;

import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.List;

@Stateless
public class TfServingModelPathValidator {

  @EJB
  private InodeFacade inodeFacade;

  public void validateModelPath(String path, Integer version) throws IllegalArgumentException {
    try {
      List<Inode> children = inodeFacade.getChildren(Paths.get(path, version.toString()).toString());

      // C
      if (children.stream().noneMatch(inode -> inode.getInodePK().getName().equals("variables")) ||
          children.stream().noneMatch(inode -> inode.getInodePK().getName().contains(".pb"))) {
        throw new IllegalArgumentException("The model path does not respect the TensorFlow standard");
      }
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException("The model path provided does not exists");
    }
  }
}
