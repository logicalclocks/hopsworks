/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

/*
 * Client Code:
 *
 * Settings settings = ...
 * StringBuilder zeppelin_env = ConfigFileGenerator.instantiateFromTemplate(
 * ConfigFileGenerator.ZEPPELIN_ENV_TEMPLATE,
 * "zeppelin_dir", settings.getZeppelinDir() + projectName,
 * "spark_dir", settings.getSparkDir(),
 * "hadoop_dir", settings.getHadoopDir()
 * );
 *
 * ConfigFileGenerator.createConfigFile(settings.getZeppelinDir() + projectName
 * + "/zeppelin_env.xml", zeppelin_env.toString());
 *
 */
public class ConfigFileGenerator {

  public static final String TEMPLATE_ROOT = File.separator + "io"
          + File.separator + "hops";
  public static final String LOG4J_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "zeppelin" + File.separator
          + "log4j_template.properties";
  public static final String ZEPPELIN_CONFIG_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "zeppelin" + File.separator
          + "zeppelin_site_template.xml";
  public static final String ZEPPELIN_ENV_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "zeppelin" + File.separator
          + "zeppelin_env_template.sh";
  public static final String INTERPRETER_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "zeppelin" + File.separator
          + "interpreter_template.json";
  public static final String JUPYTER_NOTEBOOK_CONFIG_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "jupyter" + File.separator
          + "jupyter_notebook_config_template.py";
  public static final String JUPYTER_CUSTOM_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "jupyter" + File.separator
          + "custom_template.js";
  public static final String JUPYTER_CUSTOM_KERNEL
          = TEMPLATE_ROOT + File.separator + "jupyter" + File.separator
          + "kernel_template.json";
  public static final String SPARKMAGIC_CONFIG_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "jupyter" + File.separator
          + "config_template.json";  
  public static final String LOG4J_TEMPLATE_JUPYTER
          = TEMPLATE_ROOT + File.separator + "jupyter" + File.separator
          + "log4j_template.properties";  
  public static final String METRICS_TEMPLATE
          = TEMPLATE_ROOT + File.separator
          + "metrics_template.properties";

  /**
   * @param filePath
   * @param pairs
   * @return
   * @throws IOException
   */
  public static StringBuilder instantiateFromTemplate(String filePath,
          String... pairs) throws IOException {
    if (pairs.length % 2 != 0) {
      throw new IOException(
              "Odd number of parameters when instantiating a template. Are you missing a parameter?");
    }
    StringBuilder sb = new StringBuilder();
    String script = IoUtils.readContentFromClasspath(filePath);
    if (pairs.length > 0) {
      for (int i = 0; i < pairs.length; i += 2) {
        String key = pairs[i];
        String val = pairs[i + 1];
        script = script.replaceAll("%%" + key + "%%", val);
      }
    }
    return sb.append(script);
  }

  /**
   *
   * @param filePath
   * @param params
   * @return
   * @throws IOException
   */
  public static StringBuilder instantiateFromTemplate(String filePath,
          Map<String, String> params) throws IOException {
    StringBuilder sb = new StringBuilder();
    String script = IoUtils.readContentFromClasspath(filePath);
    if (params.size() > 0) {
      for (Entry<String, String> env : params.entrySet()) {
        if (env.getValue() != null) {
          script = script.replaceAll("%%" + env.getKey() + "%%", env.getValue());
        }
      }
    }
    return sb.append(script);
  }

  public static boolean mkdirs(String path) {
    File cbDir = new File(path);
    return cbDir.mkdirs();
  }
  
  public static String getZeppelinDefaultInterpreterJson() {
    String json;
    try {
      json = IoUtils.readContentFromClasspath(INTERPRETER_TEMPLATE);
    } catch (IOException ex) {
      return null;
    }
    return json;
  }

  public static boolean deleteRecursive(File path) throws FileNotFoundException {
    if (!path.exists()) {
      throw new FileNotFoundException(path.getAbsolutePath());
    }
    boolean ret = true;
    if (path.isDirectory()) {
      for (File f : path.listFiles()) {
        ret = ret && deleteRecursive(f);
      }
    }
    return ret && path.delete();
  }

  public static boolean createConfigFile(File path, String contents) throws
          IOException {
    // write contents to file as text, not binary data
    if (!path.exists()) {
      if (!path.createNewFile()) {
        throw new IOException("Problem creating file: " + path);
      }
    }
    PrintWriter out = new PrintWriter(path);
    out.println(contents);
    out.flush();
    out.close();
    return true;
  }

}
