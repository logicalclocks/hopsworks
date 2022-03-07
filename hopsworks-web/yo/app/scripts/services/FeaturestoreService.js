/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
angular.module('hopsWorksApp')
        .factory('FeaturestoreService', ['$http', function ($http) {
            return {
                /**
                 * GET request for all featurestores for a particular project
                 *
                 * @param projectId id of the project
                 */
                getFeaturestores: function(projectId) {
                    return $http.get('/api/project/' + projectId + '/featurestores');
                },

                /**
                 * GET request for the tags that can be attached to featuregroups or training datasets
                 *
                 * @param query string for the request
                 * @returns {HttpPromise}
                 */
                getTags: function(query) {
                    return $http.get('/api/tags' + query);
                },
            };
          }]);
