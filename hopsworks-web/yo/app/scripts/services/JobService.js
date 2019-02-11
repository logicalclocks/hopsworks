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
 * Service allowing fetching job history objects by type.
 */
angular.module('hopsWorksApp')

        .factory('JobService', ['$http', function ($http) {
            var service = {
               jobFilter: "",

               /**
                 * Get the jobFilter
                 *
                 * @returns {string}
                 */
               getJobFilter: function() {
                   return this.jobFilter
               },

               /**
                 * Update the jobFilter, this method is used by other services that redirect to jobs page and
                 * wants to set the filter
                 *
                 * @param jobFilter
                */
               setJobFilter: function(jobFilter) {
                   this.jobFilter = jobFilter
               },

              /**
               * Get all the jobs defined in the project with given id.
               * @param {int} projectId
               * @param {int} limit
               * @param {int} offset
               * @param {string} query
               * @returns {unresolved} A list of job objects.
               */
              getJobs: function (projectId, limit, offset, query) {
                if (limit === undefined || limit === null) {
                    limit = 0;
                }
                if (offset === undefined || offset === null) {
                    offset = 0;
                }
                if (query === undefined || query === null) {
                    query = "";
                }
                return $http.get('/api/project/' + projectId + '/jobs?limit=' + limit + '&offset=' + offset + query);
              },
              /**
               * Create a new Job in the given project, of the given type. 
               * @param {type} projectId 
               * @param {type} config The configuration of the newly created job.
               * @returns {undefined} The newly created job object.
               */
              putJob: function (projectId, config) {
                var req = {
                  method: 'PUT',
                  url: '/api/project/' + projectId + '/jobs/' + config.appName,
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: config
                };
                return $http(req);
              },
              /**
               * Update the schedule of a job. 
               * @param {type} projectId 
               * @param {type} type
               * @param {type} schedule
               * @param {type} name
               * @returns {undefined}
               */
              updateSchedule: function (projectId, type, schedule, name) {
                var req = {
                  method: 'PUT',
                  url: '/api/project/' + projectId + '/jobs/' + name + "/schedule",
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: schedule
                };
                return $http(req);
              },
              /**
               * Get all the registered executions for the given job.
               * @param {type} projectId
               * @param {type} name
               * @param {string} query
               * @returns {undefined}
               */
              getAllExecutions: function (projectId, name, query) {
                return $http.get('/api/project/' + projectId + '/jobs/' + name + '/executions' + query);
              },
              /**
               * Get the configuration object for the given job.
               * @param {type} projectId
               * @param {type} jobId
               * @returns {unresolved}
               */
              getJob: function (projectId, name) {
                return $http.get('/api/project/' + projectId + '/jobs/' + name);
              },
              /**
               * Run the given job, creating a new Execution instance.
               * @param {type} projectId
               * @param {type} jobId
               * @returns {undefined} The new Execution instance
               */
              runJob: function (projectId, name) {
                return $http.post('/api/project/' + projectId + '/jobs/' + name + '/executions?action=start', {});
              },
              stopJob: function (projectId, name) {
                return $http.post('/api/project/' + projectId + '/jobs/' + name + '/executions?action=stop', {});
              },
              /**
               * Get the latest app Id of the given job.
               * @param {type} projectId
               * @param {type} jobId
               * @returns {unresolved} The address of the job ui.
               */
              getAppId: function (projectId, jobId) {
                return $http.get('/api/project/' + projectId + '/jobs/' + jobId + '/appId');
              },
              /**
               * Get the app Ids of the given job.
               * @param {type} projectId
               * @param {type} jobId
               * @returns {unresolved} The address of the job ui.
               */
              getAppIds: function (projectId, jobId) {
                return $http.get('/api/project/' + projectId + '/jobs/' + jobId + '/appIds');
              },
              /**
               * Get the project name of the given project.
               * @param {type} projectId
               * @param {type} jobId
               * @returns {unresolved} The address of the job ui.
               */
              getProjectName: function (projectId, jobId) {
                return $http.get('/api/project/' + projectId + '/jobs/projectName');
              },
              /**
               * Get the job ui of the given job.
               * @param {type} projectId
               * @param {type} appId
               * @param {type} isLivy
               * @returns {unresolved} The address of the job ui.
               */
              getExecutionUI: function (projectId, appId, isLivy) {
                return $http.get('/api/project/' + projectId + '/jobs/' + appId + '/ui/' + isLivy);
              },
              /**
               * Get the job ui of the given job.
               * @param {type} projectId
               * @param {type} appId
               * @returns {unresolved} The TensorBoard Urls
               */
              getTensorBoardUrls: function (projectId, appId) {
                return $http.get('/api/project/' + projectId + '/jobs/' + appId + '/tensorboard');
              },
              /**
               * Get the yarn ui of the given job.
               * @param {type} projectId
               * @param {type} appId
               * @returns {unresolved} The address of the job ui.
               */
              getYarnUI: function (projectId, appId) {
                return $http.get('/api/project/' + projectId + '/jobs/' + appId + '/yarnui');
              },
              /**
               * Get the app infos.
               * @param {type} projectId
               * @param {type} appId
               * @returns {unresolved} The app info.
               */
              getAppInfo: function (projectId, appId) {
                return $http.get('/api/project/' + projectId + '/jobs/' + appId + '/appinfo');
              },
              /**
               * Get the content of log for the appId
               * @param {type} projectId
               * @param {type} name
               * @param {type} executionId
               * @param {type} type
               * @returns {unresolved}
               */
              getLog: function (projectId, name, executionId, type) {
                  return $http.get('/api/project/' + projectId + '/jobs/' + name + '/executions/' + executionId+ '/log/' + type);
              },

              /**
               * Retrieve the logs associated to a certain job.
               * @param {type} projectId
               * @param {type} appId
               * @param {type} type
               * @returns {undefined} Log infrormation json.
               */
              retryLog: function (projectId, appId, type) {
                return $http.get('/api/project/' + projectId + '/jobs/retryLogAggregation/' + appId + '/' + type);
              },
              /**
               * Delete a job 
               * @param {type} projectId
               * @param {type} jobId
               * @returns {undefined} true if success, false otheriwse
               */
              deleteJob: function (projectId, name) {
                return $http.delete('/api/project/' + projectId + '/jobs/' + name);
              },
              /**
               * Unschedule a job
               * @param {type} projectId
               * @param {type} name
               * @returns {undefined} true if success, false otheriwse
               */
              unscheduleJob: function (projectId, name) {
                return $http.delete('/api/project/' + projectId + '/jobs/' + name  + "/schedule");
              },
              /**
               * Get inspection object (it's a subset of a job configuration)
               * @param projectId
               * @param path
               * @returns {*}
               */
              getInspection: function (projectId, type, path) {
                  return $http.get('/api/project/' + projectId + '/jobs/' + type + '/inspection?path=' + path);
              }

            };
            return service;
          }]);
