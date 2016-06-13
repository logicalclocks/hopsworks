/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('JobsCtrl', ['$scope', '$routeParams', 'growl', 'JobService', '$location', 'ModalService', '$interval', 'StorageService',
          '$mdSidenav', 'TourService', 'ProjectService',
          function ($scope, $routeParams, growl, JobService, $location, ModalService, $interval, StorageService,
                  $mdSidenav, TourService, ProjectService) {

            var self = this;
            self.tourService = TourService;
            self.projectId = $routeParams.projectID;
            self.jobs; // Will contain all the jobs.
            self.runningInfo; //Will contain run information
            self.buttonArray = [];
            self.workingArray = [];
            self.jobFilter = {
              "creator": {
                "email": ""
              },
              "jobType": "",
              "name": ""
            };

            self.hasSelectJob = false;

            self.currentjob = null;
            self.currentToggledIndex = -1;

            $scope.pageSize = 10;
            $scope.sortKey = 'creationTime';
            $scope.reverse = true;

            $scope.sort = function (keyname) {
              $scope.sortKey = keyname;   //set the sortKey to the param passed
              $scope.reverse = !$scope.reverse; //if true make it false and vice versa
            };
            self.editAsNew = function (job) {
              JobService.getConfiguration(self.projectId, job.id).then(
                      function (success) {
                        self.currentjob = job;
                        self.currentjob.runConfig = success.data;
                        self.copy();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching job configuration.', ttl: 15000});
              });
            };

            self.buttonClickedToggle = function (id, display) {
              self.buttonArray[id] = display;
            };

            self.stopbuttonClickedToggle = function (id, display) {
              self.workingArray[id] = display;
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
                case "FLINK":
                  jobType = 3;
              }
              var mainFileTxt, mainFileVal, jobDetailsTxt, sparkState, adamState, flinkState;
              if (jobType === 0) {
                mainFileTxt = "Workflow file";
                mainFileVal = self.currentjob.runConfig.wf.name;
                jobDetailsTxt = "Input variables";
              } else if (jobType === 1) {
                sparkState = {
                  "selectedJar": getFileName(self.currentjob.runConfig.jarPath)
                };
                mainFileTxt = "JAR file";
                mainFileVal = sparkState.selectedJar;
                jobDetailsTxt = "Job details";
              } else if (jobType === 2) {
                adamState = {
                  "processparameter": null,
                  "commandList": null,
                  "selectedCommand": self.currentjob.runConfig.selectedCommand.command
                };
                mainFileTxt = "ADAM command";
                mainFileVal = adamState.selectedCommand;
                jobDetailsTxt = "Job arguments";
              } else if (jobType === 3) {
                flinkState = {
                  "selectedJar": getFileName(self.currentjob.runConfig.jarPath)
                };
                mainFileTxt = "JAR file";
                mainFileVal = flinkState.selectedJar;
                jobDetailsTxt = "Job details";
              }
              var state = {
                "jobtype": jobType,
                "jobname": self.currentjob.name,
                "localResources": self.currentjob.runConfig.localResources,
                "phase": 4,
                "runConfig": self.currentjob.runConfig,
                "sparkState": sparkState,
                "adamState": adamState,
                "flinkState": flinkState,
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

            self.getRunStatus = function () {
              JobService.getRunStatus(self.projectId).then(
                      function (success) {
                        self.runningInfo = success.data;
                        angular.forEach(self.jobs, function (temp, key) {
                          if (typeof self.runningInfo['' + temp.id] !== "undefined") {
                            if (!self.runningInfo['' + temp.id].running) {
                              self.buttonArray[temp.id] = false;
                            }
                          }
                        });
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            self.createAppReport = function () {
              angular.forEach(self.jobs, function (temp, key) {
                if (typeof self.runningInfo['' + temp.id] !== "undefined") {
                  if (temp.state !== self.runningInfo['' + temp.id].state) {
                    self.showLogs(temp.id);
                  }
                  temp.duration = self.runningInfo['' + temp.id].duration;
                  temp.finalStatus = self.runningInfo['' + temp.id].finalStatus;
                  temp.progress = self.runningInfo['' + temp.id].progress;
                  temp.running = self.runningInfo['' + temp.id].running;
                  temp.state = self.runningInfo['' + temp.id].state;
                  temp.submissiontime = self.runningInfo['' + temp.id].submissiontime;
                  temp.url = self.runningInfo['' + temp.id].url;
                }
              });
            };
            getAllJobs();
            self.getRunStatus();
            self.createAppReport();

            self.runJob = function (job, index) {
              var jobId = job.id;

              ProjectService.uberPrice({id: self.projectId}).$promise.then(
                      function (success) {
                        var price = success.price;
                        price = Math.ceil(parseFloat(price).toFixed(4) * 100.0 / 1.67*100)/100;
                        ModalService.uberPrice('sm', 'Confirm', 'Do you want to run this job at this price?', price).then(
                                function (success) {
                                  JobService.runJob(self.projectId, jobId).then(
                                          function (success) {
                                            self.toggle(job, index);
                                            self.buttonClickedToggle(job.id, true);
//                                            self.stopbuttonClickedToggle(job.id, false);
                                            self.getRunStatus();
                                          }, function (error) {
                                    growl.error(error.data.errorMsg, {title: 'Failed to run job', ttl: 10000});
                                  });

                                }
                        );

                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Could not get the current YARN price.', ttl: 10000});
              }
              )
            };

            self.stopJob = function (jobId) {
              self.stopbuttonClickedToggle(jobId, true);
              JobService.stopJob(self.projectId, jobId).then(
                      function (success) {
                        self.getRunStatus();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to stop' +
                          ' job', ttl: 15000});
              });
            };

            /**
             * Navigate to the new job page.
             * @returns {undefined}
             */
            self.newJob = function () {
              StorageService.clear();
              $location.path('project/' + self.projectId + '/newjob');
              if (self.tourService.currentStep_TourThree > -1) {
                self.tourService.resetTours();
              }
            };

            self.showDetails = function (job) {
              ModalService.jobDetails('lg', job, self.projectId);
            };


            self.showLogs = function (jobId) {
              JobService.showLog(self.projectId, jobId).then(
                      function (success) {
                        self.logset = success.data.logset;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to show logs', ttl: 15000});
              });
            };

            self.deleteJob = function (jobId, jobName) {
              ModalService.confirm("sm", "Delete Job (" + jobName + ")",
                      "Do you really want to delete this job?\n\
                                This action cannot be undone.")
                      .then(function (success) {
                        JobService.deleteJob(self.projectId, jobId).then(
                                function (success) {
                                  getAllJobs();
                                  self.hasSelectJob = false;
                                  growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Failed to delete job', ttl: 15000});
                        });
                      }, function (cancelled) {
                        growl.info("Delete aborted", {title: 'Info', ttl: 5000});
                      });
            };

            self.toggle = function (job, index) {
              //reset all jobs showing flag
              angular.forEach(self.jobs, function (temp, key) {
                if (job.id !== temp.id) {
                  temp.showing = false;
                }
              });

              //handle the clicked job accordingly
              job.showing = true;
              self.hasSelectJob = true;
              $scope.selectedIndex = index;
              self.currentToggledIndex = index;

            };
            self.untoggle = function (job, index) {
              //reset all jobs showing flag
              angular.forEach(self.jobs, function (temp, key) {
                temp.showing = false;
              });

              if (self.currentToggledIndex !== index) {
                self.hasSelectJob = false;
                $scope.selectedIndex = -1;
                self.currentToggledIndex = -1;
              } else {
                job.showing = true;
              }
            };

            /**
             * Check if the jobType filter is null, and set to empty string if it is.
             * @returns {undefined}
             */
            self.checkJobTypeFilter = function () {
              if (self.jobFilter.jobType === null) {
                self.jobFilter.jobType = "";
              }
            };

            self.launchAppMasterUrl = function (trackingUrl) {
              window.open(trackingUrl);
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
                self.createAppReport();
              }, 5000);
            };
            startPolling();

          }]);


