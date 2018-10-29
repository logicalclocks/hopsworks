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
              /**
               * Get all the jobs defined in the project with given id.
               * @param {int} projectId
               * @returns {unresolved} A list of job objects.
               */
              getAllJobsInProject: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jobs');
              },
              /**
               * Get the details of the job with given ID, under the given project.
               * @param {type} projectId
               * @param {type} jobId
               * @returns {unresolved} A complete description of the requested job.
               */
              getDetailsForJob: function (projectId, jobId) {
                return $http.get('/api/project/' + projectId + '/jobs/' + jobId);
              },
              /**
               * Get all the jobs in the current project for the given type.
               * @param {type} projectId
               * @param {type} type The name of the job type (e.g. Flink, Spark)
               * @returns {unresolved} A list of defined jobs in the given project of the requested type.
               */
              getByProjectAndType: function (projectId, type) {
                return $http.get('/api/project/' + projectId + '/jobs/' + type.toLowerCase());
              },
              /**
               * Create a new Job in the given project, of the given type. 
               * @param {type} projectId 
               * @param {type} type
               * @param {type} config The configuration of the newly created job.
               * @returns {undefined} The newly created job object.
               */
              createNewJob: function (projectId, type, config) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/jobs/' + type.toLowerCase(),
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
               * @param {type} jobId
               * @returns {undefined}
               */
              updateSchedule: function (projectId, type, schedule, jobId) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/jobs/updateschedule/' + jobId,
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
               * @param {type} jobId
               * @returns {undefined}
               */
              getAllExecutions: function (projectId, jobId) {
                return $http.get('/api/project/' + projectId + '/jobs/' + jobId + '/executions');
              },
              /**
               * Get the configuration object for the given job.
               * @param {type} projectId
               * @param {type} jobId
               * @returns {unresolved}
               */
              getConfiguration: function (projectId, jobId) {
                return $http.get('/api/project/' + projectId + '/jobs/' + jobId + '/config');
              },
              /**
               * Run the given job, creating a new Execution instance.
               * @param {type} projectId
               * @param {type} jobId
               * @returns {undefined} The new Execution instance
               */
              runJob: function (projectId, jobId) {
                return $http.post('/api/project/' + projectId + '/jobs/' + jobId + '/executions', {});
              },
              stopJob: function (projectId, jobId) {
                return $http.post('/api/project/' + projectId + '/jobs/' + jobId + '/executions/stop', {});
              },
              /**
               * Get the current status of the given execution.
               * @param {type} projectId
               * @param {type} jobId
               * @param {type} executionId
               * @returns {unresolved} The entire Execution object.
               */
              getExecutionStatus: function (projectId, jobId, executionId) {
                return $http.get('/api/project/' + projectId + '/jobs/' + jobId + '/executions/' + executionId);
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
              /* Get the tensorboard URLs for the appid.
               * @param {type} projectId
               * @param {type} appId
               * @returns {unresolved} The addresses of the tensorboard uis.
               */
              getTensorboardUIs: function (projectId, appId) {
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
               * Get the current status of all jobs in the given project.
               * @param {type} projectId
               * @returns {unresolved}
               */
              getRunStatus: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jobs/running');
              },
              /**
               * Retrieve the logs associated with a certain job.
               * @param {type} projectId
               * @param {type} jobId
               * @returns {undefined} Log infrormation json.
               */
              showLog: function (projectId, jobId) {
                return $http.get('/api/project/' + projectId + '/jobs/' + jobId + '/showlog');
              },
              /**
               * Get the content of log for the appId
               * @param {type} projectId
               * @param {type} jobId
               * @param {type} type
               * @returns {unresolved}
               */
              getLog: function (projectId, jobId, type) {
                return $http.get('/api/project/' + projectId + '/jobs/getLog/' + jobId + '/' + type);
              },
              /**
               * Get log by job id and submission time if app id not available.
               * @param {type} projectId
               * @param {type} jobId
               * @param {type} submissionTime
               * @param {type} type
               * @returns {unresolved}
               */
              getLogByJobIdAndSubmissionTime: function (projectId, jobId, submissionTime, type) {
                return $http.get('/api/project/' + projectId + '/jobs/getLogByJobId/' + jobId + '/' + submissionTime + '/' + type);
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
              deleteJob: function (projectId, jobId) {
                return $http.delete('/api/project/' + projectId + '/jobs/' + jobId + '/deleteJob');
              },
              /**
               * Unschedule a job
               * @param {type} projectId
               * @param {type} jobId
               * @returns {undefined} true if success, false otheriwse
               */
              unscheduleJob: function (projectId, jobId) {
                return $http.delete('/api/project/' + projectId + '/jobs/' + jobId + '/unschedule');
              }

            };
            return service;
          }]);
