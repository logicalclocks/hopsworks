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
package io.hops.hopsworks.project;

import io.hops.hopsworks.WebDriverFactory;
import io.hops.hopsworks.util.DBHelper;
import io.hops.hopsworks.util.Helpers;
import io.hops.hopsworks.util.helpers.ProjectHelper;
import io.hops.hopsworks.util.models.Project;
import io.hops.hopsworks.util.models.User;
import io.hops.hopsworks.util.helpers.LoginHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ProjectIT {
  private static final Logger LOGGER = Logger.getLogger(ProjectIT.class.getName());
  private WebDriver driver;
  private DBHelper dbHelper;
  private User user;
  private final StringBuffer verificationErrors = new StringBuffer();
  
  @Before
  public void startUp() {
    driver = WebDriverFactory.getWebDriver();
    dbHelper = new DBHelper();
    user = LoginHelper.loginAsUser(driver, dbHelper);
  }
  
  @Test
  public void createProject() {
    Project project = ProjectHelper.createProject(driver, dbHelper);
    WebElement button = driver.findElement(By.id(project.getName()));
    try {
      assertEquals(project.getName(), button.getText());
    } catch (Error e) {
      verificationErrors.append("Should show the new project.").append(e.getMessage()).append("/n");
    }
  }
  
  @Test
  public void deleteProject() {
    Project project = ProjectHelper.createProject(driver, dbHelper);
    ProjectHelper.deleteProject(driver, project.getName());
    WebElement button = null;
    try {
      button = driver.findElement(By.id(project.getName()));
    } catch (NoSuchElementException nse) {
    }
    try {
      assertEquals(button, null);
    } catch (Error e) {
      verificationErrors.append("Should remove the project.").append(e.getMessage()).append("/n");
    }
  }
  
  @Test
  public void gotoProject() {
    Project project = ProjectHelper.createAndGotoProject(driver, dbHelper);
    WebElement title = Helpers.waitForElementVisibility(ProjectHelper.PROJECT_NAV_TITLE, driver);
    String projectName = project.getName().length() > 17? project.getName().substring(0, 17) + "..." :
      project.getName();
    try {
      assertEquals(projectName, title.getText());
    } catch (Error e) {
      verificationErrors.append("Should show the new project in the project nav title.").append(e.getMessage())
        .append("/n");
    }
  }
  
  @After
  public void tearDown() {
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
    LOGGER.log(Level.INFO, "Finshed register test.");
  }
}
