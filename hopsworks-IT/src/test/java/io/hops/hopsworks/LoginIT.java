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
package io.hops.hopsworks;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LoginIT {

  private WebDriver driver;
  private final StringBuffer verificationErrors = new StringBuffer();

  @Before
  public void startUp() {
    driver = WebDriverFactory.getWebDriver();
  }
  
  @Test
  public void firstLoginTest() {
    driver.findElement(By.id("login_inputEmail")).clear();
    driver.findElement(By.id("login_inputEmail")).sendKeys("admin@hopsworks.ai");
    driver.findElement(By.id("login_inputPassword")).clear();
    driver.findElement(By.id("login_inputPassword")).sendKeys("admin");
    driver.findElement(By.name("loginForm")).submit();
    WebElement content = null;
    try {
      content = driver.findElement(By.className("growl-title"));
    } catch (NoSuchElementException nse) {
    }
    try {
      assertEquals(content, null);
    } catch (Error e) {
      verificationErrors.append("Should not show growl on first login. But showed growl with msg: ")
        .append(content.getText())
        .append(e.getMessage());
    }
  }
  
  @Test
  public void loginTest() {
    driver.findElement(By.id("login_inputEmail")).clear();
    driver.findElement(By.id("login_inputEmail")).sendKeys("admin@hopsworks.ai");
    driver.findElement(By.id("login_inputPassword")).clear();
    driver.findElement(By.id("login_inputPassword")).sendKeys("admin");
    driver.findElement(By.name("loginForm")).submit();
    try {
      assertEquals("admin@hopsworks.ai", driver.findElement(By.id("navbarProfile")).getText());
    } catch (Error e) {
      verificationErrors.append("Should login user and show email on navbar.").append(e.getMessage());
    }
  }
  
  @Test
  public void loginGrowlTest() {
    driver.findElement(By.id("login_inputEmail")).clear();
    driver.findElement(By.id("login_inputEmail")).sendKeys("admin@hopsworks.ai");
    driver.findElement(By.id("login_inputPassword")).clear();
    driver.findElement(By.id("login_inputPassword")).sendKeys("admin1");
    driver.findElement(By.name("loginForm")).submit();
    driver.findElement(By.id("login_inputPassword")).clear();
    driver.findElement(By.id("login_inputPassword")).sendKeys("admin");
    driver.findElement(By.name("loginForm")).submit();
    WebElement content = null;
    try {
      content = driver.findElement(By.className("growl-title"));
    } catch (NoSuchElementException nse) {
    }
  
    try {
      assertEquals(content, null);
    } catch (Error e) {
      verificationErrors.append("Should not show growl after login. But showed growl with msg: ")
        .append(content.getText())
        .append(e.getMessage());
    }
  }

  @After
  public void tearDown() {
    if (driver != null) {
      driver.quit();
    }
    String verificationErrorString = verificationErrors.toString();
    if (!"".equals(verificationErrorString)) {
      fail(verificationErrorString);
    }
  }

}
