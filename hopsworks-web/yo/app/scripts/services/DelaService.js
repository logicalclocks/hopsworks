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
        .factory('DelaService', ['$http', function ($http) {
            var service = {
              getLocalPublicDatasets: function() {
                return $http({
                  method: 'GET',
                  url: '/api/dela',
                  isArray: true});         
              },
              getClientType: function() {
                return $http({
                  method: 'GET',
                  url: '/api/dela/client'}); 
              },
              search: function (searchTerm) {
                return $http({
                  method: 'GET',
                  url: '/api/dela/search?query=' + searchTerm});
              },
              getDetails: function (publicDSId) {
                return $http({
                  method: 'GET',
                  url: '/api/dela/transfers/' + publicDSId});
              },
              getReadme: function (publicDSId, bootstrap) {
                var peers = {"bootstrap": bootstrap};
                return $http({
                  method: 'POST',
                  url: '/api/dela/datasets/' + publicDSId + '/readme',
                  headers: {
                   'Content-Type': 'application/json'
                  },
                  data: peers
                });
              },
              getUserContents: function () {
                return $http({
                  method: 'GET',
                  url: '/api/dela/transfers?filter=USER'});
              }
            };
            return service;
          }]);
