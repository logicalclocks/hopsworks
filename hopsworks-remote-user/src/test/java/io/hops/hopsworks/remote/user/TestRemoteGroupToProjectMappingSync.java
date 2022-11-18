/*
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user;

import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestRemoteGroupToProjectMappingSync {

  @Test
  public void testIntervalParsingNumber() {
    Settings settings = Mockito.mock(Settings.class);
    Mockito.when(settings.ldapGroupMappingSyncInterval()).thenReturn("10000");
    RemoteGroupToProjectMappingSync sync = new RemoteGroupToProjectMappingSync();
    sync.setSettings(settings);
    Pair<Long, String> interval = sync.interval();
    Assert.assertEquals(Long.getLong("10000"), interval.getLeft());
    Assert.assertEquals("10000ms", interval.getRight());
  }

  @Test
  public void testIntervalParsingExpression() {
    Settings settings = Mockito.mock(Settings.class);
    Mockito.when(settings.ldapGroupMappingSyncInterval()).thenReturn("10m");
    Mockito.when(settings.getConfTimeValue(Mockito.any())).thenCallRealMethod();
    Mockito.when(settings.getConfTimeTimeUnit(Mockito.any())).thenCallRealMethod();
    RemoteGroupToProjectMappingSync sync = new RemoteGroupToProjectMappingSync();
    sync.setSettings(settings);
    Pair<Long, String> interval = sync.interval();
    Long expectedInterval = 10L * 60L * 1000L;
    Assert.assertEquals(expectedInterval, interval.getLeft());
    Assert.assertEquals("10m", interval.getRight());
  }

  @Test
  public void testIntervalParsingTooFrequent() {
    Settings settings = Mockito.mock(Settings.class);
    // This is too frequent
    Mockito.when(settings.ldapGroupMappingSyncInterval()).thenReturn("1m");
    Mockito.when(settings.getConfTimeValue(Mockito.any())).thenCallRealMethod();
    Mockito.when(settings.getConfTimeTimeUnit(Mockito.any())).thenCallRealMethod();
    RemoteGroupToProjectMappingSync sync = new RemoteGroupToProjectMappingSync();
    sync.setSettings(settings);
    Pair<Long, String> interval = sync.interval();
    // It should fallback to 2 minutes
    Long expectedInterval = 2L * 60L * 1000L;
    Assert.assertEquals(expectedInterval, interval.getLeft());
    Assert.assertEquals("2m", interval.getRight());
  }
}
