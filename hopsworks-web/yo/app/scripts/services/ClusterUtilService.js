/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('ClusterUtilService', ['$http', function ($http) {
            return {
              getYarnMetrics: function () {
                return $http.get('/api/clusterUtilisation/metrics');
              },
              getHdfsStatus: function () {
                return $http.get('/api/kmon/groups/HDFS');
              },
              getYarnStatus: function () {
                return $http.get('/api/kmon/groups/YARN');
              },
              getKafkaStatus: function () {
                return $http.get('/api/kmon/groups/kafka');
              }
            };
          }]);


