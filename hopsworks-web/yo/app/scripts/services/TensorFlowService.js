/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
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
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

'use strict';
/*
 * Service allowing fetching topic history objects by type.
 */
angular.module('hopsWorksApp')

        .factory('TensorFlowService', ['$http', function ($http) {
            var service = {
              /**
               * Get all the tf resources in the syste
               * @param {int} projectId
               * @returns {unresolved} A list of cluster objects.
               */
              getResources: function (projectId) {
                return $http.get('/api/project/' + projectId + '/tensorflow/resources');
              },
              
              allocateResources: function (projectId, resourceRequest){
                  var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/tensorflow/resources/request',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: resourceRequest
                };
                return $http(req);
              },

              freeResources: function (projectId, resources){
                  var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/tensorflow/resources/free',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: resources
                };
                return $http(req);
              },
              
              /**
               * Get all the tf clusters defined in the project with given id.
               * @param {int} projectId
               * @returns {unresolved} A list of cluster objects.
               */
              getClusters: function (projectId) {
                return $http.get('/api/project/' + projectId + '/tensorflow/clusters');
              },

              /**
               * Get the details of the cluster with given ID, under the given project.
               * Includes all description for the cluster
               * @param {type} projectId
               * @param {type} clusterName
               * @returns {unresolved} A complete description of the requested cluster.
               */
              getClusterDetails: function (projectId, clusterName) {
                return $http.get('/api/project/' + projectId + '/tensorflow/details/' + clusterName);
              },
       
              validateCluster: function (projectId, clusterDetails){
                  var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/tensorflow/cluster/validate',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: clusterDetails
                };
                return $http(req);
              },
              
              /**
               * Create a new schema for topics in the given project, of the given type. 
               * @param {type} projectId
               * @param {type} clusterDetails The configuration of the newly created topic.
               * @returns {undefined} The newly created topic object.
               */
              createCluster: function (projectId, clusterDetails) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/tensorflow/cluster/add',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: clusterDetails
                };
                return $http(req);
              },
              
              deleteCluster: function(projectId, clusterName){
                    return $http.delete('/api/project/' + projectId + '/tensorflow/removeCluster/'+clusterName);
              },
              
              /**
               * Add a new ACL rule to a Topic in the given project. 
               * @param {type} projectId 
               * @param {type} clusterName
               * @param {type} topicAcl The ACL for the topic.
               * @returns {undefined} The newly created topic object.
               */
              createJob: function (projectId, clusterName, job) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/tensorflow/cluster/' + clusterName + "/addJob",
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: job
                };
                return $http(req);
              },
              /**
               * Delete a job for a cluster
               * @param {type} projectId
               * @param {type} clusterName
               * @param {type} jobId
               * @returns {undefined} true if success, false otheriwse
               */
              removeJob: function (projectId, clusterName, jobId) {
                return $http.delete('/api/project/' + projectId + '/tensorflow/cluster/' + clusterName + '/removeJob/' + jobId);
              },
              
              /**
               * Delete a cluster 
               * @param {type} projectId
               * @param {type} clusterName
               * @returns {undefined} true if success, false otheriwse
               */
              removeCluster: function (projectId, clusterName) {
                return $http.delete('/api/project/' + projectId + '/tensorflow/cluster/' + clusterName + '/remove');
              },
              
               
              createTask: function (projectId, clusterName, jobId, task) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/tensorflow/cluster/' + clusterName + "/job/" + jobId + "/addTask",
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: task
                };
                return $http(req);
              },
              removeTask: function (projectId, clusterName, jobId, taskId) {
                return $http.delete('/api/project/' + projectId + '/tensorflow/cluster/' + clusterName 
                        + '/job/' + jobId + '/removeTask/' + taskId);
              },
              updateTask: function(projectId, clusterName, jobId, task){                
                var req = {
                  method: 'PUT',
                  url: '/api/project/' + projectId + '/tensorflow/cluster/' + clusterName + '/job/' + jobId + '/updateTask',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: task
                  };
                return $http(req);  
              },
              runTask: function (projectId, clusterName, jobId, taskId) {
                return $http.get('/api/project/' + projectId + '/tensorflow/cluster/' + clusterName 
                        + '/job/' + jobId + '/runTask/' + taskId);
              },
              stopTask: function (projectId, clusterName, jobId, taskId) {
                return $http.get('/api/project/' + projectId + '/tensorflow/cluster/' + clusterName 
                        + '/job/' + jobId + '/stopTask/' + taskId);
              },
              getTaskLogs: function (projectId, clusterName, jobId, taskId, numLines) {
                return getTaskExecutionLogs(projectId, clusterName, jobId, taskId, 0, numLines);
              },              
              getTaskExecutionLogs: function (projectId, clusterName, jobId, taskId, executionId, numLines) {
                return $http.get('/api/project/' + projectId + '/tensorflow/cluster/' + clusterName 
                        + '/job/' + jobId + '/taskLogs/' + taskId + '/executionId/' + executionId + '/numLines/' + numLines);
              },              
              getTaskExecutions: function (projectId, clusterName, jobId, taskId) {
                return $http.get('/api/project/' + projectId + '/tensorflow/cluster/' + clusterName 
                        + '/job/' + jobId + '/taskExecutions/' + taskId);
              },              
              removeTaskLogs: function (projectId, clusterName, jobId, taskId, executionId) {
                return $http.delete('/api/project/' + projectId + '/tensorflow/cluster/' + clusterName 
                        + '/job/' + jobId + '/taskLogs/' + taskId + '/execId/' + executionId);
              },              
              /**
               * Get all the tf serving clusters defined in the project with given id.
               * @param {int} projectId
               * @returns {unresolved} A list of tf serving objects.
               */
              getServing: function (projectId) {
                return $http.get('/api/project/' + projectId + '/tensorflow/serving');
              },
              /**
               * Get the details of the cluster with given ID, under the given project.
               * Includes all description for the serving cluster
               * @param {type} projectId
               * @param {type} clusterName
               * @returns {unresolved} A complete description of the requested cluster.
               */
              getServingDetails: function (projectId, clusterName) {
                return $http.get('/api/project/' + projectId + '/tensorflow/details/' + clusterName);
              },
              /**
               * Create a new Serving Tf server in the given project, of the given type. 
               * @param {type} projectId 
               * @param {type} servingDetails The configuration of the newly created topic.
               * @returns {undefined} The newly created tf server object.
               */
              createServing: function (projectId, servingDetails) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/tensorflow/serving/add',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: servingDetails
                };
                return $http(req);
              },
              removeServing: function (projectId, servingId) {
                return $http.delete('/api/project/' + projectId + '/tensorflow/serving/' + servingId + '/remove/');
              },                 
              compileServing: function (projectId, servingId) {
                return $http.get('/api/project/' + projectId + '/tensorflow/serving/' + servingId + '/compile');
              },   
              startServing: function (projectId, servingId) {
                return $http.get('/api/project/' + projectId + '/tensorflow/serving/' + servingId + '/start');
              },   
              stopServing: function (projectId, servingId) {
                return $http.get('/api/project/' + projectId + '/tensorflow/serving/' + servingId + '/stop');
              },   
              getServingLogs: function (projectId, servingId, numLines) {
                return $http.get('/api/project/' + projectId + '/tensorflow/serving/' + servingId  
                        + '/readLogLines/' + numLines);
              },    
              /**
               * Inspect the python at the given path.
               * @param {int} projectId
               */
              inspectProgram: function (projectId, path) {
                return $http.get('/api/project/' + projectId + '/jobs/tensorflow/inspect/' + path);
              }
            };
            return service;
          }]);
