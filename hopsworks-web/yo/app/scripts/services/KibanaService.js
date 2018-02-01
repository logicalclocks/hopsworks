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
/*
 * Service allowing fetching job history objects by type.
 */
angular.module('hopsWorksApp')

        .factory('KibanaService', ['$http', function ($http) {
            var service = {
              
              /**
               * Create a new index for a project in Kibana. 
               * @param {type} projectName
               * @returns {undefined}.
               */
              createIndex: function (projectName) {
                var req = {
                  method: 'POST',
                  url: '/kibana/elasticsearch/.kibana/index-pattern/'+projectName+'?op_type=create',
                  headers: {
                    'Content-Type': 'application/json;charset=utf-8',
                    'kbn-version':'4.6.4'
                  },
                  data: '{"title":"'+projectName+'"}'
                };
                return $http(req);
              }
              
            };
            return service;
          }]);
