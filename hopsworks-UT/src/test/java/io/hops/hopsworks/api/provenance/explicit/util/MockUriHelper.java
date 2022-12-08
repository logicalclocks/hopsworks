/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.provenance.explicit.util;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

public class MockUriHelper {
  public abstract static class TestUriBuilder extends UriBuilder {
    protected String fullPath;
  }
  
  public static TestUriBuilder nopUriBuilder() {
    TestUriBuilder uriBuilder = Mockito.mock(TestUriBuilder.class);
    uriBuilder.fullPath = "https://localhost:8181/hopsworks-api/api";
    Mockito.when(uriBuilder.path(Mockito.anyString())).thenAnswer(
      (Answer<UriBuilder>) invocation -> {
        String path = invocation.getArgument(0);
        uriBuilder.fullPath += "/" + path;
        return uriBuilder;
      });
    Mockito.when(uriBuilder.build()).thenAnswer((Answer<URI>) invocation -> new URI(uriBuilder.fullPath));
    return uriBuilder;
  }
  
  public static UriInfo nopUriInfo() {
    UriInfo uriInfo = Mockito.mock(UriInfo.class);
    Mockito.when(uriInfo.getBaseUriBuilder()).thenAnswer( (Answer<UriBuilder>) invocation -> nopUriBuilder());
    return uriInfo;
  }
}
