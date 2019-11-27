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

import io.hops.hopsworks.util.DBHelper;
import io.hops.hopsworks.util.Helpers;
import io.hops.hopsworks.util.models.User;
import io.hops.hopsworks.util.helpers.RegistrationHelper;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.concurrent.TimeUnit;

public class LoginIT {

  private WebDriver driver;
  private DBHelper dbHelper;
  private final StringBuffer verificationErrors = new StringBuffer();
  
  private final static By LOGIN_FORM = By.name("loginForm");
  private final static By LOGIN_INPUT_EMAIL = By.id("login_inputEmail");
  private final static By LOGIN_INPUT_PWD = By.id("login_inputPassword");
  private final static By LOGIN_INPUT_OTP = By.id("login_inputOTP");
  private final static By USER_PROFILE = By.id("navbarProfile");
  
  private User user;
  private String password = "12345Ab";

  @Before
  public void startUp() {
    driver = WebDriverFactory.getWebDriver();
    dbHelper = new DBHelper();
  }
  
  private void createUser() {
    user = RegistrationHelper.createActivatedUser(password, false, driver, dbHelper);
  }
  
  @Test
  public void testPasswordTypeToggle() {
    By xpath = By.xpath("(.//input[@id='login_inputPassword'])/following::span");
    driver.findElement(LOGIN_INPUT_PWD).click();
    driver.findElement(LOGIN_INPUT_PWD).clear();
    driver.findElement(LOGIN_INPUT_PWD).sendKeys("1234 is not a good password!");
    Helpers.testTogglePassword(LOGIN_INPUT_PWD, xpath, verificationErrors, driver);
  }
  
  @Test
  public void firstLoginTest() {
    createUser();
    driver.findElement(LOGIN_INPUT_EMAIL).clear();
    driver.findElement(LOGIN_INPUT_EMAIL).sendKeys(user.getEmail());
    driver.findElement(LOGIN_INPUT_PWD).clear();
    driver.findElement(LOGIN_INPUT_PWD).sendKeys(password);
    driver.findElement(LOGIN_FORM).submit();
    WebElement content = null;
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
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
    driver.manage().timeouts().implicitlyWait(WebDriverFactory.IMPLICIT_WAIT_TIMEOUT, TimeUnit.SECONDS);
  }
  
  @Test
  public void loginTest() {
    createUser();
    driver.findElement(LOGIN_INPUT_EMAIL).clear();
    driver.findElement(LOGIN_INPUT_EMAIL).sendKeys(user.getEmail());
    driver.findElement(LOGIN_INPUT_PWD).clear();
    driver.findElement(LOGIN_INPUT_PWD).sendKeys(password);
    driver.findElement(LOGIN_FORM).submit();
    try {
      assertEquals(user.getEmail(), driver.findElement(USER_PROFILE).getText());
    } catch (Error e) {
      verificationErrors.append("Should login user and show email on navbar.").append(e.getMessage());
    }
  }
  
  @Test
  public void loginGrowlTest() {
    createUser();
    driver.findElement(LOGIN_INPUT_EMAIL).clear();
    driver.findElement(LOGIN_INPUT_EMAIL).sendKeys(user.getEmail());
    driver.findElement(LOGIN_INPUT_PWD).clear();
    driver.findElement(LOGIN_INPUT_PWD).sendKeys(password + "1");
    driver.findElement(LOGIN_FORM).submit();
    driver.findElement(LOGIN_INPUT_PWD).clear();
    driver.findElement(LOGIN_INPUT_PWD).sendKeys(password);
    driver.findElement(LOGIN_FORM).submit();
    WebElement content = null;
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
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
    driver.manage().timeouts().implicitlyWait(WebDriverFactory.IMPLICIT_WAIT_TIMEOUT, TimeUnit.SECONDS);
  }
  
  @Test
  public void loginTwoFactorTest() {
    User u = RegistrationHelper.createActivatedUser(password, true, driver, dbHelper);
    driver.findElement(LOGIN_INPUT_EMAIL).clear();
    driver.findElement(LOGIN_INPUT_EMAIL).sendKeys(u.getEmail());
    driver.findElement(LOGIN_INPUT_PWD).clear();
    driver.findElement(LOGIN_INPUT_PWD).sendKeys(password);
    driver.findElement(LOGIN_FORM).submit();
    By by = By.xpath("(.//div[@id='second-factor-auth'])//h3");
    Helpers.assertEqualsElementText("2-Step Verification", by, verificationErrors, driver);
    driver.findElement(LOGIN_INPUT_OTP).clear();
    driver.findElement(LOGIN_INPUT_OTP).sendKeys("12345");
    driver.findElement(LOGIN_FORM).submit();
    By error = By.id("second-factor-error");
    By errorClose = By.id("second-factor-error-close");
    Helpers
      .assertEqualsElementText("An argument was not provided or it was malformed.", error, verificationErrors, driver);
    driver.findElement(LOGIN_INPUT_OTP).clear();
    driver.findElement(errorClose).click();
    driver.findElement(LOGIN_INPUT_OTP).sendKeys("123456");
    driver.findElement(LOGIN_FORM).submit();
    Helpers.assertEqualsElementText("Authentication failed, invalid credentials", error, verificationErrors, driver);
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
  }

}
