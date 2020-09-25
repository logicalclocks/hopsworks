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
/*
 * Service for the feature store page
 */
angular.module('hopsWorksApp').factory('FsStatisticsService', ['$http', function($http){
    return {

        getStatistics: function(projectId, featureStoreId, entity, entityType) {
            return $http.get('/api/project/' + projectId +
                             '/featurestores/' + featureStoreId +
                             '/' + entityType + '/' + entity.id +
                             "/statistics?sort_by=commit_time:desc");
        },

        getStatisticsByCommit: function(projectId, featureStoreId, entity, entityType, commitTime) {
            return $http.get('/api/project/' + projectId +
                             '/featurestores/' + featureStoreId +
                             '/' + entityType + '/' + entity.id +
                             "/statistics?filter_by=commit_time_eq:" + commitTime + "&fields=content");

        },

        getStatisticsLastCommit: function(projectId, featureStoreId, entity, entityType) {
            return $http.get('/api/project/' + projectId +
                             '/featurestores/' + featureStoreId +
                             '/' + entityType + '/' + entity.id +
                             "/statistics?sort_by=commit_time:desc&offset=0&limit=1&fields=content");

        }
    }
}])