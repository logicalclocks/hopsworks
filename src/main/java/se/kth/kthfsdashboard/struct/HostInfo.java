package se.kth.kthfsdashboard.struct;

import java.io.Serializable;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */


public class HostInfo implements Serializable {

    private String name;
    private String ip;
    private String rack;

    public HostInfo(String name, String ip, String rack) {

        this.name = name;
        this.ip = ip;
        this.rack = rack;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public String getRack() {
        return rack;
    }

}
