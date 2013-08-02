/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@Entity
@Table(name="ClusterEntities")
@NamedQueries({
    @NamedQuery(name = "ClustersEntity.findAll", query = "SELECT c FROM ClusterEntity c")
})
public class ClusterEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String clusterName;
    @Column(columnDefinition = "text")
    private String yamlContent;

    public ClusterEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getYamlContent() {
        return yamlContent;
    }

    public void setYamlContent(String yamlContent) {
        this.yamlContent = yamlContent;
    }

    public String showTrimContent() {
        System.out.println(yamlContent);
        StringBuilder builder = new StringBuilder();
        Pattern environment = Pattern.compile("environment: [a-zA-Z]+", Pattern.DOTALL);
        Pattern providerOptions =
                Pattern.compile("provider:[\\r\\n]+([\\s]+[a-zA-Z]+: ([a-zA-Z\\.0-9\\-\\_\\/\\']*||[\\s]+))*",
                Pattern.DOTALL);
        Pattern providerName = Pattern.compile("name: [a-zA-Z]+\\-[a-zA-Z]+[2]?", Pattern.DOTALL);
        Pattern providerRegion = Pattern.compile("region: [a-zA-Z\\.0-9\\-\\_\\/\\']*", Pattern.DOTALL);
        Matcher providerMatch = providerOptions.matcher(yamlContent);
        Matcher envMatch = environment.matcher(yamlContent);
        if (envMatch.find()) {
            builder.append(envMatch.group().replace("environment: ", ""));
        }
        builder.append(",");
        if (providerMatch.find()) {
            Matcher name = providerName.matcher(providerMatch.group());
            Matcher region = providerRegion.matcher(providerMatch.group());
            while (name.find()) {
                String option = name.group();
                System.out.println(option);
                builder.append(option.replaceAll("name: ", ""));
            }
            builder.append(",");
            while (region.find()) {
                String option = region.group();
                builder.append(option.replace("region: ", ""));
            }
        }
        return builder.toString();
    }
}
