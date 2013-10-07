package se.kth.kthfsdashboard.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
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

    public static Map<String, CollectdPluginInstance> parseDbiconfig(String dbiConfigPath) {
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

            br = new BufferedReader(new FileReader(dbiConfigPath));
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
