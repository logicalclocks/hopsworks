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
