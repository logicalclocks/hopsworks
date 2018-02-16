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
        .factory("ProjectService", ['$resource', function ($resource) {
            return $resource(
                    "/api/project/:id",
                    {id: "@id", projectName: "@projectName", inodeId: "@inodeId", type: "@type"},
            {
              "save": {
                method: "POST",
                headers: {'Content-Type': 'application/json; charset=UTF-8'}
              },
              "example": {
                'method': 'POST',
                 url: '/api/project/starterProject/:type'
              },
              "delete": {
                url: '/api/project/:id/delete',  
                method: 'POST'
              },
              "update": {
                method: "PUT",
                headers: {'Content-Type': 'application/json; charset=UTF-8'}
              },
              "projects": {
                'method': 'GET',
                isArray: true
              },
              "getAll": {
                url: '/api/project/getAll',
                'method': 'GET',
                isArray: true
              },
              "getQuotas":{
               url: '/api/project/:id/quotas',
               'method': 'GET'
                      },
              "uberPrice":{
               url: '/api/project/:id/multiplicators',
               'method': 'GET',
               isArray: true
                      },
              "getProjectInfo": {
                url: '/api/project/getProjectInfo/:projectName',
                'method': 'GET'
              },
              "getMoreInfo": {
                url: '/api/project/getMoreInfo/:type/:inodeId',
                'method': 'GET'
              },
              "getMoreInodeInfo": {
                url: '/api/project/:id/getMoreInfo/:type/:inodeId',
                'method': 'GET'
              },
              "enableLogs": {
                url: '/api/project/:id/logs/enable',
                'method': 'POST'
              },
              "getDatasetInfo": {
                url: '/api/project/getDatasetInfo/:inodeId',
                'method': 'GET'                  
              },              
              "getInodeInfo": {
                url: '/api/project/:id/getInodeInfo/:inodeId',
                'method': 'GET'                  
              },
              "importPublicDataset": {
                url: '/api/project/:id/importPublicDataset/:projectName/:inodeId',
                'method': 'GET'                  
              }
            }
            );
          }]);
