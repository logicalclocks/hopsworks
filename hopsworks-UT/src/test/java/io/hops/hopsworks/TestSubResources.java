/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks;

import io.hops.hopsworks.common.dataset.util.DatasetPath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import javax.enterprise.context.RequestScoped;
import javax.persistence.Entity;
import javax.ws.rs.Path;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This test will ensure that sub resources do not contain entity class properties.
 * 1. Sub resources are request scoped and so should not cache anything.
 * 2. If an entity class is passed to a sub resource that means it needs to be fetched from database before the sub
 * resource is called. This will bypass any filters that are set to intercept methods on the sub resource.
 * <p>
 * So if we have a project entity in a sub resource, and we pass it from a parent resource the project entity will be
 * fetched from database before the user, that is making the request, is authenticated and verified if he/she has access
 * to the project.
 */
public class TestSubResources {
  Set<Class<?>> subResourceClasses = new HashSet<>();
  Set<Class<?>> resourceClasses = new HashSet<>();
  Set<Class<?>> entityClasses = new HashSet<>();

  @Before
  public void beforeTest() {
    Reflections reflections = new Reflections("io.hops.hopsworks", new SubTypesScanner(false),
      new TypeAnnotationsScanner());
    for (Class<?> c : reflections.getTypesAnnotatedWith(Path.class, true)) {
      if (c.getName().startsWith("io.hops.hopsworks.api")) {
        resourceClasses.add(c);
      }
    }
    for (Class<?> c : reflections.getTypesAnnotatedWith(RequestScoped.class, true)) {
      if (c.getName().startsWith("io.hops.hopsworks.api")) {
        subResourceClasses.add(c);
      }
    }

    for (Class<?> c : reflections.getTypesAnnotatedWith(Entity.class, true)) {
      if (c.getName().startsWith("io.hops.hopsworks.persistence.entity")) {
        entityClasses.add(c);
      }
    }

  }

  @Test
  public void testIfClassesAreFound() {
    System.out.println("SubResources: " + subResourceClasses.size());
    assertFalse("Sub Resource classes should not be empty.", subResourceClasses.isEmpty());
    System.out.println("Entity classes: " + entityClasses.size());
    assertFalse("Entity classes should not be empty.", entityClasses.isEmpty());
  }

  @Test
  public void testSubResourceAreFinal() {
    for (Class<?> c : resourceClasses) {
      if (!Modifier.isFinal(c.getModifiers())) {
        System.out.println("Api Resources should be final. Inheriting a resource class will force the swagger " +
          "documentation to be too general. Offending classes " + c.getName());
      }
//      assertTrue("Api Resources should be final. Inheriting a resource class will force the swagger " +
//          "documentation to be too general. Offending classes " + c.getName(),
//        Modifier.isFinal(c.getModifiers()));
    }
    for (Class<?> c : subResourceClasses) {
      if (!Modifier.isFinal(c.getModifiers())) {
        System.out.println("Api Resources should be final. Inheriting a resource class will force the swagger " +
          "documentation to be too general. Offending classes " + c.getName());
      }
//      assertTrue("Api Resources should be final. Inheriting a resource class will force the swagger " +
//          "documentation to be too general. Offending classes " + c.getName(),
//        Modifier.isFinal(c.getModifiers()));
    }
  }

  @Test
  public void testEntityClassesInFields() {
    for (Class<?> c : subResourceClasses) {
      Set<Class<?>> entityClassesInFields = containsEntity(c);
      if (!entityClassesInFields.isEmpty()) {
        System.out.println("Class " + c.getName() + " contains entity class fields. ");
        System.out.println(entityClassesInFields);
      }
      assertTrue("SubResources should not contain entity classes fields that query the database.",
        entityClassesInFields.isEmpty());
    }
  }

  @Test
  public void testDatasetPathClassesInFields() {
    for (Class<?> c : subResourceClasses) {
      if (containsClass(c, DatasetPath.class)) {
        System.out.println("Class " + c.getName() + " contains DatasetPath in fields. ");
        Assert.fail("SubResources should not contain any Class that queries the database. Found DatasetPath");
      }
    }
  }

  private Set<Class<?>> containsEntity(Class<?> c) {
    Set<Class<?>> entityClassesInFields = new HashSet<>();
    for (Field field : c.getDeclaredFields()) {
      if (entityClasses.contains(field.getType())) {
        entityClassesInFields.add(field.getType());
      }
    }
    return entityClassesInFields;
  }

  private boolean containsClass(Class<?> c, Class<?> c1) {
    for (Field field : c.getDeclaredFields()) {
      if (field.getType().equals(c1)) {
        return true;
      }
    }
    return false;
  }
}
