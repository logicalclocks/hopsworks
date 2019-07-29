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
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

import io.hops.hopsworks.util.helpers.LoginHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Helpers {
  
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
  
  public static WebElement waitForElementVisibility(By by, WebDriver driver) {
    WebElement element = driver.findElement(by);
    WebDriverWait wait = new WebDriverWait(driver, 60);
    wait.until(ExpectedConditions.visibilityOf(element));
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
}
