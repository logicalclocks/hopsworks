/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
