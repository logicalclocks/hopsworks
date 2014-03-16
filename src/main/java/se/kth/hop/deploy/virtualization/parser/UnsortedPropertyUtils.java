/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.deploy.virtualization.parser;

import java.beans.IntrospectionException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

/**
 * This defines that we do would like to keep the properties of our YAML file unsorted
 * keeping the order in which we read the file
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class UnsortedPropertyUtils extends PropertyUtils {

    @Override
    protected Set<Property> createPropertySet(Class<? extends Object> type, BeanAccess bAccess)
            throws IntrospectionException {
        Set<Property> result = new LinkedHashSet<Property>(getPropertiesMap(type,
                BeanAccess.FIELD).values());
        return result;
    }
}