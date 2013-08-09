package se.kth.kthfsdashboard.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.stamfest.rrd.CommandResult;
import net.stamfest.rrd.RRDp;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class CollectdUtils {

    private static final String COLLECTD_PATH = "/var/lib/collectd/rrd/";
    private static final String RRD_EXT = ".rrd";
    private static final String COLLECTD_LINK = COLLECTD_PATH;
    private static final Logger logger = Logger.getLogger(CollectdUtils.class.getName());

    enum ChartType {

        LINE, AREA, AREA_STACK
    }

    private enum GraphType {

        plane, loadall, memoryall, dfall, interfaceall, swapall,
        serv_nn_capacity, serv_nn_files, serv_nn_load, serv_nn_heartbeats, serv_nn_blockreplication,
        serv_nn_blocks, serv_nn_specialblocks, serv_nn_datanodes,
        serv_nn_r_fileinfo, serv_nn_r_getblocklocations, serv_nn_r_getlisting, serv_nn_r_getlinktarget, serv_nn_r_filesingetlisting,
        serv_nn_w_createfile, serv_nn_w_filescreated, serv_nn_w_createfile_all, serv_nn_w_filesappended, serv_nn_w_filesrenamed,
        serv_nn_w_deletefile, serv_nn_w_filesdeleted, serv_nn_w_deletefile_all, serv_nn_w_addblock, serv_nn_w_createsymlink,
        serv_nn_o_getadditionaldatanode, serv_nn_o_transactions, serv_nn_o_blockreport, serv_nn_o_syncs, serv_nn_o_transactionsbatchedinsync,
        serv_nn_t_safemodetime, serv_nn_t_transactionsavgtime, serv_nn_t_syncsavgtime, serv_nn_t_blockreportavgtime,
        nn_capacity, nn_files, nn_load, nn_heartbeats, nn_blockreplication, nn_blocks, nn_specialblocks, nn_datanodes,
        nn_r_fileinfo, nn_r_getblocklocations, nn_r_getlisting, nn_r_getlinktarget, nn_r_filesingetlisting,
        nn_w_createfile, nn_w_filescreated, nn_w_createfile_all, nn_w_filesappended, nn_w_filesrenamed,
        nn_w_deletefile, nn_w_filesdeleted, nn_w_deletefile_all, nn_w_addblock, nn_w_createsymlink,
        nn_o_getadditionaldatanode, nn_o_transactions, nn_o_blockreport, nn_o_syncs, nn_o_transactionsbatchedinsync,
        nn_t_safemodetime, nn_t_transactionsavgtime, nn_t_syncsavgtime, nn_t_blockreportavgtime,
        dd_heartbeats, dd_avgTimeHeartbeats, dd_bytes, dd_opsReads, dd_opsWrites, dd_blocksRead, dd_blocksWritten,
        dd_blocksRemoved, dd_blocksReplicated, dd_blocksVerified,
        dd_opsReadBlock, dd_opsWriteBlock, dd_opsCopyBlock, dd_opsReplaceBlock,
        dd_avgTimeReadBlock, dd_avgTimeWriteBlock, dd_avgTimeCopyBlock, dd_avgTimeReplaceBlock,
        dd_opsBlockChecksum, dd_opsBlockReports, dd_avgTimeBlockChecksum, dd_avgTimeBlockReports,
        dd_blockVerificationFailures, dd_volumeFailures,
        mysql_freeDataMemory, mysql_totalDataMemory, mysql_freeIndexMemory, mysql_totalIndexMemory,
        mysql_simpleReads, mysql_Reads, mysql_Writes, mysql_rangeScans, mysql_tableScans;
    }

    public static Set<String> pluginInstances(String hostId, String plugin) {
        // Reading from RRD files
        final String p = plugin;
        Set<String> instances = new TreeSet<String>();
        File dir = new File(COLLECTD_PATH + hostId);
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(p.toString());
            }
        });
        if (files == null) {
            return instances;
        }
        for (File file : files) {
            instances.add(file.getName().split("-")[1]);
        }
        return instances;
    }

    public static int pluginInstancesCount(String plugin, String hostId) {
        // Reading from RRD files
        return pluginInstances(plugin, hostId).size();
    }

    public static Set<String> typeInstances(String hostId, String plugin) {
        // Reading from RRD files
        SortedSet<String> instances = new TreeSet<String>();
        File dir = new File(COLLECTD_PATH + hostId + "/" + plugin);
        File[] files = dir.listFiles();        // Reading from RRD files
        if (files == null) {
            return instances;
        }
        for (File file : files) {
            instances.add(file.getName().split(RRD_EXT)[0].split("-")[1]);
        }
        return instances;
    }

    public static double[] getLastLoad(String hostId) {
        // Reading from RRD files        
        DecimalFormat format = new DecimalFormat("#.##");
        String loads[] = readLastRrdValue(hostId, "load", "", "load", "").split(":")[1].trim().toUpperCase().split(" ");
        double load[] = new double[3];
        try {
            load[0] = format.parse(loads[0]).doubleValue();
            load[1] = format.parse(loads[1]).doubleValue();
            load[2] = format.parse(loads[2]).doubleValue();
        } catch (ParseException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return load;
    }

    public static Long getLatestMemoryStatus(String hostId, String type) {
        // Reading from RRD files
        String res1 = readLastRrdValue(hostId, "memory", "", "memory", type);
        String result;
        if (res1.lastIndexOf(":") < 1) { // ERROR
            logger.log(Level.SEVERE, null, "RRD: " + res1);
            return -1l;
        } else {
            result = res1.split(":")[1].trim().toUpperCase();
        }

        try {
            return ParseUtils.parseLong(result);
        } catch (ParseException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private static String readLastRrdValue(String hostId, String plugin, String pluginInstance, String type, String typeInstance) {
        // Reading from RRD files        
        try {
            String pluginURI = pluginInstance.isEmpty() ? plugin : plugin + "-" + pluginInstance;
            String typeURI = typeInstance.isEmpty() ? type : type + "-" + typeInstance;
            RRDp rrd = new RRDp(COLLECTD_LINK + hostId + "/" + pluginURI + "/", "5555");

            //get latest recoded time
            CommandResult result = rrd.command(new String[]{"last", typeURI + RRD_EXT, "MAX"});
            String t = Long.toString(((long) Math.floor((Long.parseLong(result.output.trim()) / 10))) * 10 - 10);
            result = rrd.command(new String[]{"fetch", typeURI + RRD_EXT, "MIN", "-s", t, "-e", t});
            if (!result.ok) {
                System.err.println("ERROR in collectdTools: " + result.error);
                return result.error;
            } else {
                return result.output.split("\\r?\\n")[2];
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return "ERROR";
        }
    }

    public static InputStream getGraphStream(String chartType, String host, String plugin, String pluginInstance,
            String type, String typeInstance, String ds, int start, int end, int n) throws IOException {
        String RED = "CB4B4B";
        String BLUE = "AFD8F8";
        String YELLOW = "EDC240";
        String GREEN = "4DA74D";
        List<String> col = new LinkedList<String>();
        col.add(RED);
        col.add(GREEN);
        col.add(BLUE);
        col.add(YELLOW);
        int height = 130;
        int width = 260;
        try {
            GraphType.valueOf(chartType);
        } catch (Exception e) {
            chartType = "plane";
        }
        RrdtoolCommand cmd = new RrdtoolCommand(host, plugin, pluginInstance, start, end);
        cmd.setGraphSize(width, height);

        List<String> namenodes = new ArrayList<String>();
        String namenode = "";
        if (chartType.startsWith("serv_nn_")) {
            namenodes = new ArrayList<String>(Arrays.asList(host.split(",")));
            try {
                namenode = namenodes.get(0);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Cannot get the first item from namenodes. List is empty.");
            }
        }

        switch (GraphType.valueOf(chartType)) {
// - Hosts ---------------------------------------------------------------------         
            case loadall:
                cmd.setTitle("Load");
                cmd.setVerticalLabel(" ");
                cmd.drawLine(type, "", "longterm", "Longterm ", RED, "%5.2lf");
                cmd.drawLine(type, "", "midterm", "Midterm  ", BLUE, "%5.2lf");
                cmd.drawLine(type, "", "shortterm", "Shortterm", YELLOW, "%5.2lf");
                break;

            case memoryall:
                cmd.setTitle("Memory");
                cmd.setVerticalLabel("Byte");
                cmd.drawArea(type, "used", "value", "Used    ", RED, "%5.2lf %S");
                cmd.stackArea(type, "buffered", "value", "Buffered", BLUE, "%5.2lf %S");
                cmd.stackArea(type, "cached", "value", "Cached  ", YELLOW, "%5.2lf %S");
                cmd.stackArea(type, "free", "value", "Free    ", GREEN, "%5.2lf %S");
                break;

            case swapall:
                cmd.setTitle("Swap");
                cmd.setVerticalLabel("Byte");
                cmd.drawArea(type, "used", "value", "Used    ", RED, "%5.2lf %s");
                cmd.stackArea(type, "cached", "value", "Cached  ", YELLOW, "%5.2lf %s");
                cmd.stackArea(type, "free", "value", "Free    ", GREEN, "%5.2lf %s");
                break;

            case dfall:
                cmd.setTitle("Physical Memory");
                cmd.setVerticalLabel("Byte");
                cmd.drawArea(type, "root", "used", "Used", RED, "%5.2lf %S");
                cmd.stackArea(type, "root", "free", "Free", GREEN, "%5.2lf %S");
                break;

            case interfaceall:
                cmd.setTitle("Network Interface");
                cmd.setVerticalLabel("bps");
                cmd.drawLine(type, "eth0", "rx", "RX", YELLOW, "%5.2lf %S");
                cmd.drawLine(type, "eth0", "tx", "TX", BLUE, "%5.2lf %S");
                break;

//- Namenode Service -----------------------------------------------------------

            case serv_nn_capacity:
                cmd.setTitle("Namenode Capacity");
                cmd.setVerticalLabel("GB");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.setHostId(namenode);
                cmd.drawLine("memory-CapacityTotalGB", "", "value", "Total", BLUE, "%5.2lf %S");
                cmd.drawLine("memory-CapacityRemainingGB", "", "value", "Remaining", GREEN, "%5.2lf %S");
                cmd.drawLine("memory-CapacityUsedGB", "", "value", "Used", RED, "%5.2lf %S");
                break;

            case serv_nn_files:
                cmd.setTitle("Files");
                cmd.setVerticalLabel(" ");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.setHostId(namenode);
                cmd.drawLine("gauge-FilesTotal", "", "value", "Total Files", GREEN, "%5.2lf %S");
                break;

            case serv_nn_load:
                cmd.setTitle("Total Load");
                cmd.setVerticalLabel(" ");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.setHostId(namenode);
                cmd.drawLine("gauge-TotalLoad", "", "value", "Total Load", RED, "%5.2lf %S");
                break;

            case serv_nn_heartbeats:
                cmd.setTitle("Heartbeats");
                cmd.setVerticalLabel("heartbeats/s");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.setHostId(namenode);
                cmd.drawLine("counter-ExpiredHeartbeats", "", "value", "Expired Heartbeats", RED, "%5.2lf %S");
                break;

            case serv_nn_blockreplication:
                cmd.setTitle("Block Replication");
                cmd.setVerticalLabel(" ");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.setHostId(namenode);
                cmd.drawLine("counter-UnderReplicatedBlocks", "", "value", "Under-Replicated", GREEN, "%5.2lf %S");
                cmd.drawLine("counter-PendingReplicationBlocks", "", "value", "Pending", BLUE, "%5.2lf %S");
                cmd.drawLine("counter-ScheduledReplicationBlocks", "", "value", "Scheduled", YELLOW, "%5.2lf %S");
                break;

            case serv_nn_blocks:
                cmd.setTitle("Blocks");
                cmd.setVerticalLabel(" ");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.setHostId(namenode);
                cmd.drawLine("counter-BlockCapacity", "", "value", "Blocks Total", GREEN, "%5.2lf %S");
                cmd.drawLine("counter-BlocksTotal", "", "value", "Block Capacity", BLUE, "%5.2lf %S");
                break;

            case serv_nn_specialblocks:
                cmd.setTitle("Special Blocks");
                cmd.setVerticalLabel(" ");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.setHostId(namenode);
                cmd.drawLine("counter-CorruptBlocks", "", "value", "Corrupt", RED, "%5.2lf %S");
                cmd.drawLine("counter-ExcessBlocks", "", "value", "Excess", BLUE, "%5.2lf %S");
                cmd.drawLine("counter-MissingBlocks", "", "value", "Missing", YELLOW, "%5.2lf %S");
                cmd.drawLine("counter-PendingDeletionBlocks", "", "value", "Pending Delete", GREEN, "%5.2lf %S");
                break;

            case serv_nn_datanodes:
                cmd.setTitle("Number of Data Nodes");
                cmd.setVerticalLabel(" ");
                cmd.setPlugin("GenericJMX-FSNamesystemState", "");
                cmd.setHostId(namenode);
                cmd.drawLine("gauge-NumDeadDataNodes", "", "value", "Dead", RED, "%5.2lf %S");
                cmd.drawLine("gauge-NumLiveDataNodes", "", "value", "Live", GREEN, "%5.2lf %S");
                break;

//- Namenode Service Activities------------------------------------------------------------------            

            case serv_nn_r_fileinfo:
                cmd.setTitle("Number of FileInfo operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-FileInfoOps", "", "value", "FileInfo", RED, "%5.2lf %S");
                break;

            case serv_nn_r_getblocklocations:
                cmd.setTitle("Number of GetBlockLocations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-GetBlockLocations", "", "value", "GetBlockLocations", RED, "%5.2lf %S");
                break;

            case serv_nn_r_getlisting:
                cmd.setTitle("Number of GetListing operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-GetListingOps", "", "value", "GetListing", RED, "%5.2lf %S");
                break;

            case serv_nn_r_getlinktarget:
                cmd.setTitle("Number of GetLinkTarget operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-GetLinkTargetOps", "", "value", "GetLinkTarget", RED, "%5.2lf %S");
                break;

            case serv_nn_r_filesingetlisting:
                cmd.setTitle("Number of FilesInGetListing operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-FilesInGetListingOps", "", "value", "FilesInGetListing", RED, "%5.2lf %S");
                break;

            case serv_nn_w_createfile:
                cmd.setTitle("Number of CreateFile operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-CreateFileOps", "", "value", "CreateFile", RED, "%5.2lf %S");
                break;

            case serv_nn_w_filescreated:
                cmd.setTitle("Number of Files Created");
                cmd.setVerticalLabel("files/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-FilesCreated", "", "value", "Files Created", RED, "%5.2lf %S");
                break;

            case serv_nn_w_createfile_all:
                cmd.setTitle("CreateFile operations");
                cmd.setVerticalLabel("/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-CreateFileOps", "", "value", "CreateFile Operations", RED, "%5.2lf %S");
                cmd.drawSummedLines(namenodes, "counter-FilesCreated", "", "value", "Files Created", BLUE, "%5.2lf %S");
                break;

            case serv_nn_w_filesappended:
                cmd.setTitle("Number of Files Appended");
                cmd.setVerticalLabel("files/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-FilesAppended", "", "value", "Files Appended", RED, "%5.2lf %S");
                break;

            case serv_nn_w_filesrenamed:
                cmd.setTitle("Number of Files Renamed");
                cmd.setVerticalLabel("files/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-FilesRenamed", "", "value", "Files Renamed", RED, "%5.2lf %S");
                break;

            case serv_nn_w_deletefile:
                cmd.setTitle("Number of Delete File Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-DeleteFileOps", "", "value", "DeleteFile", RED, "%5.2lf %S");
                break;

            case serv_nn_w_filesdeleted:
                cmd.setTitle("Number of Files Deleted");
                cmd.setVerticalLabel("files/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-FilesDeleted", "", "value", "Files Deleted", RED, "%5.2lf %S");
                break;

            case serv_nn_w_deletefile_all:
                cmd.setTitle("Delete File Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-DeleteFileOps", "", "value", "DeleteFile Operations", RED, "%5.2lf %S");
                cmd.drawSummedLines(namenodes, "counter-FilesDeleted", "", "value", "Files Deleted", BLUE, "%5.2lf %S");
                break;

            case serv_nn_w_addblock:
                cmd.setTitle("AddBlock Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-AddBlockOps", "", "value", "AddBlock Operations", RED, "%5.2lf %S");
                break;

            case serv_nn_w_createsymlink:
                cmd.setTitle("CreateSymlink Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-CreateSymlinkOps", "", "value", "CreateSymlink Operations", RED, "%5.2lf %S");
                break;

            case serv_nn_o_getadditionaldatanode:
                cmd.setTitle("GetAdditionalDatanode Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-GetAdditionalDatanodeOps", "", "value", "GetAdditionalDatanode Operations", RED, "%5.2lf %S");
                break;

            case serv_nn_o_transactions:
                cmd.setTitle("Number of Transaction Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-TransactionsNumOps", "", "value", "Transactions Operations", RED, "%5.2lf %S");
                break;

            case serv_nn_o_transactionsbatchedinsync:
                cmd.setTitle("Number of Transactions Batched In Sync");
                cmd.setVerticalLabel(" transactions/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-TransactionsBatchedInSync", "", "value", "Transactions Batched In Sync", RED, "%5.2lf %S");
                break;

            case serv_nn_o_blockreport:
                cmd.setTitle("Number of BlockReport Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-BlockReportNumOps", "", "value", "BlockReport Operations", RED, "%5.2lf %S");
                break;

            case serv_nn_o_syncs:
                cmd.setTitle("Number of Syncs");
                cmd.setVerticalLabel("syncs/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "counter-SyncsNumOps", "", "value", "Syncs", RED, "%5.2lf %S");
                break;

            case serv_nn_t_blockreportavgtime:
                cmd.setTitle("BlockReport Avg Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "gauge-BlockReportAvgTime", "", "value", "BlockReport Avg Time", RED, "%5.2lf %S");
                break;

            case serv_nn_t_transactionsavgtime:
                cmd.setTitle("Transactions Avg Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "gauge-TransactionsAvgTime", "", "value", "Transactions Avg Time", RED, "%5.2lf %S");
                break;

            case serv_nn_t_syncsavgtime:
                cmd.setTitle("Syncs Avg Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "gauge-SyncsAvgTime", "", "value", "Syncs Avg Time", RED, "%5.2lf %S");
                break;

            case serv_nn_t_safemodetime:
                cmd.setTitle("SafeMode Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawSummedLines(namenodes, "gauge-SafeModeTime", "", "value", "SafeMode Time", RED, "%5.2lf %S");
                break;

//- Namenode Instance -----------------------------------------------------------

            case nn_capacity:
                cmd.setTitle("Namenode Capacity");
                cmd.setVerticalLabel("GB");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.drawLine("memory-CapacityTotalGB", "", "value", "Total", BLUE, "%5.2lf %S");
                cmd.drawLine("memory-CapacityRemainingGB", "", "value", "Remaining", GREEN, "%5.2lf %S");
                cmd.drawLine("memory-CapacityUsedGB", "", "value", "Used", RED, "%5.2lf %S");
                break;

            case nn_files:
                cmd.setTitle("Files");
                cmd.setVerticalLabel(" ");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.drawLine("gauge-FilesTotal", "", "value", "Total Files", GREEN, "%5.2lf %S");
                break;

            case nn_load:
                cmd.setTitle("Total Load");
                cmd.setVerticalLabel(" ");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.drawLine("gauge-TotalLoad", "", "value", "Total Load", RED, "%5.2lf %S");
                break;

            case nn_heartbeats:
                cmd.setTitle("Heartbeats");
                cmd.setVerticalLabel("heartbeats/s");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.drawLine("counter-ExpiredHeartbeats", "", "value", "Expired Heartbeats", RED, "%5.2lf %S");
                break;

            case nn_blockreplication:
                cmd.setTitle("Block Replication");
                cmd.setVerticalLabel("blocks/s");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.drawLine("counter-UnderReplicatedBlocks", "", "value", "Under-Replicated", GREEN, "%5.2lf %S");
                cmd.drawLine("counter-PendingReplicationBlocks", "", "value", "Pending", BLUE, "%5.2lf %S");
                cmd.drawLine("counter-ScheduledReplicationBlocks", "", "value", "Scheduled", YELLOW, "%5.2lf %S");
                break;

            case nn_blocks:
                cmd.setTitle("Blocks");
                cmd.setVerticalLabel(" ");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.setHostId(host);
                cmd.drawLine("counter-BlockCapacity", "", "value", "Blocks Total", GREEN, "%5.2lf %S");
                cmd.drawLine("counter-BlocksTotal", "", "value", "Block Capacity", BLUE, "%5.2lf %S");
                break;

            case nn_specialblocks:
                cmd.setTitle("Special Blocks");
                cmd.setVerticalLabel("blocks/s");
                cmd.setPlugin("GenericJMX-FSNamesystem", "");
                cmd.setHostId(host);
                cmd.drawLine("counter-CorruptBlocks", "", "value", "Corrupt", RED, "%5.2lf %S");
                cmd.drawLine("counter-ExcessBlocks", "", "value", "Excess", BLUE, "%5.2lf %S");
                cmd.drawLine("counter-MissingBlocks", "", "value", "Missing", YELLOW, "%5.2lf %S");
                cmd.drawLine("counter-PendingDeletionBlocks", "", "value", "Pending Delete", GREEN, "%5.2lf %S");
                break;

            case nn_datanodes:
                cmd.setTitle("Number of Data Nodes");
                cmd.setVerticalLabel(" ");
                cmd.setPlugin("GenericJMX-FSNamesystemState", "");
                cmd.drawLine("gauge-NumDeadDataNodes", "", "value", "Dead", RED, "%5.2lf %S");
                cmd.drawLine("gauge-NumLiveDataNodes", "", "value", "Live", GREEN, "%5.2lf %S");
                break;

//- Namenode Instance Activities-------------------------------------------------

            case nn_r_fileinfo:
                cmd.setTitle("Number of FileInfo operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-FileInfoOps", "", "value", "FileInfo", RED, "%5.2lf %S");
                break;

            case nn_r_getblocklocations:
                cmd.setTitle("Number of GetBlockLocations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-GetBlockLocations", "", "value", "GetBlockLocations", RED, "%5.2lf %S");
                break;

            case nn_r_getlisting:
                cmd.setTitle("Number of GetListing operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-GetListingOps", "", "value", "GetListing", RED, "%5.2lf %S");
                break;

            case nn_r_getlinktarget:
                cmd.setTitle("Number of GetLinkTarget operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-GetLinkTargetOps", "", "value", "GetLinkTarget", RED, "%5.2lf %S");
                break;

            case nn_r_filesingetlisting:
                cmd.setTitle("Number of FilesInGetListing operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-FilesInGetListingOps", "", "value", "FilesInGetListing", RED, "%5.2lf %S");
                break;

            case nn_w_createfile:
                cmd.setTitle("Number of CreateFile operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-CreateFileOps", "", "value", "CreateFile", RED, "%5.2lf %S");
                break;

            case nn_w_filescreated:
                cmd.setTitle("Number of Files Created");
                cmd.setVerticalLabel(" ");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-FilesCreated", "", "value", "Files Created", RED, "%5.2lf %S");
                break;

            case nn_w_createfile_all:
                cmd.setTitle("CreateFile operations");
                cmd.setVerticalLabel("/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-CreateFileOps", "", "value", "CreateFile Operations", RED, "%5.2lf %S");
                cmd.drawLine("counter-FilesCreated", "", "value", "Files Created", BLUE, "%5.2lf %S");
                break;

            case nn_w_filesappended:
                cmd.setTitle("Number of Files Appended");
                cmd.setVerticalLabel("files/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-FilesAppended", "", "value", "Files Appended", RED, "%5.2lf %S");
                break;

            case nn_w_filesrenamed:
                cmd.setTitle("Number of Files Renamed");
                cmd.setVerticalLabel("/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-FilesRenamed", "", "value", "Files Renamed", RED, "%5.2lf %S");
                break;

            case nn_w_deletefile:
                cmd.setTitle("Number of Delete File Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-DeleteFileOps", "", "value", "DeleteFile", RED, "%5.2lf %S");
                break;

            case nn_w_filesdeleted:
                cmd.setTitle("Number of Files Deleted");
                cmd.setVerticalLabel("files/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-FilesDeleted", "", "value", "Files Deleted", RED, "%5.2lf %S");
                break;

            case nn_w_deletefile_all:
                cmd.setTitle("Delete File Operations");
                cmd.setVerticalLabel("/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-DeleteFileOps", "", "value", "DeleteFile Operations", RED, "%5.2lf %S");
                cmd.drawLine("counter-FilesDeleted", "", "value", "Files Deleted", BLUE, "%5.2lf %S");
                break;

            case nn_w_addblock:
                cmd.setTitle("AddBlock Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-AddBlockOps", "", "value", "AddBlock Operations", RED, "%5.2lf %S");
                break;

            case nn_w_createsymlink:
                cmd.setTitle("CreateSymlink Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-CreateSymlinkOps", "", "value", "CreateSymlink Operations", RED, "%5.2lf %S");
                break;

            case nn_o_getadditionaldatanode:
                cmd.setTitle("GetAdditionalDatanode Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-GetAdditionalDatanodeOps", "", "value", "GetAdditionalDatanode Operations", RED, "%5.2lf %S");
                break;

            case nn_o_transactions:
                cmd.setTitle("Number of Transaction Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-TransactionsNumOps", "", "value", "Transactions Operations", RED, "%5.2lf %S");
                break;

            case nn_o_transactionsbatchedinsync:
                cmd.setTitle("Number of Transactions Batched In Sync");
                cmd.setVerticalLabel("transactions/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-TransactionsBatchedInSync", "", "value", "Transactions Batched In Sync", RED, "%5.2lf %S");
                break;

            case nn_o_blockreport:
                cmd.setTitle("Number of BlockReport Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-BlockReportNumOps", "", "value", "BlockReport Operations", RED, "%5.2lf %S");
                break;

            case nn_o_syncs:
                cmd.setTitle("Number of Syncs");
                cmd.setVerticalLabel("sync/s");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("counter-SyncsNumOps", "", "value", "Syncs", RED, "%5.2lf %S");
                break;

            case nn_t_blockreportavgtime:
                cmd.setTitle("BlockReport Avg Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("gauge-BlockReportAvgTime", "", "value", "BlockReport Avg Time", RED, "%5.2lf %S");
                break;

            case nn_t_transactionsavgtime:
                cmd.setTitle("Transactions Avg Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("gauge-TransactionsAvgTime", "", "value", "Transactions Avg Time", RED, "%5.2lf %S");
                break;

            case nn_t_syncsavgtime:
                cmd.setTitle("Syncs Avg Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("gauge-SyncsAvgTime", "", "value", "Syncs Avg Time", RED, "%5.2lf %S");
                break;

            case nn_t_safemodetime:
                cmd.setTitle("SafeMode Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-NameNodeActivity", "");
                cmd.drawLine("gauge-SafeModeTime", "", "value", "SafeMode Time", RED, "%5.2lf %S");
                break;

//- Datanode Instance Activities------------------------------------------------

            case dd_heartbeats:
                cmd.setTitle("Number of Heartbeats");
                cmd.setVerticalLabel("heartbeats/s");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("counter-HeartbeatsNumOps", "", "value", "Heartbeats", GREEN, "%5.2lf %S");
                break;

            case dd_avgTimeHeartbeats:
                cmd.setTitle("Heartbeats Average Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-HeartbeatsAvgTime", "", "value", "Heartbeats Average Time", GREEN, "%5.2lf %S");
                break;

            case dd_bytes:
                cmd.setTitle("Bytes Read/Written");
                cmd.setVerticalLabel("bytes");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-BytesRead", "", "value", "Read", BLUE, "%5.2lf %S");
                cmd.drawLine("gauge-BytesWritten", "", "value", "Written", GREEN, "%5.2lf %S");
                break;

            case dd_opsReads:
                cmd.setTitle("Read Operations");
                cmd.setVerticalLabel("ops");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-ReadsFromLocalClient", "", "value", "Local Client", BLUE, "%5.2lf %S");
                cmd.drawLine("gauge-ReadsFromRemoteClient", "", "value", "Remote Client", GREEN, "%5.2lf %S");
                break;

            case dd_opsWrites:
                cmd.setTitle("Write Operations");
                cmd.setVerticalLabel("ops");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-WritesFromLocalClient", "", "value", "Local Client", BLUE, "%5.2lf %S");
                cmd.drawLine("gauge-WritesFromRemoteClient", "", "value", "Remote Client", GREEN, "%5.2lf %S");
                break;

            case dd_blocksRead:
                cmd.setTitle("Blocks Read");
                cmd.setVerticalLabel("blocks");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-BlocksRead", "", "value", "Blocks Read", GREEN, "%5.2lf %S");
                break;

            case dd_blocksWritten:
                cmd.setTitle("Blocks Written");
                cmd.setVerticalLabel("blocks");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-BlocksWritten", "", "value", "Blocks Written", GREEN, "%5.2lf %S");
                break;

            case dd_blocksRemoved:
                cmd.setTitle("Blocks Removed");
                cmd.setVerticalLabel("blocks");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-BlocksRemoved", "", "value", "Blocks Removed", GREEN, "%5.2lf %S");
                break;

            case dd_blocksReplicated:
                cmd.setTitle("Blocks Replicated");
                cmd.setVerticalLabel("blocks");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-BlocksReplicated", "", "value", "Blocks Replicated", GREEN, "%5.2lf %S");
                break;

            case dd_blocksVerified:
                cmd.setTitle("Blocks Verified");
                cmd.setVerticalLabel("blocks");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-BlocksVerified", "", "value", "Blocks Verified", GREEN, "%5.2lf %S");
                break;

            case dd_opsReadBlock:
                cmd.setTitle("Read Block Operations");
                cmd.setVerticalLabel("ops");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-ReadBlockOpNumOps", "", "value", "Read Block Operations", GREEN, "%5.2lf %S");
                break;

            case dd_opsWriteBlock:
                cmd.setTitle("Write Block Operations");
                cmd.setVerticalLabel("ops");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-WriteBlockOpNumOps", "", "value", "Write Block Operations", GREEN, "%5.2lf %S");
                break;

            case dd_opsCopyBlock:
                cmd.setTitle("Copy Block Operations");
                cmd.setVerticalLabel("ops/s");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("counter-CopyBlockOpNumOps", "", "value", "Copy Block Operations", GREEN, "%5.2lf %S");
                break;

            case dd_opsReplaceBlock:
                cmd.setTitle("Replace Block Operations");
                cmd.setVerticalLabel("ops");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-ReplaceBlockOpNumOps", "", "value", "Replace Block Operations", GREEN, "%5.2lf %S");
                break;

            case dd_avgTimeReadBlock:
                cmd.setTitle("Read Block Average Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-ReadBlockOpAvgTime", "", "value", "Read Block Average Time", GREEN, "%5.2lf %S");
                break;

            case dd_avgTimeWriteBlock:
                cmd.setTitle("Write Block Average Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-WriteBlockOpAvgTime", "", "value", "Write Block Average Time", GREEN, "%5.2lf %S");
                break;

            case dd_avgTimeCopyBlock:
                cmd.setTitle("Copy Block Average Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-CopyBlockOpAvgTime", "", "value", "Copy Block Average Time", GREEN, "%5.2lf %S");
                break;

            case dd_avgTimeReplaceBlock:
                cmd.setTitle("Replace Block Average Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-ReplaceBlockOpAvgTime", "", "value", "Replace Block Average Time", GREEN, "%5.2lf %S");
                break;

            case dd_opsBlockChecksum:
                cmd.setTitle("Block Checksum Operations");
                cmd.setVerticalLabel("ops");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-BlockChecksumOpNumOps", "", "value", "Block Checksum Operations", GREEN, "%5.2lf %S");
                break;

            case dd_opsBlockReports:
                cmd.setTitle("Block Report Operations");
                cmd.setVerticalLabel("ops");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-BlockReportsNumOps", "", "value", "Block Report Operations", GREEN, "%5.2lf %S");
                break;

            case dd_avgTimeBlockChecksum:
                cmd.setTitle("Block Checksum Average Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-BlockChecksumOpAvgTime", "", "value", "Block Checksum Average Time", GREEN, "%5.2lf %S");
                break;

            case dd_avgTimeBlockReports:
                cmd.setTitle("Block Report Average Time");
                cmd.setVerticalLabel("sec");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-BlockReportsAvgTime", "", "value", "Block Report Average Time", GREEN, "%5.2lf %S");
                break;

            case dd_blockVerificationFailures:
                cmd.setTitle("Block Verification Failures");
                cmd.setVerticalLabel("ops");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-BlockVerificationFailures", "", "value", "Block Verification Failures", GREEN, "%5.2lf %S");
                break;

            case dd_volumeFailures:
                cmd.setTitle("Volume Failures");
                cmd.setVerticalLabel("failures");
                cmd.setPlugin("GenericJMX-DataNodeActivity", "");
                cmd.drawLine("gauge-VolumeFailures", "", "value", "Volume Failures", GREEN, "%5.2lf %S");
                break;

//- Mysqlcluster ---------------------------------------------------------------

            case mysql_freeDataMemory:
                cmd.setTitle("Free Data Memory");
                cmd.setVerticalLabel("bytes");
                cmd.setPlugin("dbi-ndbinfo", "");
                for (int i = 1; i <= n; i++) {
                    cmd.drawLine("gauge-free_data_memory-" + i, "", "value", "Node " + i, col.get(i - 1), "%5.2lf %S");
                }
                break;

            case mysql_totalDataMemory:
                cmd.setTitle("Total Data Memory");
                cmd.setVerticalLabel("bytes");
                cmd.setPlugin("dbi-ndbinfo", "");
                for (int i = 1; i <= n; i++) {
                    cmd.drawLine("gauge-total_data_memory-" + i, "", "value", "Node " + i, col.get(i - 1), "%5.2lf %S");
                }
                break;

            case mysql_freeIndexMemory:
                cmd.setTitle("Free Index Memory");
                cmd.setVerticalLabel("bytes");
                cmd.setPlugin("dbi-ndbinfo", "");
                for (int i = 1; i <= n; i++) {
                    cmd.drawLine("gauge-free_index_memory-" + i, "", "value", "Node " + i, col.get(i - 1), "%5.2lf %S");
                }
                break;

            case mysql_totalIndexMemory:
                cmd.setTitle("Total Index Memory");
                cmd.setVerticalLabel("bytes");
                cmd.setPlugin("dbi-ndbinfo", "");
                for (int i = 1; i <= n; i++) {
                    cmd.drawLine("gauge-total_index_memory-" + i, "", "value", "Node " + i, col.get(i - 1), "%5.2lf %S");
                }
                break;

            case mysql_simpleReads:
                cmd.setTitle("Simple Reads");
                cmd.setVerticalLabel("reads/s");
                cmd.setPlugin("dbi-ndbinfo", "");
                cmd.drawLine("derive-rate_counters_sum-SIMPLE_READS", "", "value", "Simple Reads", GREEN, "%5.2lf %S");
                break;

            case mysql_Reads:
                cmd.setTitle("Reads");
                cmd.setVerticalLabel("reads/s ");
                cmd.setPlugin("dbi-ndbinfo", "");
                cmd.drawLine("derive-rate_counters_sum-READS", "", "value", "Reads", GREEN, "%5.2lf %S");
                break;

            case mysql_Writes:
                cmd.setTitle("Writes");
                cmd.setVerticalLabel("writes/s");
                cmd.setPlugin("dbi-ndbinfo", "");
                cmd.drawLine("derive-rate_counters_sum-WRITES", "", "value", "Writes", GREEN, "%5.2lf %S");
                break;

            case mysql_rangeScans:
                cmd.setTitle("Range Scans");
                cmd.setVerticalLabel("scans/s");
                cmd.setPlugin("dbi-ndbinfo", "");
                cmd.drawLine("derive-rate_counters_sum-RANGE_SCANS", "", "value", "Range Scans", GREEN, "%5.2lf %S");
                break;

            case mysql_tableScans:
                cmd.setTitle("Table Scans");
                cmd.setVerticalLabel("scans/s");
                cmd.setPlugin("dbi-ndbinfo", "");
                cmd.drawLine("derive-rate_counters_sum-TABLE_SCANS", "", "value", "Table Scans", GREEN, "%5.2lf %S");
                break;

            default:
                cmd.setTitle(plugin);
                cmd.setVerticalLabel(plugin);
                cmd.drawLine(type, typeInstance, ds, typeInstance, GREEN, null);
        }

        Process process = new ProcessBuilder(cmd.getCommands()).directory(new File("/usr/bin/"))
                .redirectErrorStream(true).start();
        try {
            process.waitFor();
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
            return null;
        }
        return process.getInputStream();
    }
}
