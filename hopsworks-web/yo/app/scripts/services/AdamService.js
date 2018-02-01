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
 * Service responsible for communicating with the Adam backend.
 */
angular.module('hopsWorksApp')

        .factory('AdamService', ['$http', function ($http) {
            var service = {
              /**
               * Request a list of all available commands.
               * @param {int} projectId
               */
              getCommandList: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jobs/adam/commands');
              },
              /**
               * Get the details of the given command, i.e. arguments and options.
               * @param {type} projectId
               * @param {type} commandname
               * @returns {unresolved}
               */
              getCommand: function (projectId, commandname) {
                return $http.get('/api/project/' + projectId + '/jobs/adam/commands/' + commandname);
              }
            };
            return service;
          }]);


