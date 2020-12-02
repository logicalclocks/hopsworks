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
                    'TourService', 'ProjectService', 'DataSetService', 'FileSaver', '$mdToast', '$timeout',
          function ($scope, $window, $routeParams, growl, JobService, $location,
          ModalService, $interval, StorageService, TourService, ProjectService, DataSetService, FileSaver, $mdToast,
          $timeout) {

            var self = this;
            self.tourService = TourService;
            self.tourService.currentStep_TourNine = 11; //Feature Store Tour
            self.projectId = $routeParams.projectID;
            var dataSetService = new DataSetService(self.projectId); //The datasetservice for the current project.

            self.jobs = {}; // Will contain all the jobs.
            self.runningStates = ['INITIALIZING', 'CONVERTING_NOTEBOOK', 'RUNNING', 'ACCEPTED', 'NEW', 'NEW_SAVING', 'SUBMITTED',
                  'STARTING_APP_MASTER', 'AGGREGATING_LOGS'];
            self.jobFilter = JobService.getJobFilter();
            self.selectedJobs = {};

            self.loadingLog = false;
            self.showlogs=false;
            self.log = {};
            self.pageSize = 10;
            self.currentPage = 1;

            self.executions = {};
            self.executionsPagination = {};
            self.executionsPageSize = 5;
            self.executionsDefaultCurrentPage = 1;

            self.jobsToDate = new Date();
            self.jobsToDate.setMinutes(self.jobsToDate.getMinutes() + 60*24);
            self.jobsFromDate = new Date();
            self.jobsFromDate.setMinutes(self.jobsToDate.getMinutes() - 60*24*90);

            //Save job sort key and order in local storage
            if(StorageService.contains(self.projectId + '_jobs_sort_key')){
                self.sortKey = StorageService.get(self.projectId + '_jobs_sort_key');
            } else {
                self.sortKey = 'date_created';
                StorageService.store(self.projectId + '_jobs_sort_key', self.sortKey);
            }

            if(StorageService.contains(self.projectId + '_jobs_order_by')){
                self.orderBy = StorageService.get(self.projectId + '_jobs_order_by');
            } else {
                self.orderBy = 'desc';
                StorageService.store(self.projectId + '_jobs_order_by', self.orderBy);
            }

            if(StorageService.contains(self.projectId + '_jobs_reverse')){
                self.reverse = StorageService.get(self.projectId + '_jobs_reverse');
            } else {
                self.reverse = true;
                StorageService.store(self.projectId + '_jobs_reverse', self.reverse);
            }


            self.getAllJobsStatusIsPending = false;
            self.dimTable = false;
            self.totalItems = 0;

            self.sort = function (keyname) {
                //Untoggle current job
                if(self.sortKey !== keyname){
                    self.reverse = true;
                } else {
                    self.reverse = !self.reverse; //if true make it false and vice versa
                }
                StorageService.store(self.projectId + '_jobs_reverse', self.reverse);
                self.sortKey = keyname;   //set the sortKey to the param passed
                StorageService.store(self.projectId + '_jobs_sort_key', self.sortKey);
                self.order();
                //We need to fetch all the jobs for these
                self.getAllJobsStatus(true, true);
            };

            self.sortExecutions = function (jobName, keyname) {
                if (self.executionsPagination[jobName].sort !== keyname) {
                    self.executionsPagination[jobName].reverse = true;
                } else {
                    self.executionsPagination[jobName].reverse = !self.executionsPagination[jobName].reverse;
                }
                self.executionsPagination[jobName].sort = keyname;   //set the sortKey to the param passed
                self.orderExecutions(jobName);
                //We need to fetch all the jobs for these
                self.getJobExecutions(jobName, true);
            };

            self.order = function () {
                if (self.reverse) {
                    self.orderBy = "desc";
                } else {
                    self.orderBy = "asc";
                }
                StorageService.store(self.projectId + '_jobs_order_by', self.orderBy);
            };

            self.orderExecutions = function (jobName) {
                if (self.executionsPagination[jobName].reverse) {
                    self.executionsPagination[jobName].order = 'desc';
                } else {
                    self.executionsPagination[jobName].order = 'asc';
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
                self.refreshSlider();
                self.copy(job);
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
              self.showError = function (error, msg, id) {
                  var errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : msg;
                  var refId = typeof id === "undefined"? 4:id;
                  growl.error(errorMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: refId});
              };

              self.downloadLog = function () {
                  dataSetService.getDownloadToken(self.log.path, 'DATASET').then(
                      function (success) {
                          var token = success.data.data.value;
                          $mdToast.hide();
                          dataSetService.download(self.log.path, token, 'DATASET');
                      }, function (error) {
                          $mdToast.hide();
                          self.showError(error, '', 4);
                      });
              };

            self.copy = function (job) {
              StorageService.store(self.projectId + "_newjob", JobService.getJobState(job));
              $location.path('project/' + self.projectId + '/newjob');
            };


            self.getAllJobsStatus = function (toDimTable, overwriteJobs, limit, offset, sortBy, expansion) {
                if(self.getAllJobsStatusIsPending === true){
                    return;
                }
                self.getAllJobsStatusIsPending = true;
                if(typeof toDimTable !== 'undefined' && toDimTable && self.jobs.length !== 0) {
                    self.dimTable = true;
                } else {
                    self.dimTable = false;
                }
                if(typeof limit === 'undefined'){
                    limit = self.pageSize;
                }
                if(typeof offset === 'undefined'){
                    offset = self.pageSize * (self.currentPage - 1);
                }
                if(typeof expansion === 'undefined'){
                    //Just to get the executions count
                    expansion = "&expand=executions(offset=0;limit=1;filter_by=state:" + self.runningStates.toString().replace(" " ,"") + ")&expand=creator";
                }
                if(typeof sortBy === 'undefined'){
                    //Here we want to use the *_latest sortBy keys of Hopsworks, which return a view of the jobs
                    // sorted by their latest execution (=subresource) attributes
                    sortBy = "&sort_by=" + self.sortKey.toLowerCase() + ":" + self.orderBy.toLowerCase();
                }

                var filterBy = "&filter_by=date_created_lt:" + self.jobsToDate.toISOString().replace('Z','')
                    + "&filter_by=date_created_gt:" + self.jobsFromDate.toISOString().replace('Z','');
                if(typeof self.jobFilter !== 'undefined' && self.jobFilter !== "") {
                    filterBy += "&filter_by=latest_execution:" + self.jobFilter;
                }

                JobService.getJobs(self.projectId, limit, offset, sortBy + filterBy + expansion).then(
                    function (success) {
                        if(success.data.count === 0){
                          self.getAllJobsStatusIsPending = false;
                          self.jobs = {};
                          self.totalItems = 0;
                          return;
                        }
                        //If jobs fetched from Hopsworks are non the same count as in browser, reset jobs table
                        //Otherwise just set values, we do this to avoid unfocusing the job dropdown menu
                        if(overwriteJobs || Object.keys(self.jobs).length !== success.data.items.length ){
                            self.jobs = {};
                        }
                        angular.forEach(success.data.items, function (job, key) {
                            if (typeof self.jobs[job.name] === "undefined" || !(job.name in self.jobs)) {
                                self.jobs[job.name] = job;
                            }
                            self.jobs[job.name]['running'] = job.executions.count;
                            delete self.jobs[job.name].executions;
                            delete self.jobs["length"];
                            if (typeof self.executionsPagination[job.name] === "undefined"){
                                self.executionsPagination[job.name] = {"pageSize": self.executionsPageSize};
                                self.executionsPagination[job.name]["currentPage"] = self.executionsDefaultCurrentPage;
                                self.executionsPagination[job.name]["sort"] = 'submissiontime';
                                self.executionsPagination[job.name]["order"] = 'desc';
                                self.executionsPagination[job.name]["reverse"] = 'true';
                            }

                        });

                        self.totalItems = 0;
                        if(typeof success.data.count !== 'undefined' && success.data.count !== null) {
                            self.totalItems = success.data.count;
                        }

                        Object.keys(self.jobs).forEach(function(key) {
                            if(key in self.selectedJobs){
                                self.getJobExecutions(key, false);
                            }
                        });

                        self.getAllJobsStatusIsPending = false;
                        if(self.tourService.currentStep_TourNine === 11){
                            self.tourService.currentStep_TourNine = 12 //Feature Store tour
                        }
                        if(typeof toDimTable !== 'undefined' && toDimTable){
                            self.dimTable = false;
                        }

                    }, function (error) {
                        if(typeof toDimTable !== 'undefined' && toDimTable){
                            self.dimTable = false;
                        }
                        self.getAllJobsStatusIsPending = false;
                        if(self.tourService.currentStep_TourNine === 11){
                            self.tourService.currentStep_TourNine = 12 //Feature Store tour
                        }
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                    });
            };

              self.getJobExecutions = function(jobName, overwriteExecutions, limit, offset, sortBy, expansion) {

                  if(limit == null || typeof limit === 'undefined'){
                      limit = self.executionsPagination[jobName]["pageSize"];
                  }
                  if(offset == null || typeof offset === 'undefined'){
                      offset = self.executionsPagination[jobName]["pageSize"] * (self.executionsPagination[jobName]["currentPage"] - 1);
                  }
                  if(typeof expansion === 'undefined'){
                      //Just to get the executions count
                      expansion = "&expand=user";
                  }
                  if(typeof sortBy === 'undefined'){
                      //Here we want to use the *_latest sortBy keys of Hopsworks, which return a view of the jobs
                      // sorted by their latest execution (=subresource) attributes
                      sortBy = "&sort_by="
                          + self.executionsPagination[jobName].sort.toLowerCase()
                          + ":"
                          + self.executionsPagination[jobName].order.toLowerCase();

                      if (self.executionsPagination[jobName].sort === "user"){
                          sortBy += ',id:desc'
                      }
                  }

                  JobService.getAllExecutions(self.projectId, jobName, "?offset=" + offset + "&limit=" + limit + sortBy + expansion).then(
                      function (success) {
                          if (overwriteExecutions || success.data.count === 0 || (typeof self.executions[jobName] === "undefined" && success.data.count > 0) || (typeof self.executions[jobName] !== "undefined" && self.executions[jobName].length !== success.data.items.length)) {
                              self.executions[jobName] = [];
                              angular.forEach(success.data.items, function (execution, key) {
                                  self.executions[jobName].push(execution);
                               });
                          } else {
                              //Add all new executions
                              angular.forEach(success.data.items, function (execution, key) {

                                  self.executions[jobName][key].user = execution.user;
                                  self.executions[jobName][key].submissionTime = execution.submissionTime;
                                  self.executions[jobName][key].stdoutPath = execution.stdoutPath;
                                  self.executions[jobName][key].stderrPath = execution.stderrPath;
                                  self.executions[jobName][key].appId = execution.appId;
                                  self.executions[jobName][key].hdfsUser = execution.hdfsUser;
                                  self.executions[jobName][key].args = execution.args;
                                  self.executions[jobName][key].finalStatus = execution.finalStatus;
                                  self.executions[jobName][key].state = execution.state;
                                  self.executions[jobName][key].progress = execution.progress;
                                  self.executions[jobName][key].duration = execution.duration;
                                  if(typeof execution.flinkMasterURL !== "undefined"){
                                      self.executions[jobName][key].flinkMasterURL = execution.flinkMasterURL;
                                  }
                              });
                          }
                          self.executionsPagination[jobName]["totalItems"] = success.data.count;
                      }, function (error) {
                          if (typeof error.data.usrMsg !== 'undefined') {
                              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                          } else {
                              growl.error("", {title: error.data.errorMsg, ttl: 8000});
                          }
                      });
              };

              self.getJobsNextPage = function (job) {
                  var offset = self.pageSize * (self.currentPage - 1);
                  if (self.totalItems > offset) {
                      self.getAllJobsStatus(true, true, null, offset);
                  }
              };

              self.getExecutionsNextPage = function (job) {
                  var offset = self.executionsPagination[job.name]["pageSize"] * (self.executionsPagination[job.name]["currentPage"] - 1);
                  if (self.executionsPagination[job.name]["totalItems"]> offset) {
                      self.getJobExecutions(job.name, true, null, offset);
                  }
              };


            self.getNumOfExecution = function (jobName) {
              if (self.hasSelectJob) {
                if (typeof self.executionTotalItems === 'undefined') {
                  return 0;
                } else {
                    return self.executionTotalItems;
                }
              }
              return 0;
            };

              self.getJobValues = function () {
                  return Object.values(self.jobs);
              };

              self.executionsEmpty = function (obj) {
                  return Object.keys(obj).length === 0;
              };

              self.runJob = function (job, index) {
                  self.toggleOn(job, index);
                  //If type Flink, run immediately
                  if (job.jobType.toUpperCase() === 'FLINK'){
                      JobService.runJob(self.projectId, job.name).then(
                          function (success) {
                              self.getAllJobsStatus();
                          }, function (error) {
                              if (typeof error.data.usrMsg !== 'undefined') {
                                  growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                              } else {
                                  growl.error("", {title: error.data.errorMsg, ttl: 8000});
                              }
                          });
                  }
                   else {
                     var args;
                     if (job.config.defaultArgs) {
                        JobService.runJob(self.projectId, job.name, null).then(
                            function (success) {
                                self.getAllJobsStatus();
                                self.getJobExecutions(job.name, true);
                            }, function (error) {
                                if (typeof error.data.usrMsg !== 'undefined') {
                                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                                } else {
                                    growl.error("", {title: error.data.errorMsg, ttl: 8000});
                                }
                            });
                     } else {
                        ModalService.jobArgs('md', 'Confirm', 'Input Arguments', args).then(
                            function (success) {
                                 JobService.runJob(self.projectId, job.name, success.args).then(
                                     function (success) {
                                         self.getAllJobsStatus();
                                         self.getJobExecutions(job.name, true);
                                     }, function (error) {
                                         if (typeof error.data.usrMsg !== 'undefined') {
                                             growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                                         } else {
                                             growl.error("", {title: error.data.errorMsg, ttl: 8000});
                                         }
                                     });
                            });
                     }
                  }
              };

              self.showExecutionDetails = function (job, execution) {
                  ModalService.executionDetails('lg', job, execution).then(
                      function (success) {
                      }
                  );
              };

              self.stopJob = function (jobName) {
                  ModalService.confirm("sm", "Stop Job", "Do you really want to stop all runs of this job?")
                      .then(function (success) {
                          // Stop all executions of the job
                          JobService.getAllExecutions(self.projectId, jobName, "?filter_by=state:" + self.runningStates.toString().replace(" " ,"")).then(
                              function (success) {
                                  for (var execId in success.data.items) {
                                      if (self.runningStates.includes(success.data.items[execId].state)) {
                                          JobService.stopExecution(self.projectId, jobName, success.data.items[execId].id).then(
                                              function (success) {
                                                  growl.success("Stopping, please wait...", {title: 'Success', ttl: 5000});
                                              }, function (error) {
                                                  if (typeof error.data.usrMsg !== 'undefined') {
                                                      growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                                                  } else {
                                                      growl.error("", {title: error.data.errorMsg, ttl: 8000});
                                                  }
                                              });
                                      }
                                  }
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

              self.stopExecution = function (jobName, executionId) {
                  ModalService.confirm("sm", "Stop run", "Do you really want to stop this run?")
                      .then(function (success) {
                          JobService.stopExecution(self.projectId, jobName, executionId).then(
                              function (success) {
                                  growl.success("Run is stopping, please wait...", {title: 'Success', ttl: 5000});
                              }, function (error) {
                                  if (typeof error.data.usrMsg !== 'undefined') {
                                      growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                                  } else {
                                      growl.error("", {title: error.data.errorMsg, ttl: 8000});
                                  }
                              });
                      }, function (cancelled) {
                          // growl.info("Stop aborted", {title: 'Info', ttl: 5000});
                      });
              };

              self.deleteExecution = function (jobName, executionId) {
                  ModalService.confirm("lg", "Delete run", "Do you really want to delete this run?" +
                      " If it is still ongoing, it will stop abruptly. If it is finished already, logs will" +
                      " remain in the Logs dataset.")
                      .then(function (success) {
                          JobService.deleteExecution(self.projectId, jobName, executionId).then(
                              function (success) {
                                  growl.success("Run is being deleted...", {title: 'Success', ttl: 5000});
                              }, function (error) {
                                  if (typeof error.data.usrMsg !== 'undefined') {
                                      growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                                  } else {
                                      growl.error("", {title: error.data.errorMsg, ttl: 8000});
                                  }
                              });
                      }, function (cancelled) {
                      });
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

            self.showExecutionUI = function (execution) {
              var id = "";
              if(typeof execution.appId !== "undefined"){
                  id = execution.appId;
              } else {
                  id = execution.id;
              }
              $location.path('project/' + self.projectId + '/jobMonitor-app/' + id);
            };


            self.getLog = function (jobName, executionId, type) {
                  self.loadingLog = true;

                  JobService.getLog(self.projectId, jobName, executionId, type).then(
                      function (success) {
                          self.log = {};
                          if (typeof success.data['log'] !== 'undefined') {
                              self.log = {"log" : '<html><body><text><div >' + success.data['log'] + '</div></text></body></html>' , 'type': type};
                          }
                          if (typeof success.data['path'] !== 'undefined') {
                              self.log['path'] = success.data['path'];
                          }
                          self.loadingLog = false;
                          self.showlogs = true;
                    }, function (error) {
                        self.loadingLog = false;
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                });
            };

            self.retryLogs = function (jobName, appId, type) {
              if (typeof appId === 'undefined' || appId === '' ) {
                growl.error("Can not retry log. The job has not yet been assigned an Id", {title: 'Error', ttl: 5000});
              }
              JobService.retryLog(self.projectId, appId, type).then(
                      function (success) {
                        growl.success("Retry was successful", {title: 'Success', ttl: 5000});
                        self.showLogs(jobName);
                      }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
              });
            };

            self.deleteJob = function (jobName) {
              ModalService.confirm("md", "Delete Job (" + jobName + ")",
                      "Do you really want to delete this job? This action cannot be undone. All runs will stop!")
                      .then(function (success) {
                        JobService.deleteJob(self.projectId, jobName).then(
                                function (success) {
                                  delete self.jobs[jobName];
                                  if(typeof self.selectedJobs[jobName] !== "undefined") {
                                      self.toggleOff(jobName, self.selectedJobs[jobName]["index"]);
                                  }
                                  self.getAllJobsStatus();
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
                if(typeof job !== "undefined") {
                    if (job.name in self.selectedJobs) {
                        self.toggleOff(job, index);
                    } else {
                        self.toggleOn(job, index);
                    }
                }
            };

            self.toggleOff = function (job, index) {
              delete self.selectedJobs[job.name];
              delete self.executions[job.name];
            };

            self.toggleOn = function (job, index) {
                  self.selectedJobs[job.name] = index;
                  var offset = self.executionsPagination[job.name]["pageSize"] * (self.executionsPagination[job.name]["currentPage"] - 1);
                  self.getJobExecutions(job.name, true, null, offset);
            };

            self.initJobs = function(job, index){
                if(typeof job !== "undefined" && job.running){
                    self.toggleOn(job, index);
                }
            };


            $scope.$watch('jobsCtrl.jobFilter', function (val) {
                self.getAllJobsStatus(true, true);
            });


            /**
             * Close the poller if the controller is destroyed.
             */
            $scope.$on('$destroy', function () {
              $interval.cancel(self.poller);
            });


            $scope.convertMS = function (ms) {
              if (typeof ms === 'undefined') {
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
              } else {
                  //Set/recover user dates
                  //If dates are in local storage, retrieve them and set them. Otherwise set default values

                  if (StorageService.contains(self.projectId + "_jobsToDate")) {
                      var to = StorageService.get(self.projectId + "_jobsToDate");
                      self.jobsToDate = new Date(to);
                  } else {
                      self.jobsToDate.setMinutes(self.jobsToDate.getMinutes() + 60*24);
                  }
                  if (StorageService.contains(self.projectId + "_jobsFromDate")) {
                      var from = StorageService.get(self.projectId + "_jobsFromDate");
                      self.jobsFromDate = new Date(from);
                  } else {
                      self.jobsFromDate.setMinutes(self.jobsToDate.getMinutes() - 60*24*31);
                  }
                  if (StorageService.contains(self.projectId + "_jobsPageSize")) {
                      self.pageSize = StorageService.get(self.projectId + "_jobsPageSize");
                  } else {
                      self.pageSize = 10;
                  }
                  if (StorageService.contains(self.projectId + "_executionsPageSize")) {
                      self.executionsPageSize = StorageService.get(self.projectId + "_executionsPageSize");
                  } else {
                      self.executionsPageSize = 5;
                  }
              }
            };

            self.storeDates = function (type) {
                if (type === "jobsFromDate") {
                    self.jobsFromDate.setHours(0,0,0,0);
                    StorageService.store(self.projectId + "_" + type, self.jobsFromDate.getTime());
                } else if (type === "jobsToDate") {
                    self.jobsToDate.setMinutes(self.jobsToDate.getMinutes() + 60*25);
                    StorageService.store(self.projectId + "_" + type, self.jobsToDate.getTime());
                }
            };

            self.storePageSize = function() {
              StorageService.store(self.projectId + "_jobsPageSize", self.pageSize);
            };

            self.storeExecutionsPageSize = function() {
                  StorageService.store(self.projectId + "_executionsPageSize", self.executionsPageSize);
            };

            self.isRunningState = function(state){
                return self.runningStates.includes(state);
            };

            init();

            var startPolling = function () {
              self.poller = $interval(function () {
                  if (self.getAllJobsStatusIsPending) {
                      return;
                  }
                  self.getAllJobsStatus(false, false);
              }, 5000);
            };
            startPolling();
          }]);
