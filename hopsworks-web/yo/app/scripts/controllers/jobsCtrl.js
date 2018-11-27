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

/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('JobsCtrl', ['$scope', '$window', '$routeParams', 'growl',
        'JobService', '$location', 'ModalService', '$interval', 'StorageService',
                    'TourService', 'ProjectService','FileSaver', '$timeout', '$route',
          function ($scope, $window, $routeParams, growl, JobService, $location,
          ModalService, $interval, StorageService, TourService, ProjectService, FileSaver,
          $timeout, $route) {

            var self = this;
            self.tourService = TourService;
            self.projectId = $routeParams.projectID;
            self.jobs; // Will contain all the jobs.
            self.runningInfo; //Will contain run information
            self.runningStates = ['INITIALIZING', 'RUNNING', 'ACCEPTED', 'NEW', 'NEW_SAVING', 'SUBMITTED',
                  'STARTING_APP_MASTER', 'AGGREGATING_LOGS'];
            self.buttonArray = [];
            self.workingArray = [];
            self.jobFilter = "";
            self.hasSelectJob = false;

            self.currentjob = null;
            self.currentToggledIndex = -1;
            self.fetchingLogs = 0;
            self.loadingLog = 0;
            $scope.pageSize = 10;
            $scope.sortKey = 'creationTime';
            $scope.reverse = true;

            $scope.sort = function (keyname) {
              $scope.sortKey = keyname;   //set the sortKey to the param passed
              $scope.reverse = !$scope.reverse; //if true make it false and vice versa
            };

            self.refreshSlider = function () {
              $timeout(function () {
                $scope.$broadcast('rzSliderForceRender');
              });
            };

            self.editAsNew = function (job) {
                        self.currentjob = job;
                        self.currentjob.runConfig = job.config;
                        self.refreshSlider();
                        self.copy();
            };

            self.buttonClickedToggle = function (name, display) {
              self.buttonArray[name] = display;
              self.workingArray[name] = "true";
            };

            self.stopbuttonClickedToggle = function (name, display) {
              self.workingArray[name] = display;
              var jobClickStatus = StorageService.recover(self.projectId + "_jobstopclicked_"+name);
              StorageService.store(self.projectId + "_jobstopclicked_"+name, jobClickStatus);
              if(jobClickStatus === "stopping"){
                StorageService.store(self.projectId + "_jobstopclicked_"+name, "killing");
              } else {
                StorageService.store(self.projectId + "_jobstopclicked_"+name, "stopping");
              }
            };

            self.getJobClickStatus = function(name){
              var status = StorageService.recover(self.projectId + "_jobstopclicked_"+name);
              if(status === "stopping" || status === "killing"){
                StorageService.store(self.projectId + "_jobstopclicked_"+name, status);
              }
              if(status !== "stopping" && status !== "killing"){
                status = "running";
              }
              return status;
            };

            self.copy = function () {
              var jobType;
              switch (self.currentjob.jobType.toUpperCase()) {
                case "SPARK":
                  jobType = 1;
                  break;
                case "PYSPARK":
                  jobType = 2;
                  break;
                case "FLINK":
                  jobType = 3;
                  break;
              }
              var mainFileTxt, mainFileVal, jobDetailsTxt, sparkState, flinkState;
              if (jobType === 1 || jobType === 2 ) {

                sparkState = {
                  "selectedJar": getFileName(self.currentjob.runConfig.appPath)
                };
                mainFileTxt = "App file";
                mainFileVal = sparkState.selectedJar;
                jobDetailsTxt = "Job details";
              } else if (jobType === 3) {
                flinkState = {
                  "selectedJar": getFileName(self.currentjob.runConfig.appPath)
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
              StorageService.store(self.projectId + "_newjob", state);
              $location.path('project/' + self.projectId + '/newjob');
            };



            var getAllJobs = function () {
              JobService.getJobs(self.projectId, "?expand=creator").then(
                      function (success) {
                          self.jobs = success.data.items;
                          angular.forEach(self.jobs, function (job, key) {
                              job.showing = false;
                          });
                      }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
              });
            };

            self.getNumOfExecution = function () {
              if (self.hasSelectJob) {
                if (self.logset === undefined) {
                  return 0;
                }
                if (self.logset.length > 1) {
                  return self.logset.length;
                } else if (self.logset.length === 1) {
                  return 1;
                } else {
                  return 0;
                }
              }
            };

          /**
           * Retrieve status for all jobs of this project.
           * @returns {undefined}
           */
          self.getRunStatus = function () {
              JobService.getJobs(self.projectId, "?expand=executions(offset=0;limit=1;sort_by=id:desc)").then(
                  function (success) {
                      var jobLatestExecutions = success.data.items;
                      //Construct a map of <jobName,execInfo>
                      self.runningInfo = {};
                      angular.forEach(jobLatestExecutions, function (temp, key) {
                          var name = temp.name;
                          angular.forEach(temp.executions.items, function (temp, key) {
                              var running = self.runningStates.includes(temp.state);
                              self.runningInfo[name] = {
                                  "running": running,
                                  "state": temp.state,
                                  "finalStatus": temp.finalStatus,
                                  "progress": temp.progress,
                                  "duration": temp.duration,
                                  "submissionTime": temp.submissionTime
                              };
                          });
                      });
                      angular.forEach(self.jobs, function (temp, key) {
                          if (typeof self.runningInfo[temp.name] !== "undefined") {
                              if (!self.runningInfo[temp.name].running) {
                                  self.buttonArray[temp.name] = false;
                              }
                          }
                      });
                  }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
                  });
          };

            /**
             * Get data from runningInfo and update jobs.
             * @returns {undefined}
             */
            self.createAppReport = function () {
              angular.forEach(self.jobs, function (temp, key) {
                if (typeof self.runningInfo['' + temp.name] !== "undefined") {
                  if (temp.state !== self.runningInfo['' + temp.name].state && temp.showing === true) {
                    self.showLogs(temp.name);
                  }
                  temp.duration = self.runningInfo['' + temp.name].duration;
                  temp.finalStatus = self.runningInfo['' + temp.name].finalStatus;
                  temp.progress = self.runningInfo['' + temp.name].progress;
                  temp.running = self.runningInfo['' + temp.name].running;
                  temp.state = self.runningInfo['' + temp.name].state;
                  temp.submissionTime = self.runningInfo['' + temp.name].submissionTime;
                  temp.url = self.runningInfo['' + temp.name].url;
                }
              });
            };
            getAllJobs();
            self.getRunStatus();
            self.createAppReport();

            self.runJob = function (job, index) {
                        ProjectService.uberPrice({id: self.projectId}).$promise.then(
                                function (success) {
                                  var gpuPrice = 0;
                                  var generalPrice = 0;
                                  for (var i = 0; i < success.length; i++) {
                                    var multiplicator = success[i];
                                    if (multiplicator.id === "GPU") {
                                      gpuPrice = Math.ceil(parseFloat(multiplicator.multiplicator).toFixed(4) * 100) / 100;
                                    } else if (multiplicator.id === "GENERAL") {
                                      generalPrice = Math.ceil(parseFloat(multiplicator.multiplicator).toFixed(4) * 100) / 100;
                                    }
                                  }
                                  if (typeof job.config['spark.executor.gpus'] === 'undefined' || job.config['spark.executor.gpus'] === 0) {
                                    gpuPrice = 0;
                                  }
                                  ModalService.uberPrice('sm', 'Confirm', 'Do you still want to run this job?', generalPrice, gpuPrice).then(
                                          function (success) {
                                            JobService.runJob(self.projectId, job.name).then(
                                                    function (success) {
                                                      self.toggle(job, index);
                                                      self.buttonClickedToggle(job.name, true);
                                                      StorageService.store(self.projectId + "_jobstopclicked_" + job.name, "running");
                                                      self.getRunStatus();
                                                    }, function (error) {
                                                    if (typeof error.data.usrMsg !== 'undefined') {
                                                        growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                                                    } else {
                                                        growl.error("", {title: error.data.errorMsg, ttl: 8000});
                                                    }
                                            });

                                          }
                                  );

                                }, function (error) {
                                if (typeof error.data.usrMsg !== 'undefined') {
                                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                                } else {
                                    growl.error("", {title: error.data.errorMsg, ttl: 8000});
                                }
                        });
            };

            self.stopJob = function (name) {
              self.stopbuttonClickedToggle(name, "true");
              JobService.stopJob(self.projectId, name).then(
                      function (success) {
                        self.getRunStatus();
                      }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
              });
            };

            self.killJob = function (name) {
              ModalService.confirm('sm', 'Confirm', 'Attemping to stop your job. For streaming jobs this operation can take a few minutes... Do you really want to kill this job and risk losing streaming events?').then(
                function (success) {
                  self.stopJob(name);
                }
              );
            };
            /**
             * Navigate to the new job page.
             * @returns {undefined}
             */
            self.newJob = function () {
              StorageService.remove(self.projectId + '_newjob');
              $location.path('project/' + self.projectId + '/newjob');
              if (self.tourService.currentStep_TourThree > -1) {
                self.tourService.resetTours();
              }
            };



            self.showDetails = function (job) {
              ModalService.jobDetails('lg', job, self.projectId);
            };


            self.exportJob = function (job) {
              var jobConfig = {"jobType": job.jobType, "config": job.config};
              var blob = new Blob([JSON.stringify(jobConfig)], {type:
              'application/json;charset=utf-8;'});
              FileSaver.saveAs(blob, job.name + '_config.json');
            };

            self.showUI = function (job) {
              StorageService.store(self.projectId + "_jobui_" + job.name, job);
              $location.path('project/' + self.projectId + '/jobMonitor-job/' + job.name);

            };

            self.showLogs = function (jobName) {
              self.fetchingLogs = 1;
                JobService.getAllExecutions(self.projectId, jobName).then(
                    function (success) {
                        self.logset = [];
                        angular.forEach(success.data.items, function (temp, key) {
                            var entry = {"jobName": jobName, "executionId": temp.id,  "appId":temp.appId, "time": temp.submissionTime};
                            self.logset.push(entry);
                        });

                        self.fetchingLogs = 0;
                  }, function (error) {
                    self.fetchingLogs = 0;
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
              });
            };

              self.getLog = function (logsetEntry, type) {
                  self.loadingLog = 1;
                  JobService.getLog(self.projectId, logsetEntry.jobName, logsetEntry.executionId, type).then(
                      function (success) {
                          var logContent = success.data;
                          if (logContent['log'] !== undefined) {
                              logsetEntry['log'+type] = logContent['log'];
                          }
                          if (logContent['type'] !== undefined) {
                              logsetEntry['type'] = logContent['type'];
                          }
                          if (logContent[type + 'Path'] !== undefined) {
                              logsetEntry[type + 'Path'] = logContent[type + 'Path'];
                          }
                          if (logContent['retriableErr'] !== undefined) {
                              logsetEntry['retriableErr'] = logContent['retriableErr'];
                          }
                          if (logContent['retriableOut'] !== undefined) {
                              logsetEntry['retriableOut'] = logContent['retriableOut'];
                          }
                          self.loadingLog = 0;
                    }, function (error) {
                        self.loadingLog = 0;
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                });
            };

            self.retryLogs = function (appId, type) {
              if (appId === '' || appId === undefined) {
                growl.error("Can not retry log. The job has not yet been assigned an Id", {title: 'Error', ttl: 5000});
              }
              JobService.retryLog(self.projectId, appId, type).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                        self.showLogs(self.currentjob.name);
                      }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
              });
            };

            self.deleteJob = function (jobName) {
              ModalService.confirm("sm", "Delete Job (" + jobName + ")",
                      "Do you really want to delete this job?\n\
                                This action cannot be undone.")
                      .then(function (success) {
                        JobService.deleteJob(self.projectId, jobName).then(
                                function (success) {
                                  getAllJobs();
                                  self.hasSelectJob = false;
                                  StorageService.remove(self.projectId + "_jobui_" + jobName);
                                  growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                                }, function (error) {
                                if (typeof error.data.usrMsg !== 'undefined') {
                                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                                } else {
                                    growl.error("", {title: error.data.errorMsg, ttl: 8000});
                                }
                        });
                      }, function (cancelled) {
                        growl.info("Delete aborted", {title: 'Info', ttl: 5000});
                      });
            };

            //Called when clicking on a job row
            self.toggle = function (job, index) {
              //reset all jobs showing flag
              angular.forEach(self.jobs, function (temp, key) {
                if (job.name !== temp.name) {
                  temp.showing = false;
                }
              });

              //handle the clicked job accordingly
              job.showing = true;
              self.hasSelectJob = true;
              self.selectedIndex = index;
              self.currentToggledIndex = index;
              self.currentjob = job;
              StorageService.remove(self.projectId + "_jobui_" + job.name)
              StorageService.store(self.projectId + "_jobui_" + job.name, job)

            };

            //untoggle is not used in the jobsCtrl
            ////////////////////////////////////////////////////////////////////
            self.untoggle = function (job, index) {
              StorageService.remove(self.projectId + "_jobui_" + job.name)
              //reset all jobs showing flag
              angular.forEach(self.jobs, function (temp, key) {
                temp.showing = false;
              });

              if (self.currentToggledIndex !== index) {
                self.hasSelectJob = false;
                self.selectedIndex = -1;
                self.currentToggledIndex = -1;
              } else {
                job.showing = true;
              }
            };
            ////////////////////////////////////////////////////////////////////

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

            $scope.convertMS = function (ms) {
              if (ms === undefined) {
                return "";
              }
              var m, s;
              s = Math.floor(ms / 1000);
              m = Math.floor(s / 60);
              s = s % 60;
              if (s.toString().length < 2) {
                s = '0' + s;
              }
              if (m.toString().length < 2) {
                m = '0' + m;
              }
              var ret = m + ":" + s;
              return ret;
            };

            self.toTitleCase = function(str) {
                return str.replace(/\w\S*/g, function(txt){
                    return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
                }).replace(/_/g, ' ');
            }

            var init = function () {
              var stored = StorageService.contains(self.projectId + "_newjob");
              if (stored) {
                  $location.path('project/' + self.projectId + '/newjob');
              }
            };

            init();
          }]);
