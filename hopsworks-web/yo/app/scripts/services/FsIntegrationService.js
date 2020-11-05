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

angular.module('hopsWorksApp').factory('FsIntegrationService', ['$http', function($http){
    return {

        getInstances: function(projectId) { 
            return $http.get('/api/project/' + projectId + '/integrations/databricks/');
        },

        addInstance: function(projectId, dbInstanceUrl, dbInstanceApiKey) {
            return $http.post('/api/project/' + projectId +
                             '/integrations/databricks/' + dbInstanceUrl,
                             JSON.stringify({'apiKey': dbInstanceApiKey}),
                             {headers: {'Content-Type': 'application/json'}});
        },

        getClusters: function(projectId, dbInstance) {
            return $http.get('/api/project/' + projectId +
                             '/integrations/databricks/' + dbInstance);
        },

        configure: function(projectId, dbInstance, clusterId, targetUsername) {
            return $http.post('/api/project/' + projectId +
                             '/integrations/databricks/' + dbInstance +
                             '/clusters/' + clusterId + "?username=" + targetUsername);
        },

        downloadJarsToken: function(projectId) {
            return $http.get('/api/project/' + projectId + '/integrations/spark/client/token');
        },

        downloadJars: function(projectId, token) {
            return location.href = getPathname() + '/api/project/' + projectId + 
                '/integrations/spark/client/download?token='+ token;
        },

        sparkConfiguration: function(projectId) {
            return $http.get('/api/project/' + projectId + '/integrations/spark/client/configuration');
        }
    }
}])