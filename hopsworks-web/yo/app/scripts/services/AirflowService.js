/*
 *
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
 *
 */

/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('AirflowService', ['$http', function ($http) {
            return {
              getSecretPath: function (projectId) {
                return $http.get('/api/project/' + projectId + '/airflow/secretDir');
              },
              purgeAirflowDagsLocal: function (projectId) {
                return $http.get('/api/project/' + projectId + '/airflow/purgeAirflowDagsLocal');
              },
              restartAirflow: function (projectId) {
                return $http.get('/api/project/' + projectId + '/airflow/restartWebserver');
              },
              copyFromHdfsToAirflow: function (projectId) {
                return $http.get('/api/project/' + projectId + '/airflow/copyFromHdfsToAirflow');
              },
              copyFromAirflowToHdfs: function (projectId) {
                return $http.get('/api/project/' + projectId + '/airflow/copyFromAirflowToHdfs');
              },
              storeAirflowJWT: function (projectId) {
                return $http.post('/api/project/' + projectId + "/airflow/jwt")
              },
              logout: function() {
                $http.get(getApiLocationBase() + '/airflow/admin/airflow/logout').then(
                  function successCallback(response) {
                    // Do nothing
                  }, function errorCallback(response) {
                    // I wish http module had a way not to follow redirects!!!
                    // When Airflow logs out a user, it sends a redirect
                    // which in browser console will appear as 403 or 500 INTERNAL ERROR
                    //
                    // Both status codes are normal.
                    console.info("Logged out from Airflow successfully");
                  });
              }
            };
          }]);
