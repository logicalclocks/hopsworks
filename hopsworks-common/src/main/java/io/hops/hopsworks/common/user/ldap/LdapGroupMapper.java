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

package io.hops.hopsworks.common.user.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class LdapGroupMapper {

  private static final String GROUP_SEPARATOR = ",";
  private static final String MAPPING_SEPARATOR = "->";
  private static final String GROUP_MAPPING_SEPARATOR = ";";
  private final String mappingStr;
  private final Map<String, List<String>> mappings = new HashMap<>();

  public LdapGroupMapper(String mappings) {
    this.mappingStr = mappings;
    parse(mappings);
  }

  /**
   * Mapping string in format 
   * "groupA-> HOPS_USER,HOPS_ADMIN;group B -> HOPS_USER; group-C-> HOPS_ADMIN,CLUSTER_AGENT, AGENT"
   * @param mappingStr 
   */
  private void parse(String mappingStr) {
    if (mappingStr == null || mappingStr.isEmpty()) {
      return;
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
      this.mappings.put(mappedGroup, mappedTOGroupList);
    }
  }

  public Map<String, List<String>> getMappings() {
    return mappings;
  }

  public List<String> getMappedGroups(List<String> groups) {
    List<String> mappedGroups = new ArrayList<>();
    if (this.mappings == null || this.mappings.isEmpty() || groups == null || groups.isEmpty()) {
      return mappedGroups;
    }
    for (String group : groups) {
      addUnique(this.mappings.get(group), mappedGroups);
    }
    return mappedGroups;
  }

  public String getMappingStr() {
    return mappingStr;
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
