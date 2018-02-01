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
