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
