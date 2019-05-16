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
import io.hops.hopsworks.util.JavascriptExec;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.hops.hopsworks.util.helpers.RegistrationHelper;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class RegisterIT {
  
  private static final Logger LOGGER = Logger.getLogger(RegisterIT.class.getName());
  private WebDriver driver;
  private DBHelper dbHelper;
  private final StringBuffer verificationErrors = new StringBuffer();
  
  @Before
  public void startUp() {
    driver = WebDriverFactory.getWebDriver();
    dbHelper = new DBHelper();
    driver.findElement(By.linkText("Register")).click();
    Helpers.waitForElement(By.name("registerForm"), driver);
  }
  
  
  private WebElement registerUser(boolean twoFactor) {
    RegistrationHelper.registerUserTest(twoFactor, driver, dbHelper);
    By by;
    if (twoFactor) {
      by = By.id("qr_code_panel");
    } else {
      by = By.xpath("//div[contains(@class, \"panel-body\")]/div[2]");
    }
    WebElement element = Helpers.waitForElementVisibility(by, driver);
    return element;
  }
  
  @Test
  public void testSecurityAnswerTypeToggle() {
    By element = By.name("sec_answer");
    By xpath = By.xpath("(.//input[@name='sec_answer'])/following::span");
    driver.findElement(element).click();
    driver.findElement(element).clear();
    driver.findElement(element).sendKeys("Some answer");
    Helpers.testTogglePassword(element, xpath, verificationErrors, driver);
  }
  
  @Test
  public void testEmailConstraint() {
    driver.findElement(By.name("user_email")).sendKeys("user1234@kth.");
    String attribute = driver.findElement(By.xpath("(.//input[@name='user_email'])/following::span[3]")).
      getAttribute("aria-hidden");
    LOGGER.log(Level.INFO, "Email Constraint. {0}", attribute);
    try {
      assertEquals("false", attribute);
    } catch (Error e) {
      verificationErrors.append("Should not accept invalid email address.").append(e.getMessage()).append("\n");
    }
    driver.findElement(By.name("user_email")).clear();
    driver.findElement(By.name("user_email")).sendKeys("user1234@kth.se");
    attribute = driver.findElement(By.xpath("(.//input[@name='user_email'])/following::span[3]")).
      getAttribute("aria-hidden");
    try {
      assertEquals("true", attribute);
    } catch (Error e) {
      verificationErrors.append("Should accept a valid email address.").append(e.getMessage()).append("\n");
    }
    driver.findElement(By.name("user_email")).clear();
  }
  
  @Test
  public void testPassword() {
    driver.findElement(By.id("id-password")).sendKeys("12345A");
    String attribute =
      Helpers.waitForElement(By.xpath("(.//input[@name='user_password'])/../../../following::div/span[3]"), driver).
        getAttribute("aria-hidden");
    LOGGER.log(Level.INFO, "invalid password. {0}", attribute);
    try {
      assertEquals("false", attribute);
    } catch (Error e) {
      verificationErrors.append("Should not accept invalid password.").append(e.getMessage()).append("\n");
    }
    driver.findElement(By.id("id-password")).clear();
    driver.findElement(By.id("id-password")).sendKeys("12345a");
    attribute =
      Helpers.waitForElement(By.xpath("(.//input[@name='user_password'])/../../../following::div/span[3]"), driver).
        getAttribute("aria-hidden");
    LOGGER.log(Level.INFO, "invalid password. {0}", attribute);
    try {
      assertEquals("false", attribute);
    } catch (Error e) {
      verificationErrors.append("Should not accept invalid password.").append(e.getMessage()).append("\n");
    }
    driver.findElement(By.id("id-password")).clear();
    driver.findElement(By.id("id-password")).sendKeys("12345Ab");
    attribute =
      Helpers.waitForElement(By.xpath("(.//input[@name='user_password'])/../../../following::div/span[3]"), driver).
        getAttribute("aria-hidden");
    try {
      assertEquals("true", attribute);
    } catch (Error e) {
      verificationErrors.append("Should accept valid password.").append(e.getMessage()).append("\n");
    }
    driver.findElement(By.id("id-password")).clear();
  }
  
  @Test
  public void testPasswordMatch() {
    driver.findElement(By.id("id-password")).sendKeys("12345Ab");
    driver.findElement(By.name("user_password_confirm")).sendKeys("12345A");
    String attribute = Helpers.waitForElement(By.xpath("(.//input[@name='user_password'])/../../." +
      "./following::div/span[4]"), driver).getAttribute("aria-hidden");
    try {
      assertEquals("false", attribute);
    } catch (Error e) {
      verificationErrors.append("Should not accept passwords that do not match.").append(e.getMessage()).append("\n");
    }
    driver.findElement(By.name("user_password_confirm")).clear();
    driver.findElement(By.name("user_password_confirm")).sendKeys("12345Ab");
    attribute = Helpers.waitForElement(By.xpath("(.//input[@name='user_password'])/../../../following::div/span[4]"),
      driver).getAttribute("aria-hidden");
    try {
      assertEquals("true", attribute);
    } catch (Error e) {
      verificationErrors.append("Should accept passwords that match.").append(e.getMessage()).append("\n");
    }
    driver.findElement(By.id("id-password")).clear();
    driver.findElement(By.name("user_password_confirm")).clear();
  }
  
  @Test
  public void testRegister() {
    WebElement element = registerUser(false);
    String attribute = Helpers.waitForElement(By.xpath("//div[contains(@class, \"panel-body\")]/div[3]"), driver)
      .getAttribute("aria-hidden");
    try {
      assertEquals("true", attribute);
    } catch (Error e) {
      verificationErrors.append("Should not show error message. ").append(e.getMessage()).append("\n");
    }
    attribute = element.getAttribute("aria-hidden");
    try {
      assertEquals("false", attribute);
    } catch (Error e) {
      verificationErrors.append("Should show success message. ").append(element.getText())
        .append(e.getMessage()).append("\n");
    }
  }
  
  @Test
  public void testRegisterTwoFactor() {
    registerUser(true);
    String URL = driver.getCurrentUrl();
    try {
      assertTrue(URL.contains("/qrCode"));
    } catch (Error e) {
      verificationErrors.append("Should show qr code. ").append(URL).append(e.getMessage()).append("/n");
    }
    WebElement image = Helpers.waitForElementVisibility(By.id("qr_code_img"), driver);
    boolean loaded = JavascriptExec.checkImageLoaded(driver, image);
    try {
      assertTrue(loaded);
    } catch (Error e) {
      verificationErrors.append("Should load qr code image. ").append(URL).append(e.getMessage()).append("/n");
    }
    driver.findElement(By.xpath("//div[contains(@class, \"panel-body\")]/a")).click();
    Helpers.waitForElementVisibility(By.name("loginForm"), driver);
    URL = driver.getCurrentUrl();
    try {
      assertTrue(URL.contains("/login"));
    } catch (Error e) {
      verificationErrors.append("Should goto login page. ").append(URL).append(e.getMessage()).append("/n");
    }
  }
  
  @Test
  public void emailValidation() {
    driver.findElement(By.linkText("Sign in")).click();
    String email = RegistrationHelper.registerUser(false, driver, dbHelper);
    String key = dbHelper.getValidationKey(email);
    Helpers.driverGet(RegistrationHelper.EMAIL_VALIDATION_URL + key + "1", driver);
    By ev = By.xpath(
      "(.//*[normalize-space(text()) and normalize-space(.)='Email Address Verification'])[1]/following::p[1]");
    String emailVerification = driver.findElement(ev).getText();
    try {
      assertEquals("The email address you are trying to validate does not exist in the system.", emailVerification);
    } catch (Error e) {
      verificationErrors.append(e.getMessage()).append("/n");
    }
    Helpers.driverGet(RegistrationHelper.EMAIL_VALIDATION_URL + key, driver);
    emailVerification = driver.findElement(ev).getText();
    try {
      assertEquals("You have successfully validated your email address, but your account still has to be approved" +
        " by an administrator. Hopefully soon!", emailVerification);
    } catch (Error e) {
      verificationErrors.append(e.getMessage()).append("/n");
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
    LOGGER.log(Level.INFO, "Finished register test.");
  }
}
