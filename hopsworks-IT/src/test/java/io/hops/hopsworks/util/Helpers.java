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
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Helpers {

  private final WebDriver driver;

  public Helpers(WebDriver driver) {
    this.driver = driver;
  }

  public void login(String username, String password) {
    driverGet("hopsworks/#!/login");
    driver.findElement(By.id("login_inputEmail")).clear();
    driver.findElement(By.id("login_inputEmail")).sendKeys(username);
    driver.findElement(By.id("login_inputPassword")).sendKeys(password);
    driver.findElement(By.name("loginForm")).submit();
    assertEquals("admin@kth.se", driver.findElement(By.id("navbarProfile")).getText());
  }

  public void enableTwoFactor() {
    driverGet("hopsworks-admin/security/protected/admin/refreshVariables.xhtml");
    driver.findElement(By.id("updateVariablesForm:variablesTable:idColumn:filter")).click();
    driver.findElement(By.id("updateVariablesForm:variablesTable:idColumn:filter")).clear();
    driver.findElement(By.id("updateVariablesForm:variablesTable:idColumn:filter")).sendKeys("twofactor_auth");
    wait(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Reload variables'])[1]/following::span[1]"), 
        "(1 of 1)");
    driver.findElement(By.xpath(".//*[contains(@class, 'ui-icon-pencil')]")).click();
    driver.findElement(By.id("updateVariablesForm:variablesTable:0:j_idt17")).click();
    driver.findElement(By.id("updateVariablesForm:variablesTable:0:j_idt17")).clear();
    driver.findElement(By.id("updateVariablesForm:variablesTable:0:j_idt17")).sendKeys("true"); 
    driver.findElement(By.xpath(".//*[contains(@class, 'ui-icon-check')]")).click();
    assertEquals("Updated variable : twofactor_auth to: true", driver.findElement(
        By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Updated variable : twofactor_auth to: true'])"
            + "[1]/following::p[1]")).getText());
  }

  public void logout() {
    driverGet("hopsworks/");
    if (driver.findElement(By.id("navbarProfile")) != null) {
      driver.findElement(By.id("navbarProfile")).click();
      driver.findElement(By.linkText("Sign out")).click();
    }
  }

  public void driverGet(String path) {
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

  public WebElement waitForElement(By by) {
    return (new WebDriverWait(driver, 600).pollingEvery(Duration.ofSeconds(5))).until((ExpectedCondition<WebElement>) (
        WebDriver d) -> d.findElement(by));
  }

  private boolean wait(By by, String txt) {
    return (new WebDriverWait(driver, 100)).until((ExpectedCondition<Boolean>) (WebDriver d) -> d.findElement(by).
        getText().equals(txt));
  }

}
