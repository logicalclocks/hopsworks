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

    @Override
    protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
            Object propertyValue, Tag customTag) {
        String name = property.getName();
        if ((name.equals("recipes") || name.equals("authorizePorts")
                || name.equals("chefAttributes") || name.equals("zones") || name.equals("loginUser")
                ||name.equals("bittorrent")||name.equals("git")||name.equals("user")||name.equals("repository")
                ||name.equals("key")||name.equals("nodes"))
                && (propertyValue == null||"".equals(propertyValue)||"{}".equals(propertyValue)
                ||"[]".equals(propertyValue))) {
            return null;
        } else if(name.equals("environment")&&propertyValue==null){
            return super.representJavaBeanProperty(javaBean, property, "prod", customTag);
        } else {
            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
        }
    }
}