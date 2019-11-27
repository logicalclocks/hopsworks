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
package io.hops.hopsworks.admin;

import static org.junit.Assert.assertEquals;
import io.hops.hopsworks.WebDriverFactory;
import io.hops.hopsworks.util.DBHelper;
import io.hops.hopsworks.util.Helpers;
import io.hops.hopsworks.util.models.User;
import io.hops.hopsworks.util.helpers.LoginHelper;
import org.junit.After;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class VariablesIT {

  private WebDriver driver;
  private DBHelper dbHelper;
  private User user;
  private final StringBuffer verificationErrors = new StringBuffer();

  @Before
  public void startUp() {
    driver = WebDriverFactory.getWebDriver();
    dbHelper = new DBHelper();
    user = LoginHelper.loginAsAdmin(driver, dbHelper);
    Helpers.driverGet("hopsworks-admin/security/protected/admin/refreshVariables.xhtml", driver);
  }

  @Test
  public void enableTwoFactor() {
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
    try {
      assertEquals("Updated variable : twofactor_auth to: true", driver.findElement(
        By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Logout'])[1]/following::span[2]")).getText());
    } catch (Error e) {
      verificationErrors.append(e.toString());
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
  
  private boolean wait(By by, String txt) {
    return (new WebDriverWait(driver, 100)).until((ExpectedCondition<Boolean>) (WebDriver d) ->
        d.findElement(by).getText().equals(txt));
  }
}
