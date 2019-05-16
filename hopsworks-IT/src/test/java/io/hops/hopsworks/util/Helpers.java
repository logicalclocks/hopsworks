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
package io.hops.hopsworks.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

import io.hops.hopsworks.util.helpers.LoginHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Helpers {
  private static final Logger LOGGER = Logger.getLogger(Helpers.class.getName());
  public static void enableTwoFactor(WebDriver driver, DBHelper dbHelper) {
    LoginHelper.loginAsAdmin(driver, dbHelper);
    driverGet("hopsworks-admin/security/protected/admin/refreshVariables.xhtml", driver);
    driver.findElement(By.id("updateVariablesForm:variablesTable:idColumn:filter")).click();
    driver.findElement(By.id("updateVariablesForm:variablesTable:idColumn:filter")).clear();
    driver.findElement(By.id("updateVariablesForm:variablesTable:idColumn:filter")).sendKeys("twofactor_auth");
    wait(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Reload variables'])[1]/following::span[1]"),
      "(1 of 1)", driver);
    driver.findElement(By.xpath(".//*[contains(@class, 'ui-icon-pencil')]")).click();
    driver.findElement(By.id("updateVariablesForm:variablesTable:0:j_idt17")).click();
    driver.findElement(By.id("updateVariablesForm:variablesTable:0:j_idt17")).clear();
    driver.findElement(By.id("updateVariablesForm:variablesTable:0:j_idt17")).sendKeys("true");
    driver.findElement(By.xpath(".//*[contains(@class, 'ui-icon-check')]")).click();
    assertEquals("Updated variable : twofactor_auth to: true", driver.findElement(
      By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Updated variable : twofactor_auth to: true'])"
        + "[1]/following::p[1]")).getText());
    driverGet("hopsworks", driver);
    LoginHelper.logout(driver);
  }
  
  public static void driverGet(String path, WebDriver driver) {
    String urlStr = driver.getCurrentUrl();
    URL url;
    try {
      url = new URL(urlStr);
      path = path.startsWith("/") ? path : "/" + path;
      String fullPath = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + path;
      driver.get(fullPath);
    } catch (MalformedURLException ex) {
      Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public static WebElement waitForElement(By by, WebDriver driver) {
    return (new WebDriverWait(driver, 60).pollingEvery(Duration.ofSeconds(5))).until((ExpectedCondition<WebElement>) (
      WebDriver d) -> d.findElement(by));
  }
  
  public static boolean wait(By by, String txt, WebDriver driver) {
    return (new WebDriverWait(driver, 100)).until((ExpectedCondition<Boolean>) (WebDriver d) -> d.findElement(by).
      getText().equals(txt));
  }
  
  public static WebElement waitForElementToBeClickable(By by, WebDriver driver) {
    WebDriverWait wait = new WebDriverWait(driver, 120);
    WebElement element =
      wait.pollingEvery(Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(by));
    return element;
  }
  
  public static WebElement waitForElementVisibility(By by, WebDriver driver) {
    WebElement element = waitForElementVisibility(by, driver, 60);
    return element;
  }
  
  public static WebElement waitForElementVisibility(By by, WebDriver driver, long timeOut) {
    WebDriverWait wait = new WebDriverWait(driver, timeOut);
    WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    return element;
  }
  
  public static WebElement waitForElementInvisibility(By by, WebDriver driver) {
    try {
      WebElement element = driver.findElement(by);
      WebDriverWait wait = new WebDriverWait(driver, 60);
      wait.until(ExpectedConditions.invisibilityOf(element));
      return element;
    } catch (NoSuchElementException e) {
    }
    return null;
  }
  
  public static WebElement waitForElementInvisibility(WebElement element, WebDriver driver) {
    WebDriverWait wait = new WebDriverWait(driver, 60);
    wait.until(ExpectedConditions.invisibilityOf(element));
    return element;
  }
  
  public static void testTogglePassword(By element, By xpath, StringBuffer verificationErrors, WebDriver driver) {
    String content = driver.findElement(element).getAttribute("type");
    try {
      assertEquals(content, "password");
    } catch (Error e) {
      verificationErrors.append("Default password type should be password but found ")
        .append(content)
        .append(e.getMessage()).append("/n");
    }
    driver.findElement(xpath).click();
    content = driver.findElement(element).getAttribute("type");
    try {
      assertEquals(content, "text");
    } catch (Error e) {
      verificationErrors.append("When clicked password type should be text but found ")
        .append(content)
        .append(e.getMessage()).append("/n");
    }
    driver.findElement(xpath).click();
    content = driver.findElement(element).getAttribute("type");
    try {
      assertEquals(content, "password");
    } catch (Error e) {
      verificationErrors.append("When clicked again password type should be password but found ")
        .append(content)
        .append(e.getMessage()).append("/n");
    }
  }
  
  public static void assertEqualsElementText(String msg, By element, StringBuffer verificationErrors,
    WebDriver driver) {
    Helpers.waitForElement(element, driver);
    try {
      assertEquals(msg, driver.findElement(element).getText());
    } catch (Error e) {
      verificationErrors.append(e.toString()).append("/n");
    }
  }
  
  public static void waitForModal(WebDriver driver, int timeOut) {
    new WebDriverWait(driver, timeOut)
      .until(ExpectedConditions.invisibilityOfElementLocated(By.className("modal")));
    pause(driver, 20);
  }
  
  public static void modalFooterClick(WebDriver driver, String button) {
    WebElement element = waitForElementToBeClickable(By.className("modal-footer"), driver);
    element.findElements(By.tagName("button")).stream()
      .filter(i -> i.getText().equals(button)).findAny().get().click();
  }
  
  public static void pause(WebDriver driver, long timeOut) {
    driver.manage().timeouts().implicitlyWait(timeOut, TimeUnit.SECONDS);
  }
  
  public static void clickWithRetry(WebDriver driver, By by) {
    waitForElementToBeClickable(by, driver);
    boolean failed = true;
    int countRetry = 0;
    while(failed && countRetry < 5) {
      try {
        driver.findElement(by).click();
        failed = false;
      } catch (WebDriverException e) {
        pause(driver, 5);
      }
      countRetry++;
    }
    if (failed) {
      throw new NoSuchElementException("Unable to locate element: " + by.toString());
    }
  }
}
