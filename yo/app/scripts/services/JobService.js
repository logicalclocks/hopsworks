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
               * @param {type} type The name of the job type (e.g. Cuneiform, Spark, Adam)
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
              updateSchedule: function (projectId, type, schedule,jobId) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/jobs/updateschedule/'+jobId,
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
               * Get the current status of all jobs in the given project.
               * @param {type} projectId
               * @returns {unresolved}
               */
              getRunStatus: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jobs/running');
              },
              /**
               * Retrieve the logs associated to a certain job.
               * @param {type} projectId
               * @param {type} jobId
               * @returns {undefined} Log infrormation json.
               */
              showLog: function (projectId, jobId) {
                return $http.get('/api/project/' + projectId + '/jobs/' + jobId + '/showlog');
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
