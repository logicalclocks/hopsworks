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
package io.hops.hopsworks.util;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

public class JavascriptExec {

  public static void jsClick(WebDriver driver, WebElement element) {
    ((JavascriptExecutor) driver).executeScript("return arguments[0].click();", element);
  }

  public static boolean checkImageLoaded(WebDriver driver, WebElement element) {
    Object result = ((JavascriptExecutor) driver).executeScript("return arguments[0].complete && "
        + "typeof arguments[0].naturalWidth != \"undefined\" && arguments[0].naturalWidth > 0", element);
    boolean loaded = false;
    if (result instanceof Boolean) {
      loaded = (Boolean) result;
    }
    return loaded;
  }
  
  public static Boolean waitPageLoad(WebDriver driver) {
    return new WebDriverWait(driver, 60).until(
          webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
  }
}
