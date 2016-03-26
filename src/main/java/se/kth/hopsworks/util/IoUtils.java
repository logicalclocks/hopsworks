package se.kth.hopsworks.util;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IoUtils {
  private static final Logger logger = Logger.getLogger(IoUtils.class.getName());

  public static String readContentFromClasspath(String path) throws IOException {
    URL url = IoUtils.class.getResource(path);// Resources.getResource(path);
    if (url == null) {
      throw new IOException("No config.props file found in cookbook");
    }
    return Resources.toString(url, Charsets.UTF_8);
  }

  public static String readContentFromPath(String path) throws IOException {
    return Files.toString(new File(path), Charsets.UTF_8);
  }

  public static List<String> readLinesFromClasspath(String url) throws IOException {
    return Resources.readLines(Resources.getResource(url), Charsets.UTF_8);
  }

  public static List<String> readLinesFromPath(String url) throws IOException {
    return Files.readLines(new File(url), Charsets.UTF_8);
  }

  public static List<String> readLinesFromWeb(String url) throws IOException {
    URL fileUrl = new URL(url);
    return Resources.readLines(fileUrl, Charsets.UTF_8);
  }

  public static String readContentFromWeb(String url) throws IOException {
    URL fileUrl = new URL(url);
    return Resources.toString(fileUrl, Charsets.UTF_8);
  }

  public static String getMainClassNameFromJar(String amJarPath, InputStream inputStream  ) {
    if (amJarPath == null) {
      throw new IllegalStateException(
          "amJar path cannot be null.");
    }
    String fileName = amJarPath;
    
    if (amJarPath.startsWith("hdfs:")) {
      // download the jar file
    }
    
    String mainClassName = null;

    try (JarFile jarFile = new JarFile(fileName)) {
      Manifest manifest = jarFile.getManifest();
      if (manifest != null) {
        mainClassName = manifest.getMainAttributes().getValue("Main-Class");
      }
    } catch (IOException io) {
      logger.log(Level.SEVERE, "Could not open jar file " + amJarPath
          + " to load main class.", io);
      return null;
    }

    if (mainClassName != null) {
      return mainClassName.replaceAll("/", ".");
    } else {
      return null;
    }
  }

}
