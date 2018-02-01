/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
