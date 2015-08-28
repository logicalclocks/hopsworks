'use strict';
/*
 * Controller for the job detail dialog. 
 */
angular.module('hopsWorksApp')
        .controller('JobDetailCtrl', ['$scope', '$modalInstance', 'growl', 'JobService', 'job', 'projectId', '$interval', 'StorageService', '$routeParams', '$location',
          function ($scope, $modalInstance, growl, JobService, job, projectId, $interval, StorageService, $routeParams, $location) {

            var self = this;
            this.job = job;
            this.jobtype; //Holds the type of job.
            this.execFile; //Holds the name of the main execution file
            this.showExecutions = false;
            this.projectId = $routeParams.projectID;

            var getConfiguration = function () {
              JobService.getConfiguration(projectId, job.id).then(
                      function (success) {
                        self.job.runConfig = success.data;
                        self.setupInfo();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching job configuration.', ttl: 15000});
              });
            };

            var getExecutions = function () {
              JobService.getAllExecutions(projectId, job.id).then(
                      function (success) {
                        self.job.executions = success.data;
                        self.showExecutions = success.data.length > 0;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching execution history.', ttl: 15000});
              });
            };

            this.setupInfo = function () {
              if (self.job.runConfig.type == "cuneiformJobConfiguration") {
                self.jobtype = "Cuneiform";
                self.execFile = job.runConfig.wf.name;
              } else if (self.job.runConfig.type == "sparkJobConfiguration") {
                self.jobtype = "Spark";
                self.execFile = getFileName(job.runConfig.jarPath);
              } else if (self.job.runConfig.type == "adamJobConfiguration") {
                self.jobtype = "ADAM";
                self.execFile = job.runConfig.selectedCommand.command;
              }
            };

            getConfiguration();
            getExecutions();

            /**
             * Close the modal dialog.
             * @returns {undefined}
             */
            self.close = function () {
              $modalInstance.dismiss('cancel');
            };

            /**
             * Close the poller if the controller is destroyed.
             */
            $scope.$on('$destroy', function () {
              $interval.cancel(self.poller);
            });

            self.poller = $interval(function () {
              getExecutions();
            }, 3000);

            self.copy = function () {
              var jobType;
              switch (self.job.jobType.toUpperCase()) {
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
                mainFileVal = self.job.runConfig.wf.name;
                jobDetailsTxt = "Input variables";
              } else if (jobType == 1) {
                sparkState = {
                  "selectedJar": getFileName(self.job.runConfig.jarPath)
                };
                mainFileTxt = "JAR file";
                mainFileVal = sparkState.selectedJar;
                jobDetailsTxt = "Job details";
              } else if (jobType == 2) {
                adamState = {
                  "processparameter": null,
                  "commandList": null,
                  "selectedCommand": self.job.runConfig.selectedCommand.command
                };
                mainFileTxt = "ADAM command";
                mainFileVal = adamState.selectedCommand;
                jobDetailsTxt = "Job arguments";
              }
              var state = {
                "jobtype": jobType,
                "jobname": self.job.name,
                "localResources": self.job.runConfig.localResources,
                "phase": 4,
                "runConfig": self.job.runConfig,
                "sparkState": sparkState,
                "adamState": adamState,
                "accordion1": {//Contains the job name
                  "isOpen": false,
                  "visible": true,
                  "value": " - " + self.job.name,
                  "title": "Job name"},
                "accordion2": {//Contains the job type
                  "isOpen": false,
                  "visible": true,
                  "value": " - " + self.job.jobType,
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
                  "title": "Configure and create"},
                "accordion6": {//Contains the schedule
                  "isOpen": false,
                  "visible": true,
                  "value": "",
                  "title": "Schedule (optional)"}
              };
              StorageService.store(self.projectId + "newjob", state);              
              $modalInstance.dismiss('cancel');
              $location.path('project/' + self.projectId + '/newjob');
            };

          }]);
