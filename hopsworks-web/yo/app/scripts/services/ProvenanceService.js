/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

'use strict';

angular.module('hopsWorksApp')
    .factory('ProvenanceService', ['$http', function($http) {
        return {
            getAppLinks: function(projectId, params) {
                var query = '?only_apps=true&full_link=true';
                if (!(params.appId === undefined || params.appId === null)) {
                    query = query + '&filter_by=APP_ID:' + params.appId;
                }
                if (!(params.inArtifactName === undefined || params.inArtifactName === null) && !(params.inArtifactVersion === undefined || params.inArtifactVersion === null)) {
                    query = query + '&filter_by=IN_ARTIFACT:' + params.inArtifactName + '_' + params.inArtifactVersion;
                }
                if(!(params.inArtifactType === undefined || params.inArtifactType === null)) {
                    query = query + '&filter_by=IN_TYPE:' + params.inArtifactType;
                }
                if (!(params.outArtifactName === undefined || params.outArtifactName === null) && !(params.outArtifactVersion === undefined || params.outArtifactVersion === null)) {
                    query = query + '&filter_by=OUT_ARTIFACT:' + params.outArtifactName + '_' + params.outArtifactVersion;
                }
                if(!(params.outArtifactType === undefined || params.outArtifactType === null)) {
                    query = query + '&filter_by=OUT_TYPE:' + params.outArtifactType;
                }
                return $http.get('/api/project/' + projectId + '/provenance/links' + query);
            }
        };
    }]);