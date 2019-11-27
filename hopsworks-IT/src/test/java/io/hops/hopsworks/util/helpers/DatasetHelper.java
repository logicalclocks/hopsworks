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
package io.hops.hopsworks.util.helpers;

import io.hops.hopsworks.util.DBHelper;
import io.hops.hopsworks.util.Helpers;
import io.hops.hopsworks.util.models.Dataset;
import io.hops.hopsworks.util.models.Project;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Random;
import java.util.logging.Logger;

public class DatasetHelper {
  private static final Logger LOGGER = Logger.getLogger(DatasetHelper.class.getName());
  public final static By DATASETS_BTN = By.id("datasetsTab");
  public final static By METADATA_TAB = By.id("metadataTab");
  public final static By CREATE_DATASET = By.id("creat_dataset");
  public final static By CREATE_NEW_DATASET = By.id("creat_new_dataset");
  public final static By DATASET_NAME = By.id("dataSetName");
  public final static By NEW_DATASET_FORM = By.name("dataSetForm");
  public final static By GENERATE_README = By.id("generate_readme");
  public final static By SELECT_ALL_FOLDERS = By.id("select_all");
  
  public final static By TOOLBAR_BTN_BACK = By.id("toolbar-back");
  public final static By TOOLBAR_BTN_NEW = By.id("toolbar-new-folder");
  public final static By TOOLBAR_BTN_UPLOAD = By.id("toolbar-upload");
  public final static By TOOLBAR_BTN_COPY = By.id("toolbar-copy");
  public final static By TOOLBAR_BTN_MOVE = By.id("toolbar-move");
  public final static By TOOLBAR_BTN_RENAME = By.id("toolbar-rename");
  public final static By TOOLBAR_BTN_DELETE = By.id("toolbar-delete");
  public final static By TOOLBAR_BTN_OPEN = By.id("toolbar-open");
  public final static By TOOLBAR_BTN_PREVIEW = By.id("toolbar-preview");
  public final static By TOOLBAR_BTN_UNZIP = By.id("toolbar-unzip");
  public final static By TOOLBAR_BTN_ZIP = By.id("toolbar-zip");
  public final static By TOOLBAR_BTN_CONVERT = By.id("toolbar-convert");
  
  public final static By SELECT_DIR_TABLE = By.id("select_dir");
  public final static By SELECT_FILE_TABLE = By.id("select_file");
  public final static By SELECT_DIR_LOADING = By.id("select_dir_loading");
  public final static By SELECT_FILE_LOADING = By.id("select_file_loading");
  
  public final static By UPLOAD_METADATA = By.id("upload-metadata");
  public final static By UPLOAD_BTN = By.id("file-upload");
  public final static By UPLOAD_BTN_FOLDER = By.id("folder-upload");
  public final static By UPLOAD_ALL = By.id("upload-all");
  public final static By UPLOAD_SIZE = By.id("upload-size");
  public final static By PAUSE_ALL = By.id("pause-all");
  public final static By PAUSE_FILE_UPLOAD = By.id("pause-file-upload");
  public final static By RESUME_FILE_UPLOAD = By.id("resume-file-upload");
  public final static By RETRY_FILE_UPLOAD = By.id("retry-file-upload");
  
  public final static String METADATA_DIR = "../tools/metadata_designer";
  
  public static String getRandomName(String prefix) {
    Random rand = new Random();
    String datasetName = prefix + "_" + rand.nextInt(100000);
    return datasetName;
  }
  
  public static void gotoDatasetPage(WebDriver driver) {
    Helpers.waitForElementToBeClickable(DATASETS_BTN, driver);
    driver.findElement(DATASETS_BTN).click();
    Helpers.pause(driver, 15); //wait to load
  }
  
  public static By waitForDataset(WebDriver driver, String datasetName) {
    By newDatasetID = By.id("table-row-" + datasetName);
    Helpers.waitForElementToBeClickable(newDatasetID, driver);
    Helpers.pause(driver, 2);
    return newDatasetID;
  }
  
  public static void gotoDataset(WebDriver driver, Dataset dataset) {
    gotoDir(driver, dataset.getName());
  }
  
  public static void gotoDir(WebDriver driver, String datasetName) {
    By newDatasetID = waitForDataset(driver, datasetName);
    Helpers.clickWithRetry(driver, newDatasetID);
    Helpers.pause(driver, 5);
  }
  
  public static Dataset createNewDataset(WebDriver driver, DBHelper dbHelper, Project project) {
    return createNewDataset(driver, dbHelper, project, true);
  }
  
  public static Dataset createNewDataset(WebDriver driver, DBHelper dbHelper, Project project, boolean readme) {
    String name = getRandomName("dataset");
    Helpers.clickWithRetry(driver, CREATE_DATASET);
    WebElement datasetName = Helpers.waitForElementVisibility(DATASET_NAME, driver, 120);
    datasetName.clear();
    datasetName.sendKeys(name);
    if (!readme) {
      driver.findElement(GENERATE_README).findElement(By.className("md-container")).click();
    }
    driver.findElement(NEW_DATASET_FORM).submit();
    Helpers.waitForModal(driver, 60);
    Helpers.pause(driver, 5);
    return dbHelper.getDatasetByName(project.getId(), name);
  }
  
