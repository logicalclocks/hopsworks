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

import io.hops.hopsworks.util.Helpers;
import io.hops.hopsworks.util.JavascriptExec;
import java.time.Duration;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class RegisterIT {

  private static final Logger LOGGER = Logger.getLogger(RegisterIT.class.getName());
  private WebDriver driver;
  private final StringBuffer verificationErrors = new StringBuffer();

  @Before
  public void startUp() {
    driver = WebDriverFactory.getWebDriver();
    driver.findElement(By.linkText("Register")).click();
    waitForElement(By.name("registerForm"));
  }

  private WebElement registerUser(boolean twoFactor) {
    Random rand = new Random();
    String firstName = "user_" + rand.nextInt(100000);
    String lastName = "user_" + rand.nextInt(100000);
    String email = firstName + "@kth.se";
    String password = "12345Ab";
    driver.findElement(By.name("first_name")).sendKeys(firstName);
    driver.findElement(By.name("last_name")).sendKeys(lastName);
    driver.findElement(By.name("user_email")).sendKeys(email);
    driver.findElement(By.name("phone_number")).sendKeys("1234567890");
    driver.findElement(By.id("id-password")).sendKeys(password);
    driver.findElement(By.name("user_password_confirm")).sendKeys(password);
    Select seqQA = new Select(driver.findElement(By.name("sec_question")));
    seqQA.selectByVisibleText("Name of your first pet?");
    driver.findElement(By.name("sec_answer")).sendKeys("pet");
    driver.findElement(By.name("user_agreed")).click();
    WebElement testUserElement = driver.findElement(By.name("test_user"));
    JavascriptExec.jsClick(driver, testUserElement);
    if (twoFactor) {
      driver.findElement(By.name("2FactorAuth")).click();
    }
    driver.findElement(By.name("registerForm")).submit();
    By by;
    if (twoFactor) {
      by = By.id("qr_code_panel");
    } else {
      by = By.xpath("//div[contains(@class, \"panel-body\")]/div[2]");
    }
    WebElement element = waitForElementVisibility(by);
    return element;
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
    String attribute = waitForElement(By.xpath("(.//input[@name='user_password'])/../../../following::div/span[3]")).
        getAttribute("aria-hidden");
    LOGGER.log(Level.INFO, "invalid password. {0}", attribute);
    try {
      assertEquals("false", attribute);
    } catch (Error e) {
      verificationErrors.append("Should not accept invalid password.").append(e.getMessage()).append("\n");
    }
    driver.findElement(By.id("id-password")).clear();
    driver.findElement(By.id("id-password")).sendKeys("12345a");
    attribute = waitForElement(By.xpath("(.//input[@name='user_password'])/../../../following::div/span[3]")).
        getAttribute("aria-hidden");
    LOGGER.log(Level.INFO, "invalid password. {0}", attribute);
    try {
      assertEquals("false", attribute);
    } catch (Error e) {
      verificationErrors.append("Should not accept invalid password.").append(e.getMessage()).append("\n");
    }
    driver.findElement(By.id("id-password")).clear();
    driver.findElement(By.id("id-password")).sendKeys("12345Ab");
    attribute = waitForElement(By.xpath("(.//input[@name='user_password'])/../../../following::div/span[3]")).
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
    String attribute = waitForElement(By.xpath("(.//input[@name='user_password'])/../../../following::div/span[4]")).
        getAttribute("aria-hidden");
    try {
      assertEquals("false", attribute);
    } catch (Error e) {
      verificationErrors.append("Should not accept passwords that do not match.").append(e.getMessage()).append("\n");
    }
    driver.findElement(By.name("user_password_confirm")).clear();
    driver.findElement(By.name("user_password_confirm")).sendKeys("12345Ab");
    attribute = waitForElement(By.xpath("(.//input[@name='user_password'])/../../../following::div/span[4]")).
        getAttribute("aria-hidden");
    try {
      assertEquals("true", attribute);
    } catch (Error e) {
      verificationErrors.append("Should accept passwords that match.").append(e.getMessage()).append("\n");
    }
    driver.findElement(By.id("id-password")).clear();
    driver.findElement(By.name("user_password_confirm")).clear();
  }

  @Test
  public void testRegister() throws Exception {
    WebElement element = registerUser(false);
    String attribute = waitForElement(By.xpath("//div[contains(@class, \"panel-body\")]/div[3]")).getAttribute(
        "aria-hidden");
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
  public void testRegisterTwoFactor() throws Exception {
    //enable 2factor
    Helpers helpers = new Helpers(driver);
    helpers.login("admin@hopsworks.com", "admin");
    helpers.enableTwoFactor();
    helpers.logout();
    driver.findElement(By.linkText("Register")).click();
    waitForElement(By.name("registerForm"));
    registerUser(true);
    String URL = driver.getCurrentUrl();
    try {
      assertTrue(URL.contains("/qrCode"));
    } catch (Error e) {
      verificationErrors.append("Should show qr code. ").append(URL).append(e.getMessage()).append("/n");
    }
    WebElement image = waitForElementVisibility(By.id("qr_code_img"));
    boolean loaded = JavascriptExec.checkImageLoaded(driver, image);
    try {
      assertTrue(loaded);
    } catch (Error e) {
      verificationErrors.append("Should load qr code image. ").append(URL).append(e.getMessage()).append("/n");
    }
    driver.findElement(By.xpath("//div[contains(@class, \"panel-body\")]/a")).click();
    waitForElementVisibility(By.name("loginForm"));
    URL = driver.getCurrentUrl();
    try {
      assertTrue(URL.contains("/login"));
    } catch (Error e) {
      verificationErrors.append("Should goto login page. ").append(URL).append(e.getMessage()).append("/n");
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
    LOGGER.log(Level.INFO, "Finshed register test.");
  }

  private WebElement waitForElement(By by) {
    return (new WebDriverWait(driver, 600).pollingEvery(Duration.ofSeconds(5))).until((ExpectedCondition<WebElement>) (
        WebDriver d ) -> d.findElement(by));
  }

  private WebElement waitForElementVisibility(By by) {
    WebElement element = driver.findElement(by);
    WebDriverWait wait = new WebDriverWait(driver, 60);
    wait.until(ExpectedConditions.visibilityOf(element));
    return element;
  }
}
