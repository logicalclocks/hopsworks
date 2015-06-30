/**
 * Controller for the Spark fatjar jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('SparkCtrl', ['$routeParams','growl', 'JobHistoryService','$interval','SparkService',
          function ($routeParams, growl, JobHistoryService, $interval,SparkService) {

            var self = this;
            var poller;

            self.pId = $routeParams.projectID;
            
            /*
             * Get all Spark job history objects for this project.
             */
            var getSparkHistory = function () {
              JobHistoryService.getByProjectAndType(self.pId, 'SPARK').then(function (success) {
                //Upon success, fill in jobs
                self.jobs = success.data;
              }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                self.jobs = null;
              });
            };

            getSparkHistory();
            
            self.execute = function () {
              //First: stop polling for previous jobs.
              $interval.cancel(poller);
              //Then: submit job.
              SparkService.runJob(self.pId, self.sparkConfig).then(
                      function (success) {
                        //get the resulting job
                        self.job = success.data;
                        //Add it to the history array
                        self.jobs.unshift(self.job);
                        //Reset the configuration data
                        self.sparkConfig = null;
                        //Start polling
                        poller = $interval(pollStatus, 3000);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              })
            };
            
            var pollStatus = function () {
              JobHistoryService.pollStatus(self.pId, self.job.id).then(
                      function (success) {
                        var oldindex = self.jobs.indexOf(self.job);
                        self.job = success.data;
                        //Replace the old element in the jobs array
                        if (oldindex !== -1) {
                          self.jobs[oldindex] = self.job;
                        }
                        //check if job finished
                        if (self.job.state == 'FINISHED'
                                || self.job.state == 'FAILED'
                                || self.job.state == 'KILLED'
                                || self.job.state == 'FRAMEWORK_FAILURE'
                                || self.job.state == 'APP_MASTER_START_FAILED') {
                          //if so: stop executing
                          $interval.cancel(poller);
                        }
                      }, function (error) {
                $interval.cancel(poller);
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              })
            };

          }]);


