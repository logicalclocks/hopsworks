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
import io.hops.hopsworks.util.models.Project;
import io.hops.hopsworks.util.models.ProjectServices;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class ProjectHelper {
  private static final Logger LOGGER = Logger.getLogger(ProjectHelper.class.getName());
  public final static By NEW_PROJECT_BTN = By.id("buttonCreateProject");
  public final static By NEW_PROJECT_NAME = By.name("project_name");
  public final static By NEW_PROJECT_FORM = By.name("projectForm");
  public final static By PROJECT_WORKING = By.id("project_nav_title");
  public final static By PROJECT_NAV_TITLE = By.id("project_nav_title");
  
  public static String getRandomName() {
    Random rand = new Random();
    String projectName = "project" + rand.nextInt(100000);
    return projectName;
  }
  
  public static Project newProject(WebDriver driver, DBHelper dbHelper, List<ProjectServices> disableServices) {
    String projectName = getRandomName();
    WebElement createBtn = Helpers.waitForElementToBeClickable(NEW_PROJECT_BTN, driver);
    createBtn.click();
    driver.findElement(NEW_PROJECT_NAME).clear();
    driver.findElement(NEW_PROJECT_NAME).sendKeys(projectName);
    for (ProjectServices service: disableServices) {
      driver.findElement(By.id(service.name())).click();
    }
    driver.findElement(NEW_PROJECT_FORM).submit();
    Helpers.waitForModal(driver, 240);
    Project project = dbHelper.getProjectByName(projectName);
    Helpers.pause(driver, 10);
    return project;
  }
  
  public static Project createAndGotoProject(WebDriver driver, DBHelper dbHelper) {
    Project project = createProject(driver, dbHelper);
    Helpers.clickWithRetry(driver, By.id(project.getName()));
    return project;
  }
  
  public static Project createProject(WebDriver driver, DBHelper dbHelper) {
    List<ProjectServices> disable = new ArrayList<>();
    disable.add(ProjectServices.HIVE);
    disable.add(ProjectServices.FEATURESTORE);
    disable.add(ProjectServices.AIRFLOW);
    disable.add(ProjectServices.SERVING);
    Project project = newProject(driver, dbHelper, disable);
    return project;
  }
  
  public static void deleteProject(WebDriver driver, String projectName) {
    Actions action = new Actions(driver);
    action.contextClick(driver.findElement(By.id(projectName))).perform();
    driver.findElement(By.linkText("Remove and delete DataSets")).click();
    Helpers.waitForElementInvisibility(PROJECT_WORKING, driver);
  }
  
  public static void gotoProject(WebDriver driver, Project project) {
    Helpers.driverGet("hopsworks", driver);
    Helpers.clickWithRetry(driver, By.id(project.getName()));
  }
}
