package se.kth.kthfsdashboard.log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class TempLogStore {

    private static ConcurrentHashMap<String, HashSet<Log>> logStore;
    private HashMap<String, Integer> targetCount;

    public TempLogStore() {
        logStore = new ConcurrentHashMap<String, HashSet<Log>>();
        targetCount = new HashMap<String, Integer>();
        targetCount.put("memory.memory", 4);    // used, cached, free, buffered
        targetCount.put("swap.swap", 3);        // used, caches, free
        targetCount.put("cpu.cpu", 8);          // idle, steal, softirq, interrupt, user, system, wait, nice 
        targetCount.put("load.load", 1);        // -
        
    }

    private String getKey(Log log) {

        return log.getTime() + "." + log.getHost() + "." + log.getPlugin() + "." + log.getPluginInstance() + "." + log.getType();
    }

    private String getPlugingType(Log log) {

        return log.getPlugin() + "." + log.getType();
    }

    public void addLog(Log log) {

        // Get logs for log.time, log.plugin, log.pluginInstance, log.type
        // Add log to the set, and Store the new set

        HashSet<Log> logSet = logStore.get(getKey(log));
        if (logSet == null) {
            logSet = new HashSet<Log>();
        }
        logSet.add(log);
        logStore.put(getKey(log), logSet);
    }

    public HashSet<Log> removeLogs(Log log) {
        // remove all logs with log.time time
        // return removed logs

//        System.out.println("before: " + getSize());

        HashSet<Log> logSet = logStore.get(getKey(log));
//        System.out.println("got: " + logSet.size());
        logStore.remove(getKey(log));
//        System.out.println("after: " + getSize());
        return logSet;
    }

    public int getSize() {

        return logStore.size();
    }

    public boolean isTargetCountSatisfied(Log log) {

        HashSet<Log> logSet = logStore.get(getKey(log));

        if (logSet == null) {
            return false;
        }
        if (logSet.size() == targetCount.get(getPlugingType(log))) {

            return true;
        }
        return false;
    }

    public boolean isPluginTypeValid(Log log) {

        if (targetCount.containsKey(log.getPlugin() + "." + log.getType())) {
            return true;
        }

        return false;
    }
}
