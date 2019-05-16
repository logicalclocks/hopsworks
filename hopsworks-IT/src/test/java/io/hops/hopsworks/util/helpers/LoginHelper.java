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
import io.hops.hopsworks.util.models.User;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;

public class LoginHelper {
  
  public static final String DEFAULT_ADMIN_USER = "admin@hopsworks.ai";
  public static final String DEFAULT_ADMIN_PWD = "admin";
  
  private static User adminUser;
  private static final String ADMIN_PW = "12345Ab";
  private static User user;
  private static final String USER_PW = "12345Ab";
  
  public static User loginAsAdmin(WebDriver driver, DBHelper dbHelper) {
    if (adminUser != null) {
      login(adminUser.getEmail(), ADMIN_PW, driver);
      return adminUser;
    }
    adminUser = RegistrationHelper.createAdminUser(ADMIN_PW, driver, dbHelper);
    login(adminUser.getEmail(), ADMIN_PW, driver);
    return adminUser;
  }
  
  public static User loginAsUser(WebDriver driver, DBHelper dbHelper) {
    if (user != null) {
      login(user.getEmail(), USER_PW, driver);
      return user;
    }
    user = RegistrationHelper.createActivatedUser(USER_PW, false, driver, dbHelper);
    login(user.getEmail(), USER_PW, driver);
    return user;
  }
  
  public static void login(String username, String password, WebDriver driver) {
    WebElement loginForm = driver.findElement(By.name("loginForm"));
    driver.findElement(By.id("login_inputEmail")).clear();
    driver.findElement(By.id("login_inputEmail")).sendKeys(username);
    driver.findElement(By.id("login_inputPassword")).sendKeys(password);
    loginForm.submit();
    assertEquals(username, driver.findElement(By.id("navbarProfile")).getText());
  }
  
  public static void logout(WebDriver driver) {
    if (driver.findElement(By.id("navbarProfile")) != null) {
      driver.findElement(By.id("navbarProfile")).click();
      driver.findElement(By.linkText("Sign out")).click();
    }
  }
}
