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

import io.hops.hopsworks.WebDriverFactory;
import io.hops.hopsworks.util.DBHelper;
import io.hops.hopsworks.util.Helpers;
import io.hops.hopsworks.util.JavascriptExec;
import io.hops.hopsworks.util.models.User;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class RegistrationHelper {
  
  public static final String EMAIL_VALIDATION_URL = "hopsworks-admin/security/validate_account.xhtml?key=";
  public static final String ADMIN = "HOPS_ADMIN";
  public static final String USER = "HOPS_USER";
  
  private static void gotoRegisterPage (WebDriver driver) {
    driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
    try {
      driver.findElement(By.name("registerForm"));
    } catch (NoSuchElementException | Error e) {
      driver.findElement(By.linkText("Register")).click();
    }
    driver.manage().timeouts().implicitlyWait(WebDriverFactory.IMPLICIT_WAIT_TIMEOUT, TimeUnit.SECONDS);
  }
  
  private static void registerTest(String firstName, String lastName, String email, String password, boolean twoFactor,
    WebDriver driver, DBHelper dbHelper) {
    boolean twoFactorEnabled = dbHelper.isTwoFactorEnabled();
    if (twoFactor && !twoFactorEnabled) {
      Helpers.enableTwoFactor(driver, dbHelper);
      gotoRegisterPage(driver);
    }
    Helpers.waitForElement(By.name("registerForm"), driver);
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
  }
  
  private static void register(String firstName, String lastName, String email, String password, boolean twoFactor,
    WebDriver driver, DBHelper dbHelper) {
    gotoRegisterPage(driver);
    registerTest(firstName, lastName, email, password, twoFactor, driver, dbHelper);
    if (twoFactor) {
      assertEquals("Four steps, and you're there...", driver.findElement(
        By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Hopsworks'])[2]/following::h1[1]"))
        .getText());
      driver.findElement(By.linkText("Ok")).click();
    } else {
      Helpers.waitForElementVisibility(By.id("regSuccess"), driver);
      String successAttr = driver.findElement(By.id("regSuccess")).getAttribute("aria-hidden");
      String errorAttr = driver.findElement(By.id("regError")).getAttribute("aria-hidden");
      //assert no error msg
      assertEquals("true", errorAttr);
      assertEquals("false", successAttr);
      driver.findElement(By.linkText("Sign in")).click();
    }
  }
  
  public static String registerUserTest(boolean twoFactor, WebDriver driver, DBHelper dbHelper) {
    Random rand = new Random();
    String firstName = "user_" + rand.nextInt(100000);
    String lastName = "user_" + rand.nextInt(100000);
    String email = firstName + "@hopsworks.ai";
    String password = "12345Ab";
    registerTest(firstName, lastName, email, password, twoFactor, driver, dbHelper);
    return email;
  }
  
  public static String registerUser(boolean twoFactor, WebDriver driver, DBHelper dbHelper) {
    Random rand = new Random();
    String firstName = "user_" + rand.nextInt(100000);
    String lastName = "user_" + rand.nextInt(100000);
    String email = firstName + "@hopsworks.ai";
    String password = "12345Ab";
    register(firstName, lastName, email, password, twoFactor, driver, dbHelper);
    return email;
  }
  
  public static void registerUser(String email, String password, boolean twoFactor, WebDriver driver,
    DBHelper dbHelper) {
    Random rand = new Random();
    String firstName = "user_" + rand.nextInt(100000);
    String lastName = "user_" + rand.nextInt(100000);
    register(firstName, lastName, email, password, twoFactor, driver, dbHelper);
  }
  
  public static String registerUser(String password, boolean twoFactor, WebDriver driver, DBHelper dbHelper) {
    Random rand = new Random();
    String firstName = "user_" + rand.nextInt(100000);
    String lastName = "user_" + rand.nextInt(100000);
    String email = firstName + "@hopsworks.ai";
    register(firstName, lastName, email, password, twoFactor, driver, dbHelper);
    return email;
  }
  
  public static void validateEmail(String key, WebDriver driver) {
    Helpers.driverGet(EMAIL_VALIDATION_URL + key, driver);
    assertEquals("You have successfully validated your email address, but your account still has to be approved " +
      "by an administrator. Hopefully soon!", driver.findElement(By.xpath(
      "(.//*[normalize-space(text()) and normalize-space(.)='Email Address Verification'])[1]/following::p[1]"))
      .getText());
  }
  
  public static User createActivatedUser(String password, boolean twoFactor, WebDriver driver, DBHelper dbHelper) {
    String email = registerUser(password, twoFactor, driver, dbHelper);
    User user = validateAndActivateUser(email, driver, dbHelper);
    Helpers.driverGet("hopsworks", driver);
    return user;
  }
  
  public static User createActivatedUser(String email, String password, boolean twoFactor, WebDriver driver,
    DBHelper dbHelper) {
    registerUser(email, password, twoFactor, driver, dbHelper);
    User user = validateAndActivateUser(email, driver, dbHelper);
    Helpers.driverGet("hopsworks", driver);
    return user;
  }
  
  public static User validateAndActivateUser(String email, WebDriver driver, DBHelper dbHelper) {
    User user = dbHelper.getUserByEmail(email);
    String key = dbHelper.getValidationKey(user.getEmail());
    validateEmail(key, driver);
    dbHelper.activateUser(email);
    dbHelper.addGroup(user, USER);
    return user;
  }
  
  public static User createAdminUser(String password, WebDriver driver, DBHelper dbHelper) {
    User user = createActivatedUser(password, false, driver, dbHelper);
    dbHelper.addGroup(user, ADMIN);
    return user;
  }
  
}
