/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('JobsCtrl', ['$scope', '$routeParams', 'growl', 'JobService', '$location', 'ModalService', '$interval','StorageService','$mdSidenav',
          function ($scope, $routeParams, growl, JobService, $location, ModalService, $interval,StorageService,$mdSidenav) {

            var self = this;
            this.projectId = $routeParams.projectID;
            this.jobs; // Will contain all the jobs.
            this.runningInfo; //Will contain run information
            this.jobFilter = {
              "creator":{
                "email":""
              },
              "jobType":"",
              "name":""
            };
            
            this.hasSelectJob=false;
            
            self.currentjob=null;
            self.currentToggledIndex=-1;
            
            this. editAsNew = function (job) {   
              JobService.getConfiguration(self.projectId, job.id).then(
                      function (success) {
                        self.currentjob=job;
                        self.currentjob.runConfig = success.data;
                        self.copy();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching job configuration.', ttl: 15000});
              });
            };
            
            self.copy = function () {
              var jobType;
              switch (self.currentjob.jobType.toUpperCase()) {
                case "CUNEIFORM":
                  jobType = 0;
                  break;
                case "SPARK":
                  jobType = 1;
                  break;
                case "ADAM":
                  jobType = 2;
                  break;
              }
              var mainFileTxt, mainFileVal, jobDetailsTxt, sparkState, adamState;
              if (jobType == 0) {
                mainFileTxt = "Workflow file";
                mainFileVal = self.currentjob.runConfig.wf.name;
                jobDetailsTxt = "Input variables";
              } else if (jobType == 1) {
                sparkState = {
                  "selectedJar": getFileName(self.currentjob.runConfig.jarPath)
                };
                mainFileTxt = "JAR file";
                mainFileVal = sparkState.selectedJar;
                jobDetailsTxt = "Job details";
              } else if (jobType == 2) {
                adamState = {
                  "processparameter": null,
                  "commandList": null,
                  "selectedCommand": self.currentjob.runConfig.selectedCommand.command
                };
                mainFileTxt = "ADAM command";
                mainFileVal = adamState.selectedCommand;
                jobDetailsTxt = "Job arguments";
              }
              var state = {
                "jobtype": jobType,
                "jobname": self.currentjob.name,
                "localResources": self.currentjob.runConfig.localResources,
                "phase": 4,
                "runConfig": self.currentjob.runConfig,
                "sparkState": sparkState,
                "adamState": adamState,
                "accordion1": {//Contains the job name
                  "isOpen": false,
                  "visible": true,
                  "value": " - " + self.currentjob.name,
                  "title": "Job name"},
                "accordion2": {//Contains the job type
                  "isOpen": false,
                  "visible": true,
                  "value": " - " + self.currentjob.jobType,
                  "title": "Job type"},
                "accordion3": {// Contains the main execution file (jar, workflow,...)
                  "isOpen": false,
                  "visible": true,
                  "value": " - " + mainFileVal,
                  "title": mainFileTxt},
                "accordion4": {// Contains the job setup (main class, input variables,...)
                  "isOpen": false,
                  "visible": true,
                  "value": "",
                  "title": jobDetailsTxt},
                "accordion5": {//Contains the configuration and creation
                  "isOpen": false,
                  "visible": true,
                  "value": "",
                  "title": "Configure and create"}
              };
              StorageService.store(self.projectId + "newjob", state);              
              $location.path('project/' + self.projectId + '/newjob');
            };
            
            

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
                        self.runningInfo = success.data;
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
              StorageService.clear();
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
            
           
            self.toggle = function (job,index) {
              //reset all jobs showing flag
              angular.forEach(self.jobs, function (temp, key) {
                if (job.id !== temp.id) {
                  temp.showing = false;
                }
              });

              //handle the clicked job accordingly
              job.showing = true;
              self.hasSelectJob=true;
              $scope.selectedIndex=index;
              self.currentToggledIndex=index;

            };
            self.untoggle = function (job,index) {
              //reset all jobs showing flag
              angular.forEach(self.jobs, function (temp, key) {
                   temp.showing = false;
              });
              
              if(self.currentToggledIndex !== index){
                  self.hasSelectJob=false;
                  $scope.selectedIndex=-1;
                  self.currentToggledIndex=-1;
              }else{
                  job.showing = true;
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


