package io.hops.hopsworks.kmon.role;

import io.hops.hopsworks.kmon.struct.RoleType;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import io.hops.hopsworks.kmon.struct.ServiceType;

public class ServiceRoleMapper {

  public static final Map<ServiceType, List<RoleType>> serviceRoleMap;
  public static final Map<RoleType, String> roleFullNames;

  static {
    serviceRoleMap = new EnumMap<ServiceType, List<RoleType>>(ServiceType.class);
    roleFullNames = new EnumMap<RoleType, String>(RoleType.class);

    serviceRoleMap.put(ServiceType.HDFS, Arrays.asList(RoleType.namenode,
            RoleType.datanode));
    serviceRoleMap.put(ServiceType.NDB, Arrays.asList(RoleType.ndbmtd,
            RoleType.mysqld, RoleType.ndb_mgmd));
    serviceRoleMap.put(ServiceType.YARN, Arrays.asList(RoleType.resourcemanager,
            RoleType.nodemanager));
    serviceRoleMap.put(ServiceType.MAP_REDUCE, Arrays.asList(RoleType.historyserver));
    serviceRoleMap.put(ServiceType.zookeeper, Arrays.asList(RoleType.zookeeper));
    serviceRoleMap.put(ServiceType.influxdb, Arrays.asList(RoleType.influxdb));
    serviceRoleMap.put(ServiceType.epipe, Arrays.asList(RoleType.epipe));
    serviceRoleMap.put(ServiceType.logstash, Arrays.asList(RoleType.logstash));
    serviceRoleMap.put(ServiceType.livy, Arrays.asList(RoleType.livy));
    serviceRoleMap.put(ServiceType.historyserver, Arrays.asList(RoleType.historyserver));
    serviceRoleMap.put(ServiceType.sparkhistoryserver, Arrays.asList(RoleType.sparkhistoryserver));
    serviceRoleMap.put(ServiceType.telegraf, Arrays.asList(RoleType.telegraf));
    serviceRoleMap.put(ServiceType.elasticsearch, Arrays.asList(RoleType.elasticsearch));
    serviceRoleMap.put(ServiceType.grafana, Arrays.asList(RoleType.grafana));
    serviceRoleMap.put(ServiceType.kafka, Arrays.asList(RoleType.kafka));
    serviceRoleMap.put(ServiceType.kibana, Arrays.asList(RoleType.kibana));
    
    roleFullNames.put(RoleType.namenode, "Name Node");
    roleFullNames.put(RoleType.datanode, "Data Node");
    roleFullNames.put(RoleType.ndbmtd, "MySQL Cluster NDB");
    roleFullNames.put(RoleType.mysqld, "MySQL Server");
    roleFullNames.put(RoleType.ndb_mgmd, "MGM Server");
    roleFullNames.put(RoleType.resourcemanager, "Resource Manager");
    roleFullNames.put(RoleType.nodemanager, "Node Manager");
    roleFullNames.put(RoleType.zookeeper, "Zookeeper");
    roleFullNames.put(RoleType.influxdb, "Influxdb");
    roleFullNames.put(RoleType.epipe, "Epipe");
    roleFullNames.put(RoleType.logstash, "Logstash");
    roleFullNames.put(RoleType.livy, "Livy");
    roleFullNames.put(RoleType.historyserver, "History server");
    roleFullNames.put(RoleType.sparkhistoryserver, "Spark history server");
    roleFullNames.put(RoleType.telegraf, "Telegraf");
    roleFullNames.put(RoleType.elasticsearch, "Elasticsearch");
    roleFullNames.put(RoleType.grafana, "Grafana");
    roleFullNames.put(RoleType.kafka, "Kafka");
    roleFullNames.put(RoleType.kibana, "Kibana");
  }

  public static List<RoleType> getRoles(ServiceType serviceType) {
    return serviceRoleMap.get(serviceType);
  }

  public static List<RoleType> getRoles(String service) {
    return getRoles(ServiceType.valueOf(service));
  }

  public static String[] getRolesArray(ServiceType serviceType) {

    List<RoleType> rolesList = serviceRoleMap.get(serviceType);
    String[] rolesArray = new String[rolesList.size()];
    for (int i = 0; i < rolesList.size(); i++) {
      rolesArray[i] = rolesList.get(i).toString();
    }
    return rolesArray;
  }

  public static String getRoleFullName(RoleType role) {
    return roleFullNames.get(role);
  }
}
