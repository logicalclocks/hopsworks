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
package io.hops.hopsworks.common.dataset;

import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FolderNameValidatorTest {

  @Test
  public void testProjectsAsDatasetName() {
    boolean isValid = true;
    try {
      FolderNameValidator.isValidName("Projects");
    } catch (DatasetException e) {
      isValid = false;
    }
    Assert.assertFalse(isValid);
  }

  @Test
  public void testProjectsAsProjectName() {
    Matcher m = FolderNameValidator.getProjectNameRegexValidator().matcher("Projects");
    boolean isValid = m.find();
    Assert.assertFalse(isValid);
  }

  @Test
  public void testLowercaseProjectsAsProjectName() {
    Matcher m = FolderNameValidator.getProjectNameRegexValidator().matcher("projects");
    boolean isValid = m.find();
    Assert.assertFalse(isValid);
  }

  @Test
  public void testDigitAsProjectName() {
    Matcher m = FolderNameValidator.getProjectNameRegexValidator().matcher("0");
    boolean isValid = m.find();
    Assert.assertFalse(isValid);
  }

  @Test
  public void testDoubleDigitAsProjectName() {
    Matcher m = FolderNameValidator.getProjectNameRegexValidator().matcher("02");
    boolean isValid = m.find();
    Assert.assertFalse(isValid);
  }

  @Test
  public void testStartWithSpecialCharAsProjectName() {
    Matcher m = FolderNameValidator.getProjectNameRegexValidator().matcher("_bla");
    boolean isValid = m.find();
    Assert.assertFalse(isValid);
  }

  @Test
  public void testEndsWithSpecialCharAsProjectName() {
    Matcher m = FolderNameValidator.getProjectNameRegexValidator().matcher("bla_");
    boolean isValid = m.find();
    Assert.assertFalse(isValid);
  }

  @Test
  public void testUnderscoreInMiddle() {
    Matcher m = FolderNameValidator.getProjectNameRegexValidator().matcher("foo_bar");
    boolean isValid = m.find();
    Assert.assertTrue(isValid);
  }

  @Test
  public void testUnderscoreInMiddleOfNumbers() {
    Matcher m = FolderNameValidator.getProjectNameRegexValidator().matcher("0_1");
    boolean isValid = m.find();
    Assert.assertTrue(isValid);
  }

  @Test
  public void testProjectsOneAsProjectName() {
    Matcher m = FolderNameValidator.getProjectNameRegexValidator().matcher("Projects One");
    boolean isValid = m.find();
    Assert.assertFalse(isValid);
  }

  @Test
  public void testDoubleUnderscoreInProjectName() {
    Matcher m = FolderNameValidator.getProjectNameRegexValidator().matcher("project__one");
    boolean isValid = m.find();
    Assert.assertFalse(isValid);
  }

  @Test
  public void testDoubleUnderscoreInDatasetName() {
    boolean isValid = true;
    try {
      FolderNameValidator.isValidName("dataset__name");
    } catch (DatasetException e) {
      isValid = false;
    }
    Assert.assertFalse(isValid);
  }

  @Test
  public void testSpaceInDatasetName() {
    boolean isValid = true;
    try {
      FolderNameValidator.isValidName("dataset name");
    } catch (DatasetException e) {
      isValid = false;
    }
    Assert.assertFalse(isValid);
  }

  @Test
  public void testSpaceProjectsInDatasetName() {
    boolean isValid = true;
    try {
      FolderNameValidator.isValidName("dataset Projects");
    } catch (DatasetException e) {
      isValid = false;
    }
    Assert.assertFalse(isValid);
  }

  @Test
  public void testStartDotInDatasetName() {
    boolean isValid = true;
    try {
      FolderNameValidator.isValidName(".dataset");
    } catch (DatasetException e) {
      isValid = false;
    }
    Assert.assertTrue(isValid);
  }

  @Test
  public void testEndDotInDatasetName() {
    boolean isValid = true;
    try {
      FolderNameValidator.isValidName("dataset.");
    } catch (DatasetException e) {
      isValid = false;
    }
    Assert.assertFalse(isValid);
  }

  @Test
  public void testDotInDatasetName() {
    boolean isValid = true;
    try {
      FolderNameValidator.isValidName("dataset.new");
    } catch (DatasetException e) {
      isValid = false;
    }
    Assert.assertTrue(isValid);
  }

  @Test
  public void testStartDashInDatasetName() {
    boolean isValid = true;
    try {
      FolderNameValidator.isValidName("-dataset");
    } catch (DatasetException e) {
      isValid = false;
    }
    Assert.assertTrue(isValid);
  }

  @Test
  public void testEndDashInDatasetName() {
    boolean isValid = true;
    try {
      FolderNameValidator.isValidName("dataset-");
    } catch (DatasetException e) {
      isValid = false;
    }
    Assert.assertTrue(isValid);
  }

  @Test
  public void testDashInDatasetName() {
    boolean isValid = true;
    try {
      FolderNameValidator.isValidName("dataset-new");
    } catch (DatasetException e) {
      isValid = false;
    }
    Assert.assertTrue(isValid);
  }

  @Test
  public void testProjectNameRegex() {
    Settings settings = new Settings();
    StringTokenizer tokenizer = new StringTokenizer(settings.getProjectNameReservedWordsTest(), ",");
    HashSet<String> tokens = new HashSet<>(tokenizer.countTokens());
    while (tokenizer.hasMoreTokens()) {
      tokens.add(tokenizer.nextToken().trim());
    }
    Pattern projectNameRegexValidator = Pattern.compile(FolderNameValidator.getProjectNameRegexStr(tokens),
      Pattern.CASE_INSENSITIVE);
    Matcher m = projectNameRegexValidator.matcher("VIEWS");
    boolean isValid = m.find();
    Assert.assertFalse(isValid);
    m = projectNameRegexValidator.matcher("views");
    isValid = m.find();
    Assert.assertFalse(isValid);
    m = projectNameRegexValidator.matcher("viewss");
    isValid = m.find();
    Assert.assertTrue(isValid);
  }

  @Test
  public void testProjectNameRegexForUnderscoreFeaturestore() {
    Settings settings = new Settings();
    StringTokenizer tokenizer = new StringTokenizer(settings.getProjectNameReservedWordsTest(), ",");
    HashSet<String> tokens = new HashSet<>(tokenizer.countTokens());
    while (tokenizer.hasMoreTokens()) {
      tokens.add(tokenizer.nextToken().trim());
    }
    Pattern projectNameRegexValidator = Pattern.compile(FolderNameValidator.getProjectNameRegexStr(tokens),
      Pattern.CASE_INSENSITIVE);
    Matcher m = projectNameRegexValidator.matcher("a_FeaturestoreFoo");
    boolean isValid = m.find();
    Assert.assertFalse(isValid);

    m = projectNameRegexValidator.matcher("a_FEATURESTORE");
    isValid = m.find();
    Assert.assertFalse(isValid);

    m = projectNameRegexValidator.matcher("a_featurestore");
    isValid = m.find();
    Assert.assertFalse(isValid);

    m = projectNameRegexValidator.matcher("featurestore_a");
    isValid = m.find();
    Assert.assertTrue(isValid);
  }
}
