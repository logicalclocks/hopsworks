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
               * @param {type} type The name of the job type (e.g. Flink, Spark, Adam)
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
               * @param {type} jobid.
               * @returns {undefined}.
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
               * @param {type} jobId
               * @returns {unresolved} The address of the job ui.
               */
              getExecutionUI: function (projectId, appId, isLivy) {
                return $http.get('/api/project/' + projectId + '/jobs/' + appId + '/ui/' + isLivy);
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
               * @param {type} jobId
               * @returns {unresolved} The address of the job ui.
               */
              getYarnUI: function (projectId, appId) {
                return $http.get('/api/project/' + projectId + '/jobs/' + appId + '/yarnui');
              },
              /**
               * Get the app infos.
               * @param {type} projectId
               * @param {type} jobId
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
               * @param {type} jobId
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
              }

            };
            return service;
          }]);
