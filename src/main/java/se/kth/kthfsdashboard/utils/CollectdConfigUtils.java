package se.kth.kthfsdashboard.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.kth.kthfsdashboard.struct.CollectdPluginInstance;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class CollectdConfigUtils {

    private static final Logger logger = Logger.getLogger(CollectdConfigUtils.class.getName());
    private static final String START = "START";
    private static final String PLUGIN_DBI = "<plugin dbi>";
    private static final String QUERY = "query";
    private static final String QUERY_BEGIN = "<query";
    private static final String QUERY_END = "</query>";
    private static final String TYPE = "type";
    private static final String INSTANCE_PREFIX = "instanceprefix";
    private static final String STATEMENT = "statement";
    private static final String INSTANCE_FROM = "instancesfrom";
    private static final String DATABASE_BEGIN = "<database";
    private static final String DATABASE_END = "</database>";
    private static final String PLUGIN_JAVA = "<plugin java>";
    private static final String PLUGIN_GENERICJMX = "<plugin \"genericjmx\">";
    private static final String PLUGIN_END = "</Plugin>";
    private static final String MBEAN_BEGIN = "<mbean";
    private static final String MBEAN_END = "</mbean>";
    private static final String VALUE_BEGIN = "<value>";
    private static final String VALUE_END = "</value>";
    private static final String CONNECTION_BEGIN = "<connection>";
    private static final String CONNECTION_END = "</connection>";
    private static final String SERVICE = "#service";
    private static final String ROLE = "#role";
    private static final String COLLECT = "collect";

    public static Map<String, CollectdPluginInstance> parseDbiPlugin(String configFilePath) {
        String state = START;
        BufferedReader br = null;

        String line;
        String lineLowerCase;
        HashMap<String, CollectdPluginInstance> collectdPluginInstances = new HashMap<String, CollectdPluginInstance>();
        CollectdPluginInstance collectdPluginInstance = null;
        HashMap<String, String> types = new HashMap<String, String>();
        HashMap<String, String> typeInstances = new HashMap<String, String>();
        HashMap<String, String> statements = new HashMap<String, String>();
        HashMap<String, String> instanceFroms = new HashMap<String, String>();
        String currentQuery = "";
        String databaseName;

        try {

            br = new BufferedReader(new FileReader(configFilePath));
            while ((line = br.readLine()) != null) {
                line = line.trim().replaceAll(" +", " ");
                lineLowerCase = line.toLowerCase();
                if (state.equals(START)) {
                    if (lineLowerCase.equals(PLUGIN_DBI)) {
                        state = PLUGIN_DBI;
                    }
                } else if (state.equals(PLUGIN_DBI)) {
                    if (lineLowerCase.startsWith(QUERY_BEGIN)) {
                        state = QUERY_BEGIN;
                        currentQuery = parseParameter(line);
                    } else if (lineLowerCase.startsWith(DATABASE_BEGIN)) {
                        state = DATABASE_BEGIN;
                        databaseName = parseParameter(line);
                        collectdPluginInstance = new CollectdPluginInstance(databaseName);
                    }
                } else if (state.equals(QUERY_BEGIN)) {
                    if (lineLowerCase.startsWith(QUERY_END)) {
                        state = PLUGIN_DBI;
                    } else {
                        if (lineLowerCase.startsWith(TYPE)) {
                            types.put(currentQuery, parseValue(line));
                        } else if (lineLowerCase.startsWith(INSTANCE_PREFIX)) {
                            typeInstances.put(currentQuery, parseValue(line));
                        } else if (lineLowerCase.startsWith(INSTANCE_FROM)) {
                            instanceFroms.put(currentQuery, parseValue(line));
                        } else if (lineLowerCase.startsWith(STATEMENT)) {
                            statements.put(currentQuery, parseStatement(line));
                        }
                    }
                } else if (state.equals(DATABASE_BEGIN)) {
                    if (lineLowerCase.startsWith(DATABASE_END)) {
                        state = PLUGIN_DBI;
                        collectdPluginInstances.put(collectdPluginInstance.getName(), collectdPluginInstance);
                    } else {
                        if (lineLowerCase.startsWith(QUERY)) {
                            String query = parseValue(line);
                            String statement = statements.get(query);
                            String instanceFrom = instanceFroms.get(query);
                            if (statement.contains(instanceFrom + " IN (")) {
                                for (String from : statement.split(instanceFrom + " IN [(]")[1].split("[)]", 2)[0].split(",")) {
                                    collectdPluginInstance.add(types.get(query), typeInstances.get(query),
                                            "", from.trim().substring(1, from.length() - 2));
                                }
                            } else {
                                collectdPluginInstance.add(types.get(query), typeInstances.get(query),
                                        statement, instanceFroms.get(query));
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            collectdPluginInstances.clear();
            logger.log(Level.SEVERE, ex.toString());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.toString());
            }
        }
        return collectdPluginInstances;
    }

    public static HashMap<String, HashMap<String, CollectdPluginInstance>> parseJMXPlugin(String configFilePath) {

        HashMap<String, CollectdPluginInstance> collectdPluginInstances;
        HashMap<String, CollectdPluginInstance> allPluginInstancesMap = new HashMap<String, CollectdPluginInstance>();
        HashMap<String, HashMap<String, CollectdPluginInstance>> servicePluginInstancesMap = new HashMap<String, HashMap<String, CollectdPluginInstance>>();

        HashSet<String> collectSet = new HashSet<String>();

        String state = START;
        BufferedReader br = null;
        String line;
        String lineLowerCase;
        String type = "";
        String typeInstancePrefix = "";
        String service = "";
        String role = "";
        CollectdPluginInstance collectdPluginInstance = null;
        String mbean = "";
        try {
            br = new BufferedReader(new FileReader(configFilePath));
            while ((line = br.readLine()) != null) {
                line = line.trim().replaceAll(" +", " ");
                lineLowerCase = line.toLowerCase();

                if (state.equals(START)) {
                    if (lineLowerCase.equals(PLUGIN_JAVA)) {
                        state = PLUGIN_JAVA;
                    }
                } else if (state.equals(PLUGIN_JAVA)) {
                    if (lineLowerCase.equals(PLUGIN_GENERICJMX)) {
                        state = PLUGIN_GENERICJMX;
                    }
                } else if (state.equals(PLUGIN_GENERICJMX)) {
                    if (lineLowerCase.startsWith(MBEAN_BEGIN)) {
                        state = MBEAN_BEGIN;
                        mbean = parseParameter(line);

                    } else if (lineLowerCase.startsWith(CONNECTION_BEGIN)) {
                        state = CONNECTION_BEGIN;
                        collectSet.clear();
                    }
                } else if (state.equals(MBEAN_BEGIN)) {
                    if (lineLowerCase.startsWith(INSTANCE_PREFIX)) {
                        String pluginInstancePrefix = parseValue(line);
                        collectdPluginInstance = new CollectdPluginInstance(pluginInstancePrefix);
                    } else if (lineLowerCase.equals(VALUE_BEGIN)) {
                        state = VALUE_BEGIN;
                    } else if (lineLowerCase.equals(MBEAN_END)) {
                        state = PLUGIN_GENERICJMX;
                        allPluginInstancesMap.put(mbean, collectdPluginInstance);
                    }
                } else if (state.equals(VALUE_BEGIN)) {
                    if (lineLowerCase.startsWith(TYPE)) {
                        type = parseValue(line);
                    } else if (lineLowerCase.startsWith(INSTANCE_PREFIX)) {
                        typeInstancePrefix = parseValue(line);
                    } else if (lineLowerCase.equals(VALUE_END)) {
                        state = MBEAN_BEGIN;
                        collectdPluginInstance.add(type, typeInstancePrefix, null, null);
                    }
                } else if (state.equals(CONNECTION_BEGIN)) {

                    if (lineLowerCase.startsWith(SERVICE)) {
                        service = parseValue(line);
                    } else if (lineLowerCase.startsWith(ROLE)) {
                        role = parseValue(line);
                    } else if (lineLowerCase.startsWith(COLLECT)) {
                        collectSet.add(parseValue(line));

                    } else if (lineLowerCase.equals(CONNECTION_END)) {
                        state = PLUGIN_GENERICJMX;
                        collectdPluginInstances = new HashMap<String, CollectdPluginInstance>();
                        for (String mb : collectSet) {
                            collectdPluginInstances.put(allPluginInstancesMap.get(mb).getName(), allPluginInstancesMap.get(mb));
                        }
                        servicePluginInstancesMap.put(service.toUpperCase() + "-" + role.toUpperCase(), collectdPluginInstances);
                    }
                } else if (state.equals(PLUGIN_GENERICJMX)) {
                    if (lineLowerCase.equals(PLUGIN_END)) {
                        state = PLUGIN_JAVA;
                    }
                } else if (state.equals(PLUGIN_JAVA)) {
                    if (lineLowerCase.equals(PLUGIN_END)) {
                        state = START;
                    }
                }
            }
        } catch (IOException ex) {
            servicePluginInstancesMap.clear();
            System.err.println(ex);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }

        return servicePluginInstancesMap;
    }

    public static List<String> loadPluginsFromConfigFile(String configFilePath) {
        List<String> pluginsList = new ArrayList<String>();
        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(new FileReader(configFilePath));
            while ((line = br.readLine()) != null) {
                if (line.startsWith("LoadPlugin")
                        && !line.contains("rrdtool")
                        && !line.contains("logfile")
                        && !line.contains("syslog")
                        && !line.contains("network")) {
                    pluginsList.add(line.trim().replaceAll(" +", " ").split(" ")[1]);
                }
            }
        } catch (IOException ex) {
            pluginsList.clear();
            logger.log(Level.SEVERE, ex.toString());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.toString());
            }
        }
        return pluginsList;
    }

    private static String parseValue(String line) {
        String[] parts = line.split(" ");
        return parts[1].substring(1, parts[1].length() - 1);
    }

    private static String parseStatement(String line) {
        String[] parts = line.split(" ", 2);
        return parts[1].substring(1, parts[1].length() - 1);
    }

    private static String parseParameter(String line) {
        String[] parts = line.split(" ");
        return parts[1].substring(1, parts[1].length() - 2);

    }
}