  public static void createAndGotoDataset(WebDriver driver, DBHelper dbHelper, Project project) {
    Dataset dataset = createNewDataset(driver, dbHelper, project);
    gotoDataset(driver, dataset);
  }
  
  public static String createSubDir(WebDriver driver, DBHelper dbHelper, Project project) {
    Dataset dataset = createNewDataset(driver, dbHelper, project);
    return createSubDir(driver, dataset);
  }
  
  public static String createSubDir(WebDriver driver, Dataset dataset) {
    gotoDataset(driver, dataset);
    String name = getRandomName("folder");
    Helpers.clickWithRetry(driver, TOOLBAR_BTN_NEW);
    WebElement datasetName = Helpers.waitForElementVisibility(DATASET_NAME, driver);
    datasetName.clear();
    datasetName.sendKeys(name);
    driver.findElement(NEW_DATASET_FORM).submit();
    Helpers.waitForModal(driver, 60);
    Helpers.pause(driver, 2);
    return name;
  }
  
  public static void selectDataset(WebDriver driver, String name) {
    WebElement checkbox = driver.findElement(By.id("table-row-" + name)).findElements(By.tagName("td"))
      .get(0).findElement(By.tagName("md-checkbox")).findElement(By.className("md-container"));
    checkbox.click();
  }
  
  public static void selectAllFolders(WebDriver driver) {
    driver.findElement(SELECT_ALL_FOLDERS).findElement(By.className("md-container")).click();
    Helpers.pause(driver, 10);
  }
  
  public static String uploadFile(WebDriver driver, File file) {
    WebElement uploadButtons = driver.findElement(UPLOAD_BTN);
    String fileStr = "";
    try {
      fileStr = file.getCanonicalPath();
    } catch (IOException e) {
    }
    uploadButtons.findElement(By.tagName("input")).sendKeys(fileStr);
    Helpers.waitForElementVisibility(RESUME_FILE_UPLOAD, driver);
    driver.findElement(RESUME_FILE_UPLOAD).click();
    Helpers.waitForModal(driver, 60);
    Helpers.pause(driver, 2);
    return file.getName();
  }
  
  public static String uploadFolder(WebDriver driver, File file) {
    WebElement uploadButtons = driver.findElement(UPLOAD_BTN_FOLDER);
    String fileStr = "";
    try {
      fileStr = file.getCanonicalPath();
    } catch (IOException e) {
    }
    uploadButtons.findElement(By.tagName("input")).sendKeys(fileStr);
    Helpers.waitForElementVisibility(UPLOAD_SIZE, driver);
    driver.findElement(UPLOAD_ALL).click();
    Helpers.pause(driver, 10);
    Helpers.waitForModal(driver, 120);
    Helpers.pause(driver, 5);
    return file.getName();
  }
  
  public static File[] uploadFolder(WebDriver driver, Dataset dataset) {
    gotoDataset(driver, dataset);
    return uploadFolder(driver);
  }
  
  public static File[] uploadFolder(WebDriver driver) {
    driver.findElement(TOOLBAR_BTN_UPLOAD).click();
    Helpers.waitForElementVisibility(UPLOAD_BTN, driver);
    File file = new File(METADATA_DIR);
    uploadFolder(driver, file);
    return file.listFiles();
  }
  
  public static String upload(WebDriver driver, Dataset dataset) {
    gotoDataset(driver, dataset);
    driver.findElement(TOOLBAR_BTN_UPLOAD).click();
    Helpers.waitForElementVisibility(UPLOAD_BTN, driver);
    File file = new File(METADATA_DIR + "/Sample.json");
    return uploadFile(driver, file);
  }
  
  public static String uploadMetadata(WebDriver driver) {
    Helpers.waitForElementToBeClickable(METADATA_TAB, driver);
    driver.findElement(METADATA_TAB).click();
    Helpers.waitForElement(UPLOAD_METADATA, driver);
    driver.findElement(UPLOAD_METADATA).click();
    Helpers.waitForElementVisibility(UPLOAD_BTN, driver);
    File file = new File(METADATA_DIR + "/Study.json");
    WebElement uploadButtons = driver.findElement(UPLOAD_BTN);
    String fileStr = "";
    try {
      fileStr = file.getCanonicalPath();
    } catch (IOException e) {
    }
    uploadButtons.findElement(By.tagName("input")).sendKeys(fileStr);
    try {
      Helpers.waitForModal(driver, 60);
    } catch (TimeoutException te) {
      WebElement retry = driver.findElement(By.id("retry_template"));
      if (retry.isEnabled()) {
        driver.findElement(By.className("modal-footer")).findElement(By.tagName("button")).click();
      }
      Helpers.waitForModal(driver, 60);
    }
    Helpers.pause(driver, 2);
    return file.getName();
  }
  
  public static void datasetShortCutMenu(WebDriver driver, Dataset dataset, String menu, String subMenu) {
    datasetShortCutMenu(driver, dataset.getName(), menu, subMenu, true);
  }
  
