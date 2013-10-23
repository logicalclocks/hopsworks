/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.util;

import java.beans.IntrospectionException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import se.kth.kthfsdashboard.virtualization.clusterparser.Baremetal;
import se.kth.kthfsdashboard.virtualization.clusterparser.Cluster;
import se.kth.kthfsdashboard.virtualization.clusterparser.GitProperties;
import se.kth.kthfsdashboard.virtualization.clusterparser.GlobalProperties;
import se.kth.kthfsdashboard.virtualization.clusterparser.NodeGroup;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class ClusterParserTest extends TestCase {

    private Yaml yaml;
    private final String dir;
    private final String separator;

    public ClusterParserTest(String testname) {
        super(testname);

        dir = System.getProperty("user.dir");
        separator = System.getProperty("file.separator");
    }

    public void testClusterDetection() throws Exception {
        yaml = new Yaml(new Constructor(Cluster.class));
        Object document = yaml.load(new BufferedReader(new FileReader(new File(dir + separator
                + "clusters" + separator + "dummy.yml"))));
        assertNotNull(document);
        assertTrue(document.getClass().toString(), document instanceof Cluster);
    }

    public void testClusterGlobalProperties() throws Exception {
        yaml = new Yaml(new Constructor(Cluster.class));
        Object document = yaml.load(new BufferedReader(new FileReader(new File(dir + separator
                + "clusters" + separator + "dummy.yml"))));
        Cluster cluster = (Cluster) document;
        /*
         * Check not null the variables for global configuration
         */
        assertNotNull(cluster.getGlobal());
        GlobalProperties global = cluster.getGlobal();
        assertNotNull(global.getRecipes());
        assertNotNull(global.getAuthorizePorts());
        assertNotNull(global.getGit());

        /*
         * check if they are equal to what there is in the file
         */
        assertEquals("test2", cluster.getName());
        //assertEquals("dev", global.getEnvironment());
        assertEquals("ssh", global.getRecipes().get(0));
        assertEquals("chefClient", global.getRecipes().get(1));
        assertEquals(3306, global.getAuthorizePorts().get(0).intValue());
        assertEquals(4343, global.getAuthorizePorts().get(1).intValue());
        assertEquals(3321, global.getAuthorizePorts().get(2).intValue());
        GitProperties git = global.getGit();
        assertEquals("jim", git.getUser());
        assertEquals("jim", git.getKey());
        assertEquals("https://ghetto.sics.se/jdowling/kthfs-pantry.git", git.getRepository());

    }

    public void testNodeGroups() throws Exception {
        yaml = new Yaml(new Constructor(Cluster.class));
        Object document = yaml.load(new BufferedReader(new FileReader(new File(dir + separator
                + "clusters" + separator + "dummy.yml"))));
        Cluster cluster = (Cluster) document;

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        /*
         * Check not null the variables for global configuration
         */

        assertNotNull(cluster.getNodes());
        List<NodeGroup> nodes = cluster.getNodes();

        NodeGroup ndb = nodes.get(0);
        NodeGroup mgm = nodes.get(1);
        NodeGroup mysql = nodes.get(2);
        NodeGroup namenodes = nodes.get(3);
        NodeGroup datanodes = nodes.get(4);

        assertNotNull(ndb);
        assertNotNull(mgm);
        assertNotNull(mysql);
        assertNotNull(namenodes);
        assertNotNull(datanodes);

        /*
         * check that values are correct
         */

        assertEquals("ndb", ndb.getService());
        assertEquals(2, ndb.getNumber());

//        assertEquals(10, ndb.getAuthorizePorts().get(0).intValue());
//        assertNull(ndb.getChefAttributes());

        assertEquals("mgm", mgm.getService());
        assertEquals(1, mgm.getNumber());

        assertEquals(10, mgm.getAuthorizePorts().get(0).intValue());
        assertNull(mgm.getChefAttributes());

        assertEquals("mysql", mysql.getService());
        assertEquals(1, mysql.getNumber());

        assertEquals(20, mysql.getAuthorizePorts().get(0).intValue());
        assertNull(mysql.getChefAttributes());

        /*
         * See if dump maintains order and ignores missing or empty properties
         */
        Representer repr = new MyRepresenter();
        repr.setPropertyUtils(new UnsortedPropertyUtils());
        Yaml test = new Yaml(new Constructor(Cluster.class), repr, options);
        String output = test.dump(document);
        System.out.println(output);

    }

    public void testBaremetalDetection() throws Exception {
        yaml = new Yaml(new Constructor(Baremetal.class));
        Object document = yaml.load(new BufferedReader(new FileReader(new File(dir + separator
                + "clusters" + separator + "dummyBaremetal.yml"))));
        assertNotNull(document);
        assertTrue(document.getClass().toString(), document instanceof Baremetal);
    }
    
    public void testParsingSkipProperties() throws Exception{
        yaml = new Yaml(new Constructor(Baremetal.class));
        Object document = yaml.load(new BufferedReader(new FileReader(new File(dir + separator
                + "clusters" + separator + "dummyBaremetal.yml"))));
        Baremetal cluster = (Baremetal) document;

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        
        /*
         * See if dump maintains order and ignores missing or empty properties
         */
        Representer repr = new MyRepresenter();
        repr.setPropertyUtils(new UnsortedPropertyUtils());
        Yaml test = new Yaml(new Constructor(Cluster.class), repr, options);
        String output = test.dump(document);
        System.out.println(output);
    }
}

class UnsortedPropertyUtils extends PropertyUtils {

    @Override
    protected Set<Property> createPropertySet(Class<? extends Object> type, BeanAccess bAccess)
            throws IntrospectionException {
        Set<Property> result = new LinkedHashSet<Property>(getPropertiesMap(type,
                BeanAccess.FIELD).values());
        return result;
    }
}

class MyRepresenter extends Representer {

    @Override
    protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
            Object propertyValue, Tag customTag) {
        String name = property.getName();
        if ((name.equals("recipes") || name.equals("authorizePorts")
                || name.equals("chefAttributes") || name.equals("zones") || name.equals("loginUser")
                ||name.equals("bittorrent"))
                && propertyValue == null) {
            return null;
        } else if (name.equals("environment") && propertyValue == null) {
            return super.representJavaBeanProperty(javaBean, property, "prod", customTag);
        } else {
            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
        }
    }
}
