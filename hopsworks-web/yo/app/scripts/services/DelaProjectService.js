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

'use strict';

angular.module('hopsWorksApp')

        .factory('DelaProjectService', ['$http', function ($http) {
            return function (id) {
              var service = {
                shareWithHopsByInodeId: function (inodeId) {
                  var payload = {"id": inodeId};
                  return $http({
                    method: 'POST',
                    url: '/api/project/' + id + '/dela/uploads',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: payload
                  });
                },
                unshareFromHops: function (publicDSId, cleanVal) {
                  return $http({
                    method: 'POST',
                    url: '/api/project/' + id + '/dela/transfers/' + publicDSId + '/cancel?clean=cleanVal'
                  });
                },
                downloadMetadata: function (publicDSId, json) {
                  return $http({
                    method: 'POST',
                    url: '/api/project/' + id + '/dela/downloads/' + publicDSId + '/manifest',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: json
                  });
                },
                downloadHdfs: function (publicDSId, json) {
                  return $http({
                    method: 'POST',
                    url: '/api/project/' + id + '/dela/downloads/' + publicDSId + '/hdfs',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: json
                  });
                },
                downloadKafka: function (publicDSId, json) {
                  return $http({
                    method: 'POST',
                    url: '/api/project/' + id + '/dela/downloads/'+ publicDSId + '/kafka',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: json
                  });
                },
                datasetsInfo: function () {
                  return $http({
                    method: 'GET',
                    url: '/api/project/' + id + '/dela/transfers'
                  });
                },
                getDetails: function (publicDSId) {
                  return $http({
                    method: 'GET',
                    url: '/api/project/' + id + '/dela/transfers/' + publicDSId
                  });
                },
                getManifest: function (publicDSId) {
                  return $http({
                    method: 'GET',
                    url: '/api/project/' + id + '/dela/transfers/' + publicDSId + '/manifest'
                  });
                }
              };
              return service;
            };
          }]);
