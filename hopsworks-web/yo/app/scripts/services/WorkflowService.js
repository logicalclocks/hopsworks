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

/**
 * @ngdoc service
 * @name hopsWorksApp.WorkflowService
 * @description
 * # WorkflowService
 * Service in the hopsWorksApp.
 */
angular.module('hopsWorksApp')
  .factory('WorkflowService', ['$http', function ($http) {
    return {
      index: function(projectId){
        return $http.get('/api/project/'+ projectId + '/workflows')
      },
      create: function(projectId, data){
        var regReq={
          method: 'POST',
          url: '/api/project/'+ projectId + '/workflows',
          headers: {'Content-Type': 'application/json'},
          data: data,
          dataType: "json"
        }
        return $http(regReq)
      },
      show: function(projectId, id){
        return $http.get('/api/project/'+ projectId + '/workflows/' + id)
      }
    }
  }]);
