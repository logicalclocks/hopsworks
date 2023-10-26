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

package io.hops.hopsworks.multiregion;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.message.internal.TracingAwarePropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;

import java.io.IOException;
import java.net.URI;

public class TestMultiRegionFilter {

  @InjectMocks
  private MultiRegionFilter multiRegionFilter = new MultiRegionFilter();

  @Mock
  private MultiRegionController multiRegionController;
  @Mock
  private MultiRegionConfiguration multiRegionConfiguration;

  private final URI requestUri = URI.create("https://localhost:8181/hopsworks-api/api/project");
  private final URI relocationUri =
      URI.create("https://hopsworks.glassfish.service.stockholm.consul:8181/hopsworks-api/api/project");

  @Before
  public void before() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testAllowGetSecondary() throws IOException {
    Mockito.when(multiRegionController.isEnabled()).thenReturn(true);
    Mockito.when(multiRegionController.blockSecondaryOperation()).thenReturn(true);

    ContainerRequest containerRequest =
        new ContainerRequest(null, null, "GET", null,
            new MapPropertiesDelegate());

    multiRegionFilter.filter(containerRequest);

    Assert.assertNull(containerRequest.getAbortResponse());
  }

  @Test
  public void testAllowGetPrimary() throws IOException {
    Mockito.when(multiRegionController.isEnabled()).thenReturn(true);
    Mockito.when(multiRegionController.blockSecondaryOperation()).thenReturn(false);

    ContainerRequest containerRequest =
        new ContainerRequest(null, null, "GET", null,
            new MapPropertiesDelegate());

    multiRegionFilter.filter(containerRequest);

    Assert.assertNull(containerRequest.getAbortResponse());
  }

  @Test
  public void testAllowGetDisabled() throws IOException {
    Mockito.when(multiRegionController.isEnabled()).thenReturn(false);

    ContainerRequest containerRequest =
        new ContainerRequest(null, null, "GET", null,
            new MapPropertiesDelegate());

    multiRegionFilter.filter(containerRequest);

    Assert.assertNull(containerRequest.getAbortResponse());
  }

  @Test
  public void testRedirectPostSecondary() throws IOException {
    Mockito.when(multiRegionController.isEnabled()).thenReturn(true);
    Mockito.when(multiRegionController.getPrimaryRegionName()).thenReturn("stockholm");
    Mockito.when(multiRegionController.blockSecondaryOperation()).thenReturn(true);
    Mockito.when(multiRegionConfiguration
        .getString(MultiRegionConfiguration.MultiRegionConfKeys.SERVICE_DISCOVERY_DOMAIN)).thenReturn("consul");

    ContainerRequest containerRequest =
        new ContainerRequest(null, requestUri, "POST", null,
            new MapPropertiesDelegate());

    multiRegionFilter.filter(containerRequest);

    Assert.assertEquals(Response.Status.TEMPORARY_REDIRECT.getStatusCode(),
        containerRequest.getAbortResponse().getStatus());
    Assert.assertEquals(relocationUri, containerRequest.getAbortResponse().getLocation());
  }

  // Some clients will omit port 443 for https because it's standard
  @Test
  public void testRedirectPostSecondaryOnStandardPort() throws IOException {
    Mockito.when(multiRegionController.isEnabled()).thenReturn(true);
    Mockito.when(multiRegionController.getPrimaryRegionName()).thenReturn("stockholm");
    Mockito.when(multiRegionController.blockSecondaryOperation()).thenReturn(true);
    Mockito.when(multiRegionConfiguration
        .getString(MultiRegionConfiguration.MultiRegionConfKeys.SERVICE_DISCOVERY_DOMAIN)).thenReturn("consul");
    URI requestUri = URI.create("https://localhost/hopsworks-api/api/project");
    URI relocationUri = URI.create("https://hopsworks.glassfish.service.stockholm" +
        ".consul:443/hopsworks-api/api/project");
    ContainerRequest containerRequest =
        new ContainerRequest(null, requestUri, "POST", null,
            new MapPropertiesDelegate());

    multiRegionFilter.filter(containerRequest);

    Assert.assertEquals(Response.Status.TEMPORARY_REDIRECT.getStatusCode(),
        containerRequest.getAbortResponse().getStatus());
    Assert.assertEquals(relocationUri, containerRequest.getAbortResponse().getLocation());
  }

  @Test
  public void testRedirectPutSecondary() throws IOException {
    Mockito.when(multiRegionController.isEnabled()).thenReturn(true);
    Mockito.when(multiRegionController.getPrimaryRegionName()).thenReturn("stockholm");
    Mockito.when(multiRegionController.blockSecondaryOperation()).thenReturn(true);
    Mockito.when(multiRegionConfiguration
        .getString(MultiRegionConfiguration.MultiRegionConfKeys.SERVICE_DISCOVERY_DOMAIN)).thenReturn("consul");

    ContainerRequest containerRequest =
        new ContainerRequest(null, requestUri, "PUT", null,
            new MapPropertiesDelegate());

    multiRegionFilter.filter(containerRequest);

    Assert.assertEquals(Response.Status.TEMPORARY_REDIRECT.getStatusCode(),
        containerRequest.getAbortResponse().getStatus());
    Assert.assertEquals(relocationUri, containerRequest.getAbortResponse().getLocation());
  }

  @Test
  public void testAllowPostPrimary() throws IOException {
    Mockito.when(multiRegionController.isEnabled()).thenReturn(true);
    Mockito.when(multiRegionController.blockSecondaryOperation()).thenReturn(false);

    ContainerRequest containerRequest =
        new ContainerRequest(null, null, "POST", null,
            new MapPropertiesDelegate());

    multiRegionFilter.filter(containerRequest);

    Assert.assertNull(containerRequest.getAbortResponse());
  }

  @Test
  public void testAllowPostDisabled() throws IOException {
    Mockito.when(multiRegionController.isEnabled()).thenReturn(false);

    ContainerRequest containerRequest =
        new ContainerRequest(null, null, "POST", null,
            new MapPropertiesDelegate());

    multiRegionFilter.filter(containerRequest);

    Assert.assertNull(containerRequest.getAbortResponse());
  }
}
