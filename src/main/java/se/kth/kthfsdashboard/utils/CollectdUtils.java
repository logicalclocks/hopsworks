package se.kth.kthfsdashboard.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.kth.kthfsdashboard.graph.Chart;
import se.kth.kthfsdashboard.graph.Graph;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class CollectdUtils {

    // TODO This should not be static     
    private static final String COLLECTD_PATH = "/var/lib/collectd/rrd/";
    private static final String RRD_EXT = ".rrd";
    private static final String COLLECTD_LINK = COLLECTD_PATH;
    private static final Logger logger = Logger.getLogger(CollectdUtils.class.getName());

    public static InputStream getGraphStream(Graph graph, String host, String n, int start, int end) throws IOException {

        HashMap<String, String> colorMap = new HashMap<String, String>();
        colorMap.put("RED", "CB4B4B");
        colorMap.put("BLUE", "AFD8F8");
        colorMap.put("YELLOW", "EDC240");
        colorMap.put("GREEN", "4DA74D");
        String colors[] = {};
        colors = colorMap.keySet().toArray(colors);
        int height = 130;
        int width = 260;
        RrdtoolCommand cmd = new RrdtoolCommand(COLLECTD_LINK, RRD_EXT, host, graph.getPlugin(), graph.getPluginInstance(), start, end);
        cmd.setGraphSize(width, height);
        cmd.setTitle(graph.getTitle());
        cmd.setVerticalLabel(graph.getVerticalLabel());

        for (Chart chart : graph.getCharts()) {
            String color;
            List<String> hosts;
            if (colorMap.containsKey(chart.getColor().toUpperCase())) {
                color = colorMap.get(chart.getColor().toUpperCase());
            } else {
                color = colorMap.get(colors[0]);
            }
            switch (chart.getModel()) {
                case LINE:
                    cmd.drawLine(chart.getType(), chart.getTypeInstance(), chart.getDs(), chart.getLabel(), color, chart.getFormat());
                    break;
                case AREA:
                    cmd.drawArea(chart.getType(), chart.getTypeInstance(), chart.getDs(), chart.getLabel(), color, chart.getFormat());
                    break;
                case AREA_STACK:
                    cmd.stackArea(chart.getType(), chart.getTypeInstance(), chart.getDs(), chart.getLabel(), color, chart.getFormat());
                    break;
                case LINES:
                    for (String value : n.split(",")) { // replace @n with n=1,2,3,...
                        String colorExp = chart.getColor().replaceAll("@n", value);
                        if (!colorMap.containsKey(colorExp)) {
                            int index = Integer.parseInt(colorExp.substring(chart.getColor().indexOf("(") + 1, chart.getColor().indexOf(")") - 1));
                            while (index >= colors.length) {
                                index -= colors.length;
                            }
                            color = colorMap.get(colors[index]);
                        }
                        cmd.drawLine(chart.getType().replaceAll("@n", value), chart.getTypeInstance().replaceAll("@n", value),
                                chart.getDs().replaceAll("@n", value), chart.getLabel().replaceAll("@n", value),
                                color, chart.getFormat());
                    }
                    break;
                case SUM_LINE:
                    hosts = new ArrayList<String>(Arrays.asList(host.split(",")));
                    cmd.drawSumLine(hosts, chart.getType(), chart.getTypeInstance(), chart.getDs(), chart.getLabel(), color, chart.getFormat());
                    break;
                case AVG_LINE:
                    hosts = new ArrayList<String>(Arrays.asList(host.split(",")));
                    cmd.drawAverageLine(hosts, chart.getType(), chart.getTypeInstance(), chart.getDs(), chart.getLabel(), color, chart.getFormat());
                    break;
            }
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
