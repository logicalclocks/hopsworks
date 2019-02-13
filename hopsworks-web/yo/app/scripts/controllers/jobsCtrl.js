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

angular.module('hopsWorksApp')
        .controller('JobsCtrl', ['$scope', '$window', '$routeParams', 'growl',
        'JobService', '$location', 'ModalService', '$interval', 'StorageService',
                    'TourService', 'ProjectService','FileSaver', '$timeout',
          function ($scope, $window, $routeParams, growl, JobService, $location,
          ModalService, $interval, StorageService, TourService, ProjectService, FileSaver,
          $timeout) {

            var self = this;
            self.tourService = TourService;
            self.tourService.currentStep_TourNine = 10; //Feature Store Tour
            self.projectId = $routeParams.projectID;
            self.jobs = []; // Will contain all the jobs.
            self.runningStates = ['INITIALIZING', 'RUNNING', 'ACCEPTED', 'NEW', 'NEW_SAVING', 'SUBMITTED',
                  'STARTING_APP_MASTER', 'AGGREGATING_LOGS'];
            self.buttonArray = [];
            self.workingArray = [];
            self.jobFilter = JobService.getJobFilter();
            self.hasSelectJob = false;
            self.starting = [];

            self.currentjob = null;
            self.currentToggledIndex = -1;
            self.fetchingLogs = 0;
            self.loadingLog = 0;
            self.pageSize = 10;
            self.executionsPageSize = 5;
            self.executionsCurrentPage = 1;

            self.jobsToDate = new Date();
            self.jobsToDate.setMinutes(self.jobsToDate.getMinutes() + 60*24);
            self.jobsFromDate = new Date();
            self.jobsFromDate.setMonth(self.jobsToDate.getMonth() - 1);

            self.sortKey = 'date_created';
            self.orderBy = "desc";
            self.reverse = true;
            self.getAllJobsStatusIsPending = false;
            self.dimTable = false;

            self.sort = function (keyname) {
                //Untoggle current job
                self.untoggle(self.jobs[self.selectedIndex], self.selectedIndex);
                if(self.sortKey !== keyname){
                    self.reverse = true;
                } else {
                    self.reverse = !self.reverse; //if true make it false and vice versa
                }
                self.sortKey = keyname;   //set the sortKey to the param passed
                self.order();
                //We need to fetch all the jobs for these
                self.getAllJobsStatus(true, true);
            };

            self.order = function () {
                if (self.reverse) {
                    self.orderBy = "desc";
                } else {
                    self.orderBy = "asc";
                }
            };

            self.refreshSlider = function () {
                $timeout(function () {
                $scope.$broadcast('rzSliderForceRender');
              });
            };

            /**
             * Helper function for redirecting to another project page
             *
             * @param serviceName project page
             */
            self.goToUrl = function (serviceName) {
                $location.path('project/' + self.projectId + '/' + serviceName);
            };

            self.editAsNew = function (job) {
                self.currentjob = job;
                self.currentjob.runConfig = job.config;
                self.refreshSlider();
                self.copy();
            };

            self.makeACopy = function (config, i){
                //Try to find latest available copy of doc
                JobService.getJob(self.projectId, config.appName + "-Copy" + i).then(
                    function (success) {
                        i++;
                        self.makeACopy(config, i);
                    }, function (error) {
                        config.appName = config.appName + "-Copy" + i;
                        JobService.putJob(self.projectId, config).then(
                            function (success) {
                                self.getAllJobsStatus(true, true);
                                return;
                            }, function (error) {
                                if (typeof error.data.usrMsg !== 'undefined') {
                                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                                } else {
                                    growl.error("", {title: error.data.errorMsg, ttl: 8000});
                                }
                                return;
                            });
                    });
            };

              self.buttonClickedToggle = function (name, display) {
              self.buttonArray[name] = display;
              self.workingArray[name] = "true";
            };

            self.stopButtonClickedToggle = function (name, display) {
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
                  "isOpen": true,
                  "visible": true,
                  "value": "",
                  "title": jobDetailsTxt},
                "accordion5": {//Contains the configuration and creation
                  "isOpen": true,
                  "visible": true,
                  "value": "",
                  "title": "Configure and create"}
              };
              StorageService.store(self.projectId + "_newjob", state);
              $location.path('project/' + self.projectId + '/newjob');
            };


            self.pageSize = 8;
            self.currentPage = 1;

            self.getAllJobsStatus = function (toDimTable, overwriteJobs, limit, offset, sortBy, expansion) {
                if(self.getAllJobsStatusIsPending === true){
                    return;
                }
                self.getAllJobsStatusIsPending = true;
                if(toDimTable !== undefined && toDimTable !== null && toDimTable) {
                    self.dimTable = true;
                } else {
                    self.dimTable = false;
                }
                //var jobsTemp = [];
                if(limit === undefined || limit === null){
                    limit = self.pageSize;
                }
                if(offset === undefined || offset === null){
                    offset = self.pageSize * (self.currentPage - 1);
                }
                if(expansion === undefined || expansion === null){
                    expansion = "&expand=executions(offset=0;limit=1;sort_by=id:desc)&expand=creator";
                }
                if(sortBy === undefined || sortBy === null){
                    //Here we want to use the *_latest sortBy keys of Hopsworks, which return a view of the jobs
                    // sorted by their latest execution (=subresource) attributes
                    sortBy = "&sort_by=" + self.sortKey.toLowerCase() + ":" + self.orderBy.toLowerCase();
                }

                var filterBy = "&filter_by=date_created_lt:" + self.jobsToDate.toISOString().replace('Z','')
                    + "&filter_by=date_created_gt:" + self.jobsFromDate.toISOString().replace('Z','');
                if(self.jobFilter !== undefined && self.jobFilter !== null && self.jobFilter !== "") {
                    filterBy += "&filter_by=latest_execution:" + self.jobFilter;
                }

                JobService.getJobs(self.projectId, limit, offset, sortBy + filterBy + expansion).then(
                    function (success) {
                        if(success.data.count === 0){
                          self.getAllJobsStatusIsPending = false;
                          self.jobs.length = 0;
                          return;
                        }
                        //If jobs fetched from Hopsworks are non the same count as in browser, reset jobs table
                        //Otherwise just set values, we do this to avoid unfocusing the job dropdown menu
                        if(overwriteJobs || self.jobs.length !== success.data.items.length ){
                            self.jobs.length = 0;
                        }
                        var i=0;
                        //Construct an array of jobs and their latest execution info
                        angular.forEach(success.data.items, function (job, key) {
                            if(self.jobs[i] === undefined){
                                self.jobs[i] = {};
                            }
                            self.jobs[i].name = job.name;
                            self.jobs[i].id = job.id;
                            self.jobs[i].creationTime = job.creationTime;
                            self.jobs[i].jobType = job.jobType;
                            self.jobs[i].creator = job.creator;
                            self.jobs[i].config = job.config;

                            if(job.executions.items !== undefined && job.executions.items !== null) {
                                self.jobs[i].running = self.runningStates.includes(job.executions.items[0].state);
                                self.jobs[i].showing = false;
                                self.jobs[i].state = job.executions.items[0].state;
                                self.jobs[i].finalStatus = job.executions.items[0].finalStatus;
                                self.jobs[i].progress = job.executions.items[0].progress;
                                self.jobs[i].duration = job.executions.items[0].duration;
                                self.jobs[i].submissionTime = job.executions.items[0].submissionTime;

                                if (self.currentjob != null
                                    && self.currentjob.name === job.name
                                    && job.executions.items[0].state !== self.currentjob.state
                                    && self.currentjob.showing === true) {
                                    self.showLogs(job.name);
                                }
                            }
                            i++;
                        });

                        self.totalItems = 0;
                        if(success.data.count !== undefined && success.data.count !== null) {
                            self.totalItems = success.data.count;
                        }

                        angular.forEach(self.jobs, function (job, key) {
                            if (!job.running) {
                                self.buttonArray[job.name] = false;
                            }
                        });
                        self.getAllJobsStatusIsPending = false;
                        if(self.tourService.currentStep_TourNine === 10){
                            self.tourService.currentStep_TourNine = 11 //Feature Store tour
                        }
                        if(toDimTable !== undefined && toDimTable !== null && toDimTable){
                            self.dimTable = false;
                        }
                    }, function (error) {
                        if(toDimTable !== undefined && toDimTable !== null && toDimTable){
                            self.dimTable = false;
                        }
                        self.getAllJobsStatusIsPending = false;
                        if(self.tourService.currentStep_TourNine === 10){
                            self.tourService.currentStep_TourNine = 11 //Feature Store tour
                        }
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                    });
            };

            self.getJobsNextPage = function () {
                self.untoggle(self.currentjob, self.selectedIndex);
                var offset = self.pageSize * (self.currentPage - 1);
                if (self.totalItems > offset) {
                    self.getAllJobsStatus(true, true, null, offset);
                }
            };

            self.getNumOfExecution = function () {
              if (self.hasSelectJob) {
                if (self.executionTotalItems === undefined) {
                  return 0;
                } else {
                    return self.executionTotalItems;
                }
              }
              return 0;
            };

            self.getAllJobsStatus();

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
                                self.starting[job.name] = true;
                                JobService.runJob(self.projectId, job.name).then(
                                        function (success) {
                                          self.starting[job.name] = false;
                                          self.toggle(job, index);
                                          self.buttonClickedToggle(job.name, true);
                                          StorageService.store(self.projectId + "_jobstopclicked_" + job.name, "running");
                                          self.getAllJobsStatus();
                                        }, function (error) {
                                          self.starting[job.name] = false;
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
              self.stopButtonClickedToggle(name, "true");
              JobService.stopJob(self.projectId, name).then(
                  function (success) {
                    self.getAllJobsStatus();
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
            self.logset = [];
            self.showLogs = function (jobName) {
                self.fetchingLogs = 1;
                var offset = self.executionsPageSize * (self.executionsCurrentPage - 1);
                JobService.getAllExecutions(self.projectId, jobName, "?offset="+offset+ "&limit="+self.executionsPageSize + "&sort_by=id:desc").then(
                    function (success) {
                        self.logset.length = 0;
                        angular.forEach(success.data.items, function (execution, key) {
                            var entry = {"jobName": jobName, "executionId": execution.id,  "time": execution.submissionTime};
                            if(execution.appId === undefined){
                                entry["appId"]  = "N/A";
                            } else {
                                entry["appId"] = execution.appId;
                            }
                            self.logset.push(entry);
                        });
                        self.executionTotalItems = success.data.count;
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

              self.getExecutionsNextPage = function () {
                  var offset = self.executionsPageSize * (self.executionsCurrentPage - 1);
                  if (self.executionTotalItems > offset) {
                      self.showLogs(self.currentjob.name);
                  }
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
                          if (logContent['path'] !== undefined) {
                              logsetEntry[type + 'path'] = logContent['path'];
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
                        growl.success("Retry was successful", {title: 'Success', ttl: 5000});
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
                      "Do you really want to delete this job? This action cannot be undone.")
                      .then(function (success) {
                        JobService.deleteJob(self.projectId, jobName).then(
                                function (success) {
                                  //Iterate jobs array and remove job
                                  for(var i = self.jobs.length -1; i >= 0 ; i--){
                                    if(self.jobs[i].name === jobName){
                                       self.jobs.splice(i, 1);
                                    }
                                  }
                                  self.untoggle(self.currentjob, self.selectedIndex);
                                  self.logset.length = 0;

                                  StorageService.remove(self.projectId + "_jobui_" + jobName);
                                  growl.success("Job was deleted.", {title: 'Success', ttl: 5000});
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


            self.toggle = function (job, index) {
              //reset all jobs showing flag
              angular.forEach(self.jobs, function (job, key) {
                if (job.name !== job.name) {
                    job.showing = false;
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


            ////////////////////////////////////////////////////////////////////
            self.untoggle = function (job, index) {
              if((job === undefined || job === null) && self.currentjob !== undefined && self.currentjob !== null) {
                  job = self.currentjob;
              } else  {
                  return;
              }
              StorageService.remove(self.projectId + "_jobui_" + job.name);
              //reset all jobs showing flag
              for (var i = 0; i < self.jobs.length; i++) {
                  self.jobs[i].showing = false;
              }

                self.hasSelectJob = false;
                self.selectedIndex = -1;
                self.currentToggledIndex = -1;
            };
            ////////////////////////////////////////////////////////////////////

              $scope.$watch('jobsCtrl.jobFilter', function (val) {
                self.getAllJobsStatus(true, true);
            });


            /**
             * Close the poller if the controller is destroyed.
             */
            $scope.$on('$destroy', function () {
              $interval.cancel(self.poller);
            });

            var startPolling = function () {
              self.poller = $interval(function () {
                if (self.getAllJobsStatusIsPending){
                    return;
                }
                self.getAllJobsStatus(false, false);
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
            };

            var init = function () {
              var stored = StorageService.contains(self.projectId + "_newjob");
              if (stored) {
                  $location.path('project/' + self.projectId + '/newjob');
              }
            };

            init();
          }]);
