/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('JobsCtrl', ['$scope', '$routeParams', 'growl', 'JobService', '$location', 'ModalService', '$interval',
          function ($scope, $routeParams, growl, JobService, $location, ModalService, $interval) {

            var self = this;
            this.projectId = $routeParams.projectID;
            this.jobs; // Will contain all the jobs.
            this.running; //Will contain run information
            this.jobFilter = {
              "creator":{
                "email":""
              },
              "jobType":"",
              "name":""
            };
            
            this.hasSelectJob=false;

            var getAllJobs = function () {
              JobService.getAllJobsInProject(self.projectId).then(
                      function (success) {
                        self.jobs = success.data;
                        angular.forEach(self.jobs, function (job, key) {
                        job.showing = false;
                      });
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            this.getRunStatus = function () {
              JobService.getRunStatus(self.projectId).then(
                      function (success) {
                        self.running = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            getAllJobs();
            self.getRunStatus();

            this.runJob = function (jobId) {
              JobService.runJob(self.projectId, jobId).then(
                      function (success) {
                        self.getRunStatus();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to run job', ttl: 15000});
              });
            };

            /**
             * Navigate to the new job page.
             * @returns {undefined}
             */
            self.newJob = function () {
              $location.path('project/' + self.projectId + '/newjob');
            };

            self.showDetails = function (job) {
              ModalService.jobDetails('lg', job, self.projectId);
            };
            
            
            self.showLogs = function (jobId) {
              JobService.showLog(self.projectId,jobId).then(
                      function (success) {                          
                          self.logset=success.data.logset;                         
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to show logs', ttl: 15000});
              });              
            };
            
            self.deleteJob=function (jobId,jobName){
                ModalService.confirm("sm", "Delete Job ("+jobName+")",
                                  "Do you really want to delete this job?\n\
                                This action cannot be undone.")
                                  .then(function (success) {
                                        JobService.deleteJob(self.projectId,jobId).then(
                                            function (success) {  
                                                 getAllJobs(); 
                                                 self.hasSelectJob=false;
                                                 growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                                            }, function (error) {
                                      growl.error(error.data.errorMsg, {title: 'Failed to delete job', ttl: 15000});
                                    }); 
                                  }, function (cancelled) {
                                    growl.info("Delete aborted", {title: 'Info', ttl: 5000});
                                  });
 
            };
            
            self.toggleJobs = function (job,index) {
              //reset all jobs showing flag
              angular.forEach(self.jobs, function (temp, key) {
                if (job.id !== temp.id) {
                  temp.showing = false;
                }
              });

              //handle the clicked job accordingly
              job.showing = !job.showing;
              self.hasSelectJob=true;
              $scope.selectedIndex=index;
              //if all jobs are deselected hide log information
              if (!job.showing) {
                 self.hasSelectJob=false;
                 $scope.selectedIndex=-1;
              }
            };
            
            /**
             * Check if the jobType filter is null, and set to empty string if it is.
             * @returns {undefined}
             */
            this.checkJobTypeFilter = function(){
              if(self.jobFilter.jobType == null){
                self.jobFilter.jobType = "";
              }
            };

            /**
             * Close the poller if the controller is destroyed.
             */
            $scope.$on('$destroy', function () {
              $interval.cancel(self.poller);
            });

            var startPolling = function () {
              self.poller = $interval(function () {
                self.getRunStatus();
              }, 5000);
            };
            startPolling();

          }]);


