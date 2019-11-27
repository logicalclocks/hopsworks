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
package io.hops.hopsworks;

import io.hops.hopsworks.util.DBHelper;
import io.hops.hopsworks.util.Helpers;
import io.hops.hopsworks.util.models.User;
import io.hops.hopsworks.util.helpers.RegistrationHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

import static org.junit.Assert.fail;

public class RecoveryIT {
  private WebDriver driver;
  private DBHelper dbHelper;
  private final StringBuffer verificationErrors = new StringBuffer();
  private User user;
  
  private final static By LOGIN_HELP = By.linkText("Login help?");
  private final static By SELECT_RECOVERY_TYPE = By.xpath("(//button[contains(text(),'Continue')])");
  //qr recovery
  private final static By LOST_DEVICE_SELECT = By.id("lost_device");
  private final static By DEVICE_RECOVERY_FORM = By.name("mobileRecoveryForm");
  private final static By LOST_DEVICE_EMAIL = By.id("mobileRecoveryForm_inputEmail");
  private final static By PASSWORD_INPUT = By.id("pwd-field");
  private final static By PASSWORD_INPUT_TYPE = By.xpath("(.//input[@id='pwd-field'])/following::span");
  private final static By DEVICE_RECOVERY_FORM_SUCCESS = By.id("mobileRecovery-success-msg");
  private final static By DEVICE_RECOVERY_FORM_ERROR = By.id("mobileRecovery-error-msg");
  //password recovery
  private final static By FORGOT_PASSWORD_SELECT = By.id("forgot_pwd");
  private final static By PASSWORD_RECOVERY_FORM = By.name("recoveryForm");
  private final static By FORGOT_PASSWORD_EMAIL = By.id("recover_inputEmail");
  private final static By SECURITY_QUESTION = By.name("sec_question");
  private final static By SECURITY_ANSWER = By.id("sec-answer-field");
  private final static By SECURITY_ANSWER_TYPE = By.xpath("(.//input[@name='sec_answer'])/following::span");
  private final static By PASSWORD_RECOVERY_SUCCESS = By.id("recovery-success-msg");
  private final static By PASSWORD_RECOVERY_ERROR = By.id("recovery-error-msg");
  // password reset
  private final static By PASSWORD_RESET_PWD = By.name("user_password");
  private final static By PASSWORD_RESET_PWD_CONFIRM = By.name("user_password_confirm");
  private final static By PASSWORD_RESET_SUCCESS = By.id("reset-success-msg");
  private final static By PASSWORD_RESET_ERROR = By.id("reset-error-msg");
  // qr code reset
  private final static By QR_CODE_RESET_SUCCESS = By.id("qr-recovery-success-msg");
  private final static By QR_CODE_RESET_ERROR = By.id("qr-recovery-error-msg");
  private final static By QR_CODE_RESET_VIEW = By.xpath("(.//*[normalize-space(text()) and normalize-space(" +
    ".)='Hopsworks'])[2]/following::h1[1]");
  
  
  private final static By ERROR_MSG = By.id("error-msg");
  private final static String PASSWORD_RECOVERY_URL = "/hopsworks/#!/passwordRecovery?key=";
  private final static String QR_CODE_RECOVERY_URL = "/hopsworks/#!/qrRecovery?key=";
  private String password = "12345Ab";
  
  @Before
  public void startUp() {
    driver = WebDriverFactory.getWebDriver();
    dbHelper = new DBHelper();
  }
  
  private void createUser() {
    user = RegistrationHelper.createActivatedUser(password, true, driver, dbHelper);
  }
  
  private void gotoPasswordRecovery() {
    driver.findElement(LOGIN_HELP).click();
    Helpers.waitForElement(FORGOT_PASSWORD_SELECT, driver);
    driver.findElement(FORGOT_PASSWORD_SELECT).click();
    driver.findElement(SELECT_RECOVERY_TYPE).click();
  }
  
  private void gotoQRCodeRecovery() {
    driver.findElement(LOGIN_HELP).click();
    Helpers.waitForElement(FORGOT_PASSWORD_SELECT, driver);
    driver.findElement(LOST_DEVICE_SELECT).click();
    driver.findElement(SELECT_RECOVERY_TYPE).click();
  }
  
  private void testTogglePassword(By element, By xpath) {
    Helpers.testTogglePassword(element, xpath, verificationErrors, driver);
  }
  
  @Test
  public void testSecurityAnswerTypeToggle() {
    gotoPasswordRecovery();
    driver.findElement(SECURITY_ANSWER).click();
    driver.findElement(SECURITY_ANSWER).clear();
    driver.findElement(SECURITY_ANSWER).sendKeys("Some answer");
    testTogglePassword(SECURITY_ANSWER, SECURITY_ANSWER_TYPE);
  }
  
