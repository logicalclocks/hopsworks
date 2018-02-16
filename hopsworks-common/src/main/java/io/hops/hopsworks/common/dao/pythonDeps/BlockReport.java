/*
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
 *
 */

package io.hops.hopsworks.common.dao.pythonDeps;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BlockReport {

  public static class Lib implements Comparable<PythonDep>{

    private final String lib;
    private final String channelUrl;
    private final String version;

    public Lib(String lib, String channelUrl, String version) {
      this.lib = lib;
      this.channelUrl = channelUrl;
      this.version = version;
    }

    public String getChannelUrl() {
      return channelUrl;
    }

    public String getLib() {
      return lib;
    }

    public String getVersion() {
      return version;
    }

    @Override
    public int compareTo(PythonDep t) {
      if (this.lib.compareTo(t.getDependency()) != 0) {
        return -1;
      }
      if (this.version.compareTo(t.getVersion()) != 0) {
        return -2;
      }
      if (this.channelUrl.compareTo(t.getRepoUrl().getUrl()) != 0) {
        return -3;
      }
      
      return 0;
    }
    
  }

  private String project;
  private Map<String, Lib> libs = new HashMap<>();

  public BlockReport() {
  }

  public String getProject() {
    return project;
  }

  public Collection<Lib> getLibs() {
    return libs.values();
  }
  
  public Lib getLib(String name) {
    return libs.get(name);
  }
  
  public void removeLib(String name) {
    libs.remove(name);
  }


  public void addLib(String lib, String channelUrl, String version) {
    this.libs.put(lib, new Lib(lib, channelUrl, version));
  }

  public void setProject(String project) {
    this.project = project;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof BlockReport) {
      BlockReport pd = (BlockReport) o;
      if (pd.getProject().compareTo(this.project) == 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return (this.project.hashCode() + this.libs.hashCode());
  }
}
