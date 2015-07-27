/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('JobsCtrl', ['$routeParams', 'growl', 'JobService',
          function ($routeParams, growl, JobService) {

            var self = this;
            this.projectId = $routeParams.projectID;
            this.jobs; // Will contain all the jobs.

            var getAllJobs = function () {
              JobService.getAllJobsInProject(self.projectId).then(
                      function (success) {
                        self.jobs = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            getAllJobs();

            this.runJob = function (jobId) {
              JobService.runJob(self.projectId, jobId).then(
                      function (success) {
                        var exec = success.data;
                        //Do something with it.
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to run job', ttl: 15000});
              });
            };


          }]);


