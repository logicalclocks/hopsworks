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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.openqa.selenium.chrome.ChromeDriverService;

public class WebDriverFactory {

  private static final Logger LOGGER = Logger.getLogger(WebDriverFactory.class.getName());
  private static final String GECKODRIVER_VERSION = "0.23.0";
  private static final String CHROMEDRIVER_VERSION = "76.0.3809.68";
  private static final String GECKODRIVER = "geckodriver";
  private static final String CHROMEDRIVER = "chromedriver";
  private static final String GECKODRIVER_URL = "https://github.com/mozilla/geckodriver/releases/download/v"
      + GECKODRIVER_VERSION + "/geckodriver-v" + GECKODRIVER_VERSION + "-";
  private static final String CHROMEDRIVER_URL = "https://chromedriver.storage.googleapis.com/" + CHROMEDRIVER_VERSION
      + "/chromedriver_";
  private static final int SUPPORTED_CHROME_VERSION = 68;
  private static final int SUPPORTED_FIREFOX_VERSION = 57;
  private static final String BROWSER_ENV = "BROWSER";
  private static final String BROWSER_UI_ENV = "HEADLESS";
  private static final String HOPSWORKS_URL_ENV = "HOPSWORKS_URL";
  
  public static final long IMPLICIT_WAIT_TIMEOUT = 60;

  public static WebDriver getWebDriver() {
    WebDriver driver = null;
    String downLoadsDir = FileUtils.getTempDirectory().toString() + "/webDrivers/";
    downloadDrivers(downLoadsDir);
    String chosenBrowser = System.getenv(BROWSER_ENV);

    if ("firefox".equalsIgnoreCase(chosenBrowser)) {
      File geekoDriver = new File(downLoadsDir + GECKODRIVER);
      if (geekoDriver.exists()) {
        System.setProperty(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, downLoadsDir + GECKODRIVER);
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        if ("true".equals(System.getenv(BROWSER_UI_ENV))) {
          firefoxOptions.addArguments("--headless");
        }
        driver = new FirefoxDriver(firefoxOptions);
      }
    }

    if (driver == null) {
      File chromeDriver = new File(downLoadsDir + CHROMEDRIVER);
      if (chromeDriver.exists()) {
        System.setProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, downLoadsDir + CHROMEDRIVER);
        ChromeOptions chromeOptions = new ChromeOptions();
        if ("true".equals(System.getenv(BROWSER_UI_ENV))) {
          chromeOptions.addArguments("--headless");
          chromeOptions.addArguments("--no-sandbox");
          chromeOptions.addArguments("--disable-dev-shm-usage");
        }
        driver = new ChromeDriver(chromeOptions);
      }
    }

    if (driver == null) {
      throw new IllegalStateException("No web driver found. Check your browser versions. Supported versions are:"
          + " Firefox >= " + SUPPORTED_FIREFOX_VERSION + ","
          + " Chrome >= " + SUPPORTED_CHROME_VERSION);
    }

    String url;
    if (System.getenv(HOPSWORKS_URL_ENV) != null) {
      url = System.getenv(HOPSWORKS_URL_ENV);
    } else {
      url = "https://localhost:8181/hopsworks/";
    }

    driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT_TIMEOUT, TimeUnit.SECONDS);
    driver.get(url);
    driver.manage().window().maximize();
    return driver;
  }

  public static void downloadDrivers(String downloadPath) {
    String geekoDriverUrl = GECKODRIVER_URL;
    String chromeDriverUrl = CHROMEDRIVER_URL;
    String firefoxVersionCmd = "firefox -v";
    String chromeVersionCmd = "google-chrome --version";
    if (SystemUtils.IS_OS_LINUX) {
      geekoDriverUrl += "linux64.tar.gz";
      chromeDriverUrl += "linux64.zip";
    } else if (SystemUtils.IS_OS_MAC_OSX) {
      geekoDriverUrl += "macos.tar.gz";
      chromeDriverUrl += "mac64.zip";
      firefoxVersionCmd = "/Applications/Firefox.app/Contents/MacOS/" + firefoxVersionCmd;
      chromeVersionCmd = "/Applications/Google\\ Chrome.app/Contents/MacOS/Google\\ Chrome --version";
    }
    int firefoxVersion = getVersion(firefoxVersionCmd);
    int chromeVersion = getVersion(chromeVersionCmd);

    if (chromeVersion >= SUPPORTED_CHROME_VERSION) {
      File chromeDriver = new File(downloadPath + CHROMEDRIVER);
      File chromeDriverZip = new File(downloadPath + CHROMEDRIVER + ".zip");
      downloadDriver(chromeDriver, chromeDriverZip, new File(downloadPath), chromeDriverUrl);
    }
    if (firefoxVersion >= SUPPORTED_FIREFOX_VERSION) {
      File geekoDriver = new File(downloadPath + GECKODRIVER);
      File geekoDriverZip = new File(downloadPath + GECKODRIVER + ".tar");
      downloadDriver(geekoDriver, geekoDriverZip, new File(downloadPath), geekoDriverUrl);
    }

  }

  private static void downloadDriver(File driver, File driverZip, File driverDir, String driverUrl) {
    String ext = FilenameUtils.getExtension(driverZip.getName());
    if (!driver.exists()) {
      try {
        FileUtils.copyURLToFile(new URL(driverUrl), driverZip);
        Archiver archiver;
        if ("tar".equals(ext)) {
          archiver = ArchiverFactory.createArchiver("tar", "gz");
        } else {
          archiver = ArchiverFactory.createArchiver("zip");
        }
        archiver.extract(driverZip, driverDir);
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, "Failed to download driver from: {0}", driverUrl);
      }
    }
  }

  public static int getVersion(String versionCmd) {
    StringBuilder builder = new StringBuilder();
    String[] cmd = new String[]{"bash", "-c", versionCmd};
    ProcessBuilder processBuilder = new ProcessBuilder(cmd);
    try {
      Process p = processBuilder.start();
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line = null;
      while ((line = reader.readLine()) != null) {
        builder.append(line).append("\n");
      }
      Pattern pattern = Pattern.compile("[0-9]{1,4}\\.[0-9]{1,4}");
      Matcher matcher = pattern.matcher(builder.toString());
      if (matcher.find()) {
        return Double.valueOf(matcher.group(0)).intValue();
      }
    } catch (IOException | NumberFormatException e) {
      LOGGER.log(Level.SEVERE, "Failed to get browser version with command: {0}. {1}", new Object[]{processBuilder.
        command(), e});
      return -1;
    }
    return -1;
  }
}
