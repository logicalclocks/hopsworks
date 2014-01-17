/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;

import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * A Cluster Representer for the YAML parser. This will make the parser to
 * ignore null properties when dumping the file.
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class ClusterRepresenter extends Representer {

    private String provider;

    public ClusterRepresenter() {
    }

    public ClusterRepresenter(String provider) {
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
            Object propertyValue, Tag customTag) {
        String name = property.getName();
        if ((name.equals("recipes") || name.equals("authorizePorts")
                || name.equals("chefAttributes") || name.equals("zones") || name.equals("loginUser")
                || name.equals("bittorrent") || name.equals("git") || name.equals("user") || name.equals("repository")
                || name.equals("key") || name.equals("nodes"))
                && (propertyValue == null || "".equals(propertyValue) || "{}".equals(propertyValue)
                || "[]".equals(propertyValue))) {
            return null;
        } else if (name.equals("instanceType") && propertyValue == null) {
            if (provider.equals("aws-ec2")) {
                return super.representJavaBeanProperty(javaBean, property, "m1.large", customTag);
            } else {
                return null;
            }
        } else if (name.equals("image") && propertyValue == null) {
            if (provider.equals("aws-ec2")) {
                return super.representJavaBeanProperty(javaBean, property, "eu-west-1/ami-ffcdce8b", customTag);
            } else {
                return null;
            }
        } else if (name.equals("region") && propertyValue == null) {
            if (provider.equals("aws-ec2")) {
                return super.representJavaBeanProperty(javaBean, property, "eu-west-1", customTag);
            } else {
                return null;
            }
        } else if (name.equals("loginUser") && propertyValue == null) {
            if (provider.equals("aws-ec2")) {
                return super.representJavaBeanProperty(javaBean, property, "ubuntu", customTag);
            } else {
                return null;
            }
        } else if (name.equals("installPhase") && propertyValue == null) {
            return super.representJavaBeanProperty(javaBean, property, "true", customTag);
        } else {
            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
        }
    }
}