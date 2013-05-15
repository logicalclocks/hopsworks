/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;
import junit.framework.TestCase;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class ClusterParserTest extends TestCase {
    public static void main(String[] args) {
        Yaml yaml = new Yaml();
        final String dir = System.getProperty("user.dir");
        final String separator = System.getProperty("file.separator");


        try {
            Object document = yaml.load(new BufferedReader(new FileReader(new File(dir + separator + "ClusterDraft.yml"))));
            assertNotNull(document);
            assertTrue(document.getClass().toString(), document instanceof Cluster);
            Cluster cluster =(Cluster) document;
            System.out.println("Cluster Name: "+cluster.getName());
            System.out.println("Cluster environment: "+cluster.getEnvironment());
            System.out.println("Global services:" +cluster.getGlobalServices());
            System.out.println("Authorize ports:"+cluster.getAuthorizePorts());
            System.out.println("Authorize specific ports:" +cluster.getAuthorizeSpecificPorts());
            System.out.println(cluster.getProvider().toString());
            System.out.println(cluster.getNodes());
            System.out.println(cluster.getChefAttributes().toString());
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("File not found in the directory specified");
        }
    }
}
