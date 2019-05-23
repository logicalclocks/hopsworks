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
package io.hops.hopsworks.remote.user.auth;

import io.hops.hopsworks.common.dao.remote.user.RemoteUserType;
import io.hops.hopsworks.common.util.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RemoteUserGroupMapper {

  @EJB
  private Settings settings;
  private static final String GROUP_SEPARATOR = ",";
  private static final String MAPPING_SEPARATOR = "->";
  private static final String GROUP_MAPPING_SEPARATOR = ";";

  /**
   * Creates a group mapping from remote user to local
   * @return group mapping 
   */
  public Map<String, List<String>> getGroupMappings(String mappingStr) {
    Map<String, List<String>> mappings = new HashMap<>();
    if (mappingStr == null || mappingStr.isEmpty()) {
      return mappings;
    }
    StringTokenizer tokenizer = new StringTokenizer(mappingStr, GROUP_MAPPING_SEPARATOR);
    while (tokenizer.hasMoreElements()) {
      String mapping = tokenizer.nextToken();
      String[] mappingGroups = mapping.split(MAPPING_SEPARATOR);
      String mappedGroup = null;
      String[] mappedToGroups = null;
      if (mappingGroups != null && mappingGroups.length == 2) {
        mappedGroup = mappingGroups[0].trim();
        mappedToGroups = mappingGroups[1].split(GROUP_SEPARATOR);
      }
      if (mappedGroup == null || mappedGroup.isEmpty() || mappedToGroups == null || mappedToGroups.length < 1) {
        continue;
      }
      List<String> mappedTOGroupList = new ArrayList<>();
      for (String grp : mappedToGroups) {
        mappedTOGroupList.add(grp.trim());
      }
      mappings.put(mappedGroup, mappedTOGroupList);
    }
    return mappings;
  }

  /**
   * Returns a list of local group names based on the group mapping in getMappingStr()
   * @param groups
   * @return 
   */
  public List<String> getMappedGroups(List<String> groups, RemoteUserType type) {
    List<String> mappedGroups = new ArrayList<>();
    Map<String, List<String>> mappings;
    switch (type) {
      case KRB:
      case LDAP:
        mappings = getGroupMappings(settings.getLdapGroupMapping());
        break;
      case OAUTH2:
        mappings = getGroupMappings(settings.getOAuthGroupMapping());
        break;
      default:
        mappings = getGroupMappings("");
    }
    if (mappings == null || mappings.isEmpty() || groups == null || groups.isEmpty()) {
      return mappedGroups;
    }
    for (String group : groups) {
      addUnique(mappings.get(group), mappedGroups);
    }
    return mappedGroups;
  }

  private void addUnique(List<String> src, List<String> dest) {
    if (src == null) {
      return;
    }
    for (String str : src) {
      if (!dest.contains(str)) {
        dest.add(str);
      }
    }
  }

}
