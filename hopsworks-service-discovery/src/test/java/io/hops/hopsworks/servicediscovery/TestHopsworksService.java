/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.servicediscovery;

import io.hops.hopsworks.servicediscovery.tags.NoTags;
import io.hops.hopsworks.servicediscovery.tags.ServiceTags;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class TestHopsworksService {

  enum TestTags implements ServiceTags {

    tag_a("tag-a"),
    tag_b("tag-b");

    private final String value;

    TestTags(String value) {
      this.value = value;
    }

    @Override
    public String getValue() {
      return value;
    }
  }
  @Test
  public void testServiceWithTags() {
    HopsworksService<TestTags> s = HopsworksService.of("some_service", TestTags.values());
    Assert.assertEquals("some_service", s.getName());
    Assert.assertEquals("tag-a.some_service", s.getNameWithTag(TestTags.tag_a));
    Set<String> allDomains = s.domains();
    Assert.assertEquals(3, allDomains.size());
    Assert.assertTrue(allDomains.contains("some_service"));
    Assert.assertTrue(allDomains.contains("tag-a.some_service"));
    Assert.assertTrue(allDomains.contains("tag-b.some_service"));
  }

  @Test
  public void testServiceWithoutTags() {
    HopsworksService<NoTags> s = HopsworksService.of("some_service");
    Assert.assertEquals("some_service", s.getName());
    Assert.assertEquals("some_service", s.getNameWithTag(NoTags.empty));
    Set<String> allDomains = s.domains();
    Assert.assertEquals(1, allDomains.size());
    Assert.assertTrue(allDomains.contains("some_service"));
  }
}
