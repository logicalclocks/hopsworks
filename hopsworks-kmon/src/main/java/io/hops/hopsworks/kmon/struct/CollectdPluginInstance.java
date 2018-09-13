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

package io.hops.hopsworks.kmon.struct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectdPluginInstance implements Serializable {

  private HashMap<String, HashSet<String>> typeInstances;
  private HashMap<String, String> statements;
  private HashMap<String, String> instancesFroms;
  private String pluginInstanceName;

  public CollectdPluginInstance(String pluginInstanceName) {
    typeInstances = new HashMap<String, HashSet<String>>();
    statements = new HashMap<String, String>();
    instancesFroms = new HashMap<String, String>();
    this.pluginInstanceName = pluginInstanceName;
  }

  public void add(String type, String typeInstance, String statement,
          String InstancesFrom) {

    if (statement != null) {
//            if (statement.isEmpty()) {
//                typeInstance += "-" + InstancesFrom;
//            } else {
//                typeInstance += "-@n";
//            }
      typeInstance += statement.isEmpty() ? "-" + InstancesFrom : "-@n";
    }
    HashSet<String> instancesList;
    if (typeInstances.containsKey(type)) {
      instancesList = typeInstances.get(type);
      instancesList.add(typeInstance);
      typeInstances.put(type, instancesList);
    } else {
      instancesList = new HashSet<String>();
      instancesList.add(typeInstance);
      typeInstances.put(type, instancesList);
    }
    instancesFroms.put(key(type, typeInstance), InstancesFrom);
    statements.put(key(type, typeInstance), statement);
  }

  public List<String> getTypes() {
    List types = new ArrayList<String>();
    types.addAll(typeInstances.keySet());
    return types;
  }

  public Set<String> getTypeInstances(String type) {
    return typeInstances.get(type);
  }

  public String getName() {
    return pluginInstanceName;
  }

  public String getInfo(String type, String typeInstance) {
    if (!statements.get(key(type, typeInstance)).isEmpty()) {
      return "@n = " + instancesFroms.get(key(type, typeInstance)) + " in \""
              + statements.get(key(type, typeInstance)) + "\"";
    }
    return "";
  }

  private String key(String type, String typeInstance) {
    return type + "-" + typeInstance;
  }
}
