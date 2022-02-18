/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.commit;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeatureGroupCommitController;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashSet;

import static org.mockito.Mockito.verify;

public class TestCommitBuilder {

    private CommitBuilder commitBuilder;
    private FeatureGroupCommitController featureGroupCommitController;

    private UriInfo mockUriInfo;

    private Featuregroup featuregroup;
    private Project project;

    @Before
    public void setup() {
        featureGroupCommitController = Mockito.mock(FeatureGroupCommitController.class);
        Mockito.when(featureGroupCommitController.getCommitDetails(
          Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anySet(), Mockito.anySet()))
                .thenReturn(new AbstractFacade.CollectionInfo(0L, new ArrayList()));

        //throws exception if limit or offset is null
        Mockito.when(featureGroupCommitController
                .getCommitDetails(
                  Mockito.anyInt(), Mockito.anyInt(), Mockito.isNull(),Mockito.anySet(), Mockito.anySet()))
                .thenThrow(new NullPointerException());
        Mockito.when(featureGroupCommitController
                .getCommitDetails(
                  Mockito.anyInt(), Mockito.isNull(), Mockito.anyInt(), Mockito.anySet(), Mockito.anySet()))
                .thenThrow(new NullPointerException());

        commitBuilder = new CommitBuilder(featureGroupCommitController);

        mockUriInfo = Mockito.mock(UriInfo.class);
        UriBuilder mockUriBuilder = Mockito.mock(UriBuilder.class);
        Mockito.when(mockUriBuilder.path(Mockito.anyString()))
                .thenReturn(mockUriBuilder);
        Mockito.when(mockUriInfo.getBaseUriBuilder())
                .thenReturn(mockUriBuilder);

        featuregroup = new Featuregroup();
        featuregroup.setId(0);
        Featurestore featurestore = new Featurestore();
        featurestore.setId(0);
        featuregroup.setFeaturestore(featurestore);

        project = new Project();
        project.setId(0);
    }

    @Test
    public void testBuildNullOffsetAndLimit() throws Exception {
        //arrange
        ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.COMMITS);
        resourceRequest.setOffset(null);
        resourceRequest.setLimit(null);
        resourceRequest.setSort(new HashSet<SortBy>());
        resourceRequest.setFilter(new HashSet<FilterBy>());

        //act
        CommitDTO commitDTO = commitBuilder.build(mockUriInfo, resourceRequest, project, featuregroup);

        //assert
        verify(featureGroupCommitController).getCommitDetails(featuregroup.getId(), 20, 0,
                resourceRequest.getSort(), resourceRequest.getFilter());
        Assert.assertTrue(commitDTO.getCount().intValue() <= 20);
        Assert.assertEquals(0, commitDTO.getCount().intValue());
    }

    @Test
    public void testBuildPopulatedOffsetAndLimit() throws Exception {
        //arrange
        ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.COMMITS);
        resourceRequest.setOffset(0);
        resourceRequest.setLimit(0);
        resourceRequest.setSort(new HashSet<SortBy>());
        resourceRequest.setFilter(new HashSet<FilterBy>());

        //act
        CommitDTO commitDTO = commitBuilder.build(mockUriInfo, resourceRequest, project, featuregroup);

        //assert
        verify(featureGroupCommitController).getCommitDetails(featuregroup.getId(), 0, 0,
                resourceRequest.getSort(), resourceRequest.getFilter());
        Assert.assertEquals(0, commitDTO.getCount().intValue());
    }
}
