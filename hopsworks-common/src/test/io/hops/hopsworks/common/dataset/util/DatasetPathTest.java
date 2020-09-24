/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.dataset.util;

import io.hops.hopsworks.persistence.entity.project.Project;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

public class DatasetPathTest {

  @Test
  public void testProjectsInDatasetName() throws UnsupportedEncodingException {
    Project project = new Project("project1");
    String path = "/Projects/project1/Projects";
    String root = "Projects";
    DatasetPath datasetPath = new DatasetPath(project, path, root);
    Assert.assertEquals(datasetPath.getDatasetName(), "Projects");
    Assert.assertEquals(datasetPath.getDatasetFullPath().toString(), "/Projects/project1/Projects");
    Assert.assertEquals(datasetPath.getFullPath().toString(), "/Projects/project1/Projects");
  }

  @Test
  public void testProjectsInDatasetNameRelativePath() throws UnsupportedEncodingException {
    Project project = new Project("project1");
    String path = "Projects";
    String root = "Projects";
    DatasetPath datasetPath = new DatasetPath(project, path, root);
    Assert.assertEquals(datasetPath.getDatasetName(), "Projects");
    Assert.assertEquals(datasetPath.getDatasetFullPath().toString(), "/Projects/project1/Projects");
    Assert.assertEquals(datasetPath.getFullPath().toString(), "/Projects/project1/Projects");
  }

  @Test
  public void testProjectsInDatasetNameShared() throws UnsupportedEncodingException {
    Project project = new Project("project1");
    String path = "/Projects/project1/project2::Projects";
    String root = "Projects";
    DatasetPath datasetPath = new DatasetPath(project, path, root);
    Assert.assertEquals(datasetPath.getDatasetName(), "Projects");
    Assert.assertEquals(datasetPath.getDatasetFullPath().toString(), "/Projects/project2/Projects");
    Assert.assertEquals(datasetPath.getFullPath().toString(), "/Projects/project2/Projects");
  }

  @Test
  public void testProjectsInDatasetNameSharedRelativePath() throws UnsupportedEncodingException {
    Project project = new Project("project1");
    String path = "project2::Projects";
    String root = "Projects";
    DatasetPath datasetPath = new DatasetPath(project, path, root);
    Assert.assertEquals(datasetPath.getDatasetName(), "Projects");
    Assert.assertEquals(datasetPath.getDatasetFullPath().toString(), "/Projects/project2/Projects");
    Assert.assertEquals(datasetPath.getFullPath().toString(), "/Projects/project2/Projects");
  }

  @Test
  public void testUnderScoreInSubdir() throws UnsupportedEncodingException {
    Project project = new Project("project1");
    String path = "/Projects/project1/dataset/sub__dir";
    String root = "Projects";
    DatasetPath datasetPath = new DatasetPath(project, path, root);
    Assert.assertEquals(datasetPath.getDatasetName(), "dataset");
    Assert.assertEquals(datasetPath.getDatasetFullPath().toString(), "/Projects/project1/dataset");
    Assert.assertEquals(datasetPath.getFullPath().toString(), "/Projects/project1/dataset/sub__dir");
  }
}