  @Test
  public void testPasswordTypeToggle() {
    gotoQRCodeRecovery();
    driver.findElement(PASSWORD_INPUT).click();
    driver.findElement(PASSWORD_INPUT).clear();
    driver.findElement(PASSWORD_INPUT).sendKeys("Some answer");
    testTogglePassword(PASSWORD_INPUT, PASSWORD_INPUT_TYPE);
  }
  
  @Test
  public void passwordRecovery() {
    createUser();
    gotoPasswordRecovery();
    driver.findElement(FORGOT_PASSWORD_EMAIL).click();
    driver.findElement(FORGOT_PASSWORD_EMAIL).clear();
    driver.findElement(FORGOT_PASSWORD_EMAIL).sendKeys(user.getEmail());
    new Select(driver.findElement(SECURITY_QUESTION)).selectByVisibleText("Name of your first pet?");
    driver.findElement(SECURITY_QUESTION).click();
    driver.findElement(SECURITY_ANSWER).click();
    driver.findElement(SECURITY_ANSWER).clear();
    driver.findElement(SECURITY_ANSWER).sendKeys("pets");
    driver.findElement(PASSWORD_RECOVERY_FORM).submit();
    assertEqualsElementText("Security question or answer did not match", PASSWORD_RECOVERY_ERROR);
    driver.findElement(SECURITY_ANSWER).clear();
    driver.findElement(SECURITY_ANSWER).sendKeys("pet");
    driver.findElement(PASSWORD_RECOVERY_FORM).submit();
    assertEqualsElementText("Your password reset email has been sent. Please check your inbox.",
      PASSWORD_RECOVERY_SUCCESS);
    String key = dbHelper.getValidationKey(user.getEmail());
    Helpers.driverGet(PASSWORD_RECOVERY_URL + key + "1", driver);//wrong key
    assertEqualsElementText("Error: Incorrect validation key", ERROR_MSG);
    Helpers.driverGet(PASSWORD_RECOVERY_URL + key, driver);
    Helpers.waitForElement(PASSWORD_RECOVERY_FORM, driver);
    driver.findElement(PASSWORD_RESET_PWD).clear();
    String newPwd = password + "c";
    driver.findElement(PASSWORD_RESET_PWD).sendKeys(newPwd);
    driver.findElement(PASSWORD_RESET_PWD_CONFIRM).clear();
    driver.findElement(PASSWORD_RESET_PWD_CONFIRM).sendKeys(newPwd);
    driver.findElement(PASSWORD_RECOVERY_FORM).submit();
    assertEqualsElementText("Your password was successfully changed.", PASSWORD_RESET_SUCCESS);
    driver.findElement(By.linkText("Back to login")).click();
  }
  
  
  @Test
  public void qrCodeRecovery() {
    createUser();
    gotoQRCodeRecovery();
    driver.findElement(LOST_DEVICE_EMAIL).click();
    driver.findElement(LOST_DEVICE_EMAIL).clear();
    driver.findElement(LOST_DEVICE_EMAIL).sendKeys(user.getEmail());
    driver.findElement(PASSWORD_INPUT).click();
    driver.findElement(PASSWORD_INPUT).clear();
    driver.findElement(PASSWORD_INPUT).sendKeys(password + "1");
    driver.findElement(DEVICE_RECOVERY_FORM).submit();
    assertEqualsElementText("Incorrect email or password.", DEVICE_RECOVERY_FORM_ERROR);
    driver.findElement(PASSWORD_INPUT).clear();
    driver.findElement(PASSWORD_INPUT).sendKeys(password);
    driver.findElement(DEVICE_RECOVERY_FORM).submit();
    assertEqualsElementText("Your QR code reset email has been sent. Please check your inbox.",
      DEVICE_RECOVERY_FORM_SUCCESS);
    String key = dbHelper.getValidationKey(user.getEmail());
    Helpers.driverGet(QR_CODE_RECOVERY_URL + key + "1", driver);//wrong key
    assertEqualsElementText("Incorrect validation key", QR_CODE_RESET_ERROR);
    Helpers.driverGet(QR_CODE_RECOVERY_URL + key, driver);
    assertEqualsElementText("Four steps, and you're there...", QR_CODE_RESET_VIEW);
    driver.findElement(By.linkText("Ok")).click();
  }
  
  private void assertEqualsElementText(String msg, By element) {
    Helpers.assertEqualsElementText(msg, element, verificationErrors, driver);
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