  public static void datasetShortCutMenu(WebDriver driver, String name, String menu, String subMenu, boolean dataset) {
    By newDatasetID = waitForDataset(driver, name);
    Actions action = new Actions(driver);
    WebElement datasetRow =
      driver.findElement(newDatasetID).findElements(By.tagName("td")).get(1).findElement(By.tagName("div"));
    action.contextClick(datasetRow).perform();
    String menuId = dataset? "table-menu-" : "menu-";
    By datasetDropdown = By.id(menuId + name);
    WebElement dropdownMenu = driver.findElement(datasetDropdown).findElement(By.className("dropdown-menu"));
    WebDriverWait wait = new WebDriverWait(driver, 10);
    WebElement element =
      wait.pollingEvery(Duration.ofSeconds(1)).until(ExpectedConditions.elementToBeClickable(dropdownMenu));
    WebElement menuToSelect = element.findElement(By.id(menu));
    wait = new WebDriverWait(driver, 10);
    wait.until(ExpectedConditions.elementToBeClickable(menuToSelect));
    menuToSelect.click();
    if (subMenu != null && !subMenu.isEmpty()) {
      menuToSelect.findElements(By.xpath("//ul/li/a/span")).stream().filter(i -> i.getText().equals(subMenu))
        .findFirst().get().click();
    }
  }
  
  public static void selectDropdown(WebDriver driver, String dropdownId, String value) {
    WebElement templateSelect = driver.findElement(By.id(dropdownId));
    templateSelect.click();
    WebElement searchBox = Helpers.waitForElementVisibility(By.className("ui-select-search"), driver);
    searchBox.sendKeys(value);
    Helpers.pause(driver, 10);
    Helpers.waitForElementInvisibility(By.className("loading-dots-sm"), driver);
    templateSelect.findElements(By.tagName("span")).stream().filter(i -> i.getText().equals(value)).findAny().get()
      .click();
  }
  
  public static void attachMetadata(WebDriver driver, Dataset dataset, String template) {
    datasetShortCutMenu(driver, dataset, "add_metadata", null);
    selectDropdown(driver, "template_select", template);
    Helpers.modalFooterClick(driver, "OK");
    Helpers.waitForModal(driver, 60);
    Helpers.pause(driver, 5);
  }
  
  public static void detachMetadata(WebDriver driver, Dataset dataset, String template) {
    datasetShortCutMenu(driver, dataset, "remove_metadata", null);
    WebElement templateSelect = driver.findElement(By.id("template_select"));
    templateSelect.click();
    templateSelect.findElements(By.tagName("span")).stream().filter(i -> i.getText().endsWith(template)).findAny().get()
      .click();
    Helpers.modalFooterClick(driver, "Remove template");
    Helpers.waitForModal(driver, 60);
    Helpers.pause(driver, 5);
  }
  
  public static void deleteSelected(WebDriver driver) {
    Helpers.clickWithRetry(driver, TOOLBAR_BTN_DELETE);
    Helpers.modalFooterClick(driver, "OK");
    Helpers.waitForModal(driver, 10);
  }
  
  public static void copySelectedTo(WebDriver driver, Dataset dataset) {
    Helpers.clickWithRetry(driver, TOOLBAR_BTN_COPY);
    selectDir(driver, dataset.getName());
  }
  
  public static void moveSelectedTo(WebDriver driver, Dataset dataset) {
    Helpers.clickWithRetry(driver, TOOLBAR_BTN_MOVE);
    selectDir(driver, dataset.getName());
  }
  
  public static void selectDir(WebDriver driver, String name) {
    WebElement selectDir = Helpers.waitForElementVisibility(SELECT_DIR_TABLE, driver);
    Helpers.waitForElementInvisibility(SELECT_DIR_LOADING, driver);
    Helpers.pause(driver, 10);//load files
    By rowId = By.id("row-" + name);
    Helpers.waitForElementVisibility(rowId, driver);
    selectDir.findElement(rowId);
    Helpers.pause(driver, 10);
    Helpers.modalFooterClick(driver, "Select");
    Helpers.waitForModal(driver, 60);
    Helpers.pause(driver, 5);
  }
  
  public static void back(WebDriver driver) {
    driver.findElement(TOOLBAR_BTN_BACK).click();
    Helpers.pause(driver, 5);
  }
  
  public static void share(WebDriver driver, Dataset dataset, Project project, boolean accepted) {
    datasetShortCutMenu(driver, dataset, "share_with", "Project");
    selectDropdown(driver, "project_select", project.getName());
    Helpers.modalFooterClick(driver, "Share");
  }
  
  public static void unshare(WebDriver driver, Dataset dataset, Project project, boolean accepted) {
    datasetShortCutMenu(driver, dataset, "unshare_from", "Project");
    selectDropdown(driver, "project_select", project.getName());
    Helpers.modalFooterClick(driver, "Unshare");
  }
  
  public static void changePermission(WebDriver driver, Dataset dataset, String permission) {
    datasetShortCutMenu(driver, dataset, "permissions", permission);
    Helpers.modalFooterClick(driver, "Yes");
    Helpers.waitForModal(driver, 120);
  }
}
