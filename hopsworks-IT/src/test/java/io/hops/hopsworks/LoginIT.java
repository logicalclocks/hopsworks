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
import org.openqa.selenium.WebDriver;

public class LoginIT {

  private WebDriver driver;
  private final StringBuffer verificationErrors = new StringBuffer();

  @Before
  public void startUp() {
    driver = WebDriverFactory.getWebDriver();
  }

  @Test
  public void loginTest() throws Exception {
    driver.findElement(By.id("login_inputEmail")).clear();
    driver.findElement(By.id("login_inputEmail")).sendKeys("admin@kth.se");
    driver.findElement(By.id("login_inputPassword")).sendKeys("admin");
    driver.findElement(By.name("loginForm")).submit();
    try {
      assertEquals("admin@kth.se", driver.findElement(By.id("navbarProfile")).getText());
    } catch (Error e) {
      verificationErrors.append("Should login user and show email on navbar.").append(e.getMessage());
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
