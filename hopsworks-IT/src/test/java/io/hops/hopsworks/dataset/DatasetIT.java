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
package io.hops.hopsworks.dataset;

import io.hops.hopsworks.WebDriverFactory;
import io.hops.hopsworks.util.DBHelper;
import io.hops.hopsworks.util.Helpers;
import io.hops.hopsworks.util.helpers.DatasetHelper;
import io.hops.hopsworks.util.helpers.LoginHelper;
import io.hops.hopsworks.util.helpers.ProjectHelper;
import io.hops.hopsworks.util.models.Dataset;
import io.hops.hopsworks.util.models.Project;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DatasetIT {
  private static final Logger LOGGER = Logger.getLogger(DatasetIT.class.getName());
  private static WebDriver driver;
  private static DBHelper dbHelper;
  private static Project project;
  private static final StringBuffer verificationErrors = new StringBuffer();
  
  @BeforeClass
  public static void init() {
    driver = WebDriverFactory.getWebDriver();
    dbHelper = new DBHelper();
    LoginHelper.loginAsUser(driver, dbHelper);
    project = ProjectHelper.createProject(driver, dbHelper);
  }
  
  @Test
  public void createDataset() {
    ProjectHelper.gotoProject(driver, project);
    DatasetHelper.gotoDatasetPage(driver);
    Dataset dataset = DatasetHelper.createNewDataset(driver, dbHelper, project);
    By newDatasetID = By.id("table-row-" + dataset.getName());
    WebElement row = driver.findElement(newDatasetID);
    List<WebElement> cells = row.findElements(By.tagName("td"));
    String datasetName = dataset.getName().length() > 170 ? dataset.getName().substring(0, 170) + "..." :
      dataset.getName();
    try {
      assertEquals(datasetName, cells.get(1).getText());
    } catch (Error e) {
      verificationErrors.append("Should show the new dataset.").append(e.getMessage()).append("/n");
    }
  }
  
  @Test
  public void createDir() {
    ProjectHelper.gotoProject(driver, project);
    DatasetHelper.gotoDatasetPage(driver);
    String newDirName = DatasetHelper.createSubDir(driver, dbHelper, project);
    By newDatasetID = By.id("table-row-" + newDirName);
    WebElement row = Helpers.waitForElement(newDatasetID, driver);
    List<WebElement> cells = row.findElements(By.tagName("td"));
    String datasetName = newDirName.length() > 170 ? newDirName.substring(0, 170) + "..." : newDirName;
    try {
      assertEquals(datasetName, cells.get(2).getText());
    } catch (Error e) {
      verificationErrors.append("Should show the new dir.").append(e.getMessage()).append("/n");
    }
  }
  
  @Test
  public void uploadFile() {
    ProjectHelper.gotoProject(driver, project);
    DatasetHelper.gotoDatasetPage(driver);
    Dataset dataset = DatasetHelper.createNewDataset(driver, dbHelper, project);
    String fileName = DatasetHelper.upload(driver, dataset);
    By newDatasetID = By.id("table-row-" + fileName);
    WebElement row = driver.findElement(newDatasetID);
    List<WebElement> cells = row.findElements(By.tagName("td"));
    String datasetName = fileName.length() > 170 ? fileName.substring(0, 170) + "..." : fileName;
    try {
      assertEquals(datasetName, cells.get(2).getText());
    } catch (Error e) {
      verificationErrors.append("Should show the new dir.").append(e.getMessage()).append("/n");
    }
  }
  
  @Test
  public void attachDetachMetadata() {
    ProjectHelper.gotoProject(driver, project);
    DatasetHelper.gotoDatasetPage(driver);
    String name = DatasetHelper.uploadMetadata(driver);
    String[] parts = name.split("\\.");
    String template = parts[0];
    WebElement row = driver.findElement(By.id("template-" + parts[0]));
    String templateName = template.length() > 30 ? template.substring(0, 30) + "..." : template;
    try {
      assertEquals(templateName, row.getText());
    } catch (Error e) {
      verificationErrors.append("Should show the new template.").append(e.getMessage()).append("/n");
    }
    DatasetHelper.gotoDatasetPage(driver);
    Dataset dataset = DatasetHelper.createNewDataset(driver, dbHelper, project);
    DatasetHelper.attachMetadata(driver, dataset, template);
    DatasetHelper.selectDataset(driver, dataset.getName());
    By exMd = By.id("extended_metadata");
    WebElement metadata = Helpers.waitForElementVisibility(exMd, driver);
    try {
      assertEquals(metadata.isEnabled(), true);
    } catch (Error e) {
      verificationErrors.append("Should show extended metadata.").append(e.getMessage()).append("/n");
    }
    DatasetHelper.detachMetadata(driver, dataset, template);
    By noExMd = By.id("no_extended_metadata");
    WebElement noMetadata = Helpers.waitForElementVisibility(noExMd, driver);
    try {
      assertEquals(noMetadata.isEnabled(), true);
    } catch (Error e) {
      verificationErrors.append("Should show no extended metadata.").append(e.getMessage()).append("/n");
    }
  }
  
  @Test
  public void copyAndMove() {
    ProjectHelper.gotoProject(driver, project);
    DatasetHelper.gotoDatasetPage(driver);
    Dataset dataset1 = DatasetHelper.createNewDataset(driver, dbHelper, project, false);
    DatasetHelper.waitForDataset(driver, dataset1.getName());
    Helpers.pause(driver, 5);
    Dataset dataset2 = DatasetHelper.createNewDataset(driver, dbHelper, project, false);
    DatasetHelper.waitForDataset(driver, dataset2.getName());
    Helpers.pause(driver, 5);
    Dataset dataset3 = DatasetHelper.createNewDataset(driver, dbHelper, project, false);
    DatasetHelper.waitForDataset(driver, dataset3.getName());
    Helpers.pause(driver, 5);
    File[] files = DatasetHelper.uploadFolder(driver, dataset1);
    DatasetHelper.selectAllFolders(driver);
    DatasetHelper.copySelectedTo(driver, dataset2);
    DatasetHelper.back(driver);
    DatasetHelper.gotoDataset(driver, dataset2);
    for (File file : files) {
      try {
        driver.findElement(By.id("table-row-" + file.getName()));
      } catch (Error e) {
        verificationErrors.append("Should copy files.").append(e.getMessage()).append("/n");
      }
    }
    DatasetHelper.selectAllFolders(driver);
    DatasetHelper.moveSelectedTo(driver, dataset3);
    for (File file : files) {
      try {
        driver.findElement(By.id("table-row-" + file.getName()));
        verificationErrors.append("Should have moved all files. But found ").append(file.getName()).append("/n");
      } catch (NoSuchElementException e) {
      }
    }
    DatasetHelper.back(driver);
    DatasetHelper.gotoDataset(driver, dataset3);
    Helpers.pause(driver, 10);
    for (File file : files) {
      try {
        driver.findElement(By.id("table-row-" + file.getName()));
      } catch (Error e) {
        verificationErrors.append("Should have moved files.").append(e.getMessage()).append("/n");
      }
    }
  }
  
  @Test
  public void shareAndUnshare() {
    Helpers.driverGet("hopsworks", driver);
    Project project1 = ProjectHelper.createProject(driver, dbHelper);
    Helpers.clickWithRetry(driver, By.id(project.getName()));
    DatasetHelper.gotoDatasetPage(driver);
    Dataset dataset = DatasetHelper.createNewDataset(driver, dbHelper, project);
    DatasetHelper.share(driver, dataset, project1, true);
    ProjectHelper.gotoProject(driver, project1);
    DatasetHelper.gotoDatasetPage(driver);
    String sharedName = project.getName() + "::" +dataset.getName();
    DatasetHelper.waitForDataset(driver, sharedName);
    try {
      driver.findElement(By.id("table-row-" + sharedName));
    } catch (Error e) {
      verificationErrors.append("Should share dataset.").append(e.getMessage()).append("/n");
    }
    Helpers.driverGet("hopsworks", driver);
    Helpers.clickWithRetry(driver, By.id(project.getName()));
    DatasetHelper.gotoDatasetPage(driver);
    DatasetHelper.unshare(driver, dataset, project1, true);
    ProjectHelper.gotoProject(driver, project1);
    DatasetHelper.gotoDatasetPage(driver);
    try {
      driver.findElement(By.id("table-row-" + sharedName));
      verificationErrors.append("Should have unshared the dataset ").append(sharedName).append("/n");
    } catch (NoSuchElementException e) {
    }
  }
  
  @Test
  public void deleteDataset() {
    ProjectHelper.gotoProject(driver, project);
    DatasetHelper.gotoDatasetPage(driver);
    Dataset dataset = DatasetHelper.createNewDataset(driver, dbHelper, project);
    String subDir = DatasetHelper.createSubDir(driver, dataset);
    DatasetHelper.gotoDir(driver, subDir);
    File[] files = DatasetHelper.uploadFolder(driver);
    Helpers.pause(driver, 10);
    DatasetHelper.selectAllFolders(driver);
    Helpers.pause(driver, 10);
    DatasetHelper.deleteSelected(driver);
    for (File file : files) {
      try {
        driver.findElement(By.id("table-row-" + file.getName()));
        verificationErrors.append("Should have deleted all files. But found ").append(file.getName()).append("/n");
      } catch (NoSuchElementException e) {
      }
    }
    DatasetHelper.back(driver);
    DatasetHelper.datasetShortCutMenu(driver, subDir, "remove", null, false);
    Helpers.modalFooterClick(driver, "OK");
    Helpers.waitForModal(driver, 10);
    try {
      driver.findElement(By.id("table-row-" + subDir));
      verificationErrors.append("Should have deleted dir. But found ").append(subDir).append("/n");
    } catch (NoSuchElementException e) {
    }
    DatasetHelper.back(driver);
    DatasetHelper.datasetShortCutMenu(driver, dataset, "remove", null);
    Helpers.modalFooterClick(driver, "OK");
    Helpers.waitForModal(driver, 10);
    try {
      driver.findElement(By.id("table-row-" + dataset.getName()));
      verificationErrors.append("Should have deleted dataset. But found ").append(dataset.getName()).append("/n");
    } catch (NoSuchElementException e) {
    }
  }
  
  @Test
  public void renameDir() {
    ProjectHelper.gotoProject(driver, project);
    DatasetHelper.gotoDatasetPage(driver);
    Dataset dataset = DatasetHelper.createNewDataset(driver, dbHelper, project);
    String subDir = DatasetHelper.createSubDir(driver, dataset);
    DatasetHelper.datasetShortCutMenu(driver, subDir, "rename", null, false);
    String newName = "renamed";
    WebElement newNameInput = driver.findElement(By.id("newfield_name"));
    newNameInput.clear();
    newNameInput.sendKeys(newName);
    Helpers.modalFooterClick(driver, "Rename");
    Helpers.waitForModal(driver, 10);
    try {
      driver.findElement(By.id("table-row-" + newName));
    } catch (Error e) {
      verificationErrors.append("Should rename dir.").append(e.getMessage()).append("/n");
    }
  }
  
  @Test
  public void changePermission() {
    ProjectHelper.gotoProject(driver, project);
    DatasetHelper.gotoDatasetPage(driver);
    Dataset dataset = DatasetHelper.createNewDataset(driver, dbHelper, project);
    String permission = "Group writable";
    DatasetHelper.changePermission(driver, dataset, permission);
    Helpers.pause(driver, 5);
    DatasetHelper.selectDataset(driver, dataset.getName());
    try {
      driver.findElement(By.id("sidenav_table")).findElements(By.tagName("td")).stream()
        .filter(i -> i.getText().equals(permission + ".")).findFirst().get();
    } catch (NoSuchElementException e) {
      verificationErrors.append("Should change permission.").append(e.getMessage()).append("/n");
    }
  }
  
  @AfterClass
  public static void tearDown() {
    if (driver != null) {
      driver.quit();
    }
    if (dbHelper != null) {
      dbHelper.closeConnection();
    }
    String verificationErrorString = verificationErrors.toString();
    if (!"".equals(verificationErrorString)) {
      fail(verificationErrorString);
    }
    LOGGER.log(Level.INFO, "Finished dataset test.");
  }
}
