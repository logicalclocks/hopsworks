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
/*
 * Controller for the job UI dialog. 
 */
angular.module('hopsWorksApp')
        .controller('JobUICtrl', ['$scope', '$timeout', 'growl', 'ProjectService', 'JobService', '$interval', 'StorageService',
          '$routeParams', '$route', '$sce', '$window',
          function ($scope, $timeout, growl, ProjectService, JobService, $interval, StorageService,
                  $routeParams, $route, $sce, $window) {

            var self = this;
            self.job;
            self.jobtype; //Holds the type of job.
            self.execFile; //Holds the name of the main execution file
            self.showExecutions = false;
            self.projectId = $routeParams.projectID;
            self.jobName = $routeParams.name;
            self.appId = $routeParams.appId;
            self.appIds = [];
            self.ui = "";
            self.isLivy = $routeParams.isLivy;
            self.current = "";
            self.loading = false;
            self.loadingText = "";
            self.sessions = [];
            self.tbUrls = [];
            self.session;
            self.tfExecutorId;

            var startLoading = function (label) {
              self.loading = true;
              self.loadingText = label;
            };
            var stopLoading = function () {
              self.loading = false;
              self.loadingText = "";
            };
            var getAppId = function (callback) {
              if (self.appId === undefined || self.appId === null || self.appId === "") {
                JobService.getAllExecutions(self.projectId, self.job.name, '?sort_by=submissiontime:desc&offset=0&limit=1').then(
                        function (success) {
                            if(typeof success.data.items !== 'undefined') {
                                self.appId = success.data.items[0].appId;
                                getTensorBoardUrls();
                                callback();
                            }
                        }, function (error) {

                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                  stopLoading();
                });
              } else {
                callback();
              }
            };
            var getAppIds = function () {
              if (self.job) {
                JobService.getAllExecutions(self.projectId, self.job.name, "").then(
                        function (success) {
                          //Loop through executions and set appIds
                          angular.forEach(success.data.items, function (execution, key) {
                            self.appIds.push({id:execution.appId});
                          });
                        }, function (error) {

                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                  stopLoading();
                });
              }
            };
            var getJobUI = function () {
              if (self.jobName !== undefined && self.jobName !== null && self.jobName !== "") {
                self.job = StorageService.recover(self.projectId + "_jobui_" + self.jobName);
                StorageService.store(self.projectId + "_jobui_" + self.jobName, self.job);
                self.jobType = self.job.jobType;
              }
              if (self.job || self.appId) {
                getAppIds();
                getAppId(getJobUIInt);
              }
            };


            var getJobUIInt = function () {
              //If job is not flink
                if (!self.isLivy && (self.job.jobType === 'FLINK' || self.job.jobType === 'BEAM_FLINK')) {
                    //Get Flink master url from job
                    self.ui = '/hopsworks-api/flinkmaster/' + self.appId + '/';
                    JobService.getFlinkMaster(self.appId).then(
                        function (success) {
                        }, function (error) {
                            self.ui = '/hopsworks-api/flinkhistoryserver/';
                    });
                    var iframe = document.getElementById('ui_iframe');
                    if (iframe) {
                        iframe.src = $sce.trustAsResourceUrl(self.ui);
                    }
                    $timeout(stopLoading(), 2000);
                } else {
                  JobService.getExecutionUI(self.projectId, self.appId, self.isLivy).then(
                      function (success) {
                          self.sessions = success.data;
                          if (self.sessions.length > 0) {
                              self.session = self.sessions[0];
                              self.ui = self.session.url;
                              self.current = "jobUI";
                              if (self.ui !== "") {
                                  var iframe = document.getElementById('ui_iframe');
                                  if (iframe) {
                                      iframe.src = $sce.trustAsResourceUrl(self.ui);
                                  }
                                  $timeout(stopLoading(), 2000);
                              }
                          }

                      }, function (error) {

                          if (typeof error.data.usrMsg !== 'undefined') {
                              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                          } else {
                              growl.error("", {title: error.data.errorMsg, ttl: 8000});
                          }
                          stopLoading();

                      });
              }
            };
            self.jobUI = function () {
              if (self.job === undefined || self.job === null) {
                  if (self.jobName !== undefined && self.jobName !== null && self.jobName !== "") {
                  self.job = StorageService.recover(self.projectId + "_jobui_" + self.jobName);
                  StorageService.store(self.projectId + "_jobui_" + self.jobName, self.job);
                }
              }

              startLoading("Loading Job UI...");
              getAppId(getJobUIInt);
            };

            self.flinkHistoryServer = function (){
                var iframe = document.getElementById('ui_iframe');
                if (iframe) {
                    iframe.src = $sce.trustAsResourceUrl('/hopsworks-api/flinkhistoryserver/');
                }
                $timeout(stopLoading(), 2000);
            };

            self.yarnUI = function () {

              if (self.job === undefined || self.job === null) {
                if (self.jobName !== undefined && self.jobName !== null && self.jobName !== "") {
                  self.job = StorageService.recover(self.projectId + "_jobui_" + self.jobName);
                  StorageService.store(self.projectId + "_jobui_" + self.jobName, self.job);
                }
              }

              startLoading("Loading YARN UI...");
              getAppId(yarnUIInt);
            };
            var yarnUIInt = function () {
                self.ui = '/hopsworks-api/yarnui/cluster/app/' + self.appId;
                        self.current = "yarnUI";
                        var iframe = document.getElementById('ui_iframe');
                        if (iframe !== null) {
                          iframe.src = $sce.trustAsResourceUrl(self.ui);
                        }
                        // This timeout is ignored when the iframe is loaded, replacing the overlay
                        $timeout(stopLoading(), 5000);
            };
            self.kibanaUI = function () {
              getAppId(kibanaUIInt);
            };
            var kibanaUIInt = function () {
              if (self.job === undefined || self.job === null) {
                  ProjectService.get({}, {'id': self.projectId}).$promise.then(
                        function (success) {
                          var projectName = success.projectName;
                self.ui = "/hopsworks-api/kibana/app/kibana?projectId=" + self.projectId + 
                        "#/discover?_g=()&_a=(columns:!(logdate,host,priority,logger_name,log_message),"+
                        "filters:!(('$state':(store:appState),meta:(alias:!n,disabled:!f,index:'" + projectName.toLowerCase() 
                        +"_logs-*',key:jobid,negate:!f,params:(query:notebook,type:phrase),type:phrase,value:notebook),"+
                        "query:(match:(jobid:(query:notebook,type:phrase)))),('$state':(store:appState),meta:(alias:!n,disabled:!f,index:'" + 
                        projectName.toLowerCase() +"_logs-*',key:jobname,negate:!f,params:(query:jupyter,type:phrase),"+
                        "type:phrase,value:jupyter),query:(match:(jobname:(query:jupyter,type:phrase))))),index:'" + projectName.toLowerCase() +
                        "_logs-*',interval:auto,query:(language:lucene,query:''),sort:!(logdate,desc))";

                          self.current = "kibanaUI";
                          var iframe = document.getElementById('ui_iframe');
                          if (iframe !== null) {
                            iframe.src = $sce.trustAsResourceUrl(self.ui);
                          }
                          $timeout(stopLoading(), 1000);
                        }, function (error) {

                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                  stopLoading();
                });
              } else {
                  //Get project name
                  //TODO(Theofilos): remove when we replace projectId with projectName
                  ProjectService.get({}, {'id': self.projectId}).$promise.then(
                      function (success) {
                          var projectName = success.projectName;
                          //if not jupyter we should have a job
                          self.ui = "/hopsworks-api/kibana/app/kibana?projectId=" + self.projectId + "#/discover?_g=()&_a=(columns:!(logdate,application,host,priority,logger_name,log_message),index:'"
                              + projectName.toLowerCase() + "_logs-*',interval:auto,query:(language:lucene,query:jobname=\"" + self.job.name + "\"),sort:!(logdate,desc))";
                          self.current = "kibanaUI";
                          var iframe = document.getElementById('ui_iframe');
                          if (iframe !== null) {
                              iframe.src = $sce.trustAsResourceUrl(encodeURI(self.ui));
                          }
                          $timeout(stopLoading(), 1000);
                      }, function (error) {

                          if (typeof error.data.usrMsg !== 'undefined') {
                              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                          } else {
                              growl.error("", {title: error.data.errorMsg, ttl: 8000});
                          }
                          stopLoading();
                      });

              }
            };
            self.grafanaUI = function () {
              startLoading("Loading Grafana UI...");
              getAppId(grafanaUIInt);
            };
            var grafanaUIInt = function () {
              JobService.getAppInfo(self.projectId, self.appId).then(
                      function (success) {
                        var info = success.data;
                        var appid = info.appId;
                        var startTime = info.startTime;
                        var finishTime = info.endTime;
                        //nbExecutors=;
                        if (info.now) {
                          self.ui = "/hopsworks-api/grafana/dashboard/script/spark.js?app="
                                  + appid + "&maxExecutorId="
                                  + info.nbExecutors + "&from="
                                  + startTime;
                        } else {
                          self.ui = "/hopsworks-api/grafana/dashboard/script/spark.js?app="
                                  + appid + "&maxExecutorId="
                                  + info.nbExecutors + "&from="
                                  + startTime + "&to="
                                  + finishTime;
                        }
                        self.current = "grafanaUI";
                        var iframe = document.getElementById('ui_iframe');
                        if (iframe !== null) {
                          iframe.src = $sce.trustAsResourceUrl(self.ui);
                        }
                        $timeout(stopLoading(), 1000);
                      }, function (error) {

                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
                stopLoading();
              });
            };
            self.tfUI = function (tfSession) {
              startLoading("Loading Tensorboard...");
              getAppId(tensorboardDummy);
              tensorboardInt(tfSession);
            };
            var tensorboardDummy = function () {
            };
            var tensorboardInt = function (tfSession) {
              self.ui = "/hopsworks-api/tensorboard/" + self.appId + "/" + tfSession.url + "/";
              self.current = "tensorboardUI";
              self.session = tfSession;
              var iframe = document.getElementById('ui_iframe');
              if (iframe === null) {
                stopLoading();
              } else {
                iframe.onload = function () {
                  stopLoading();
                };
              }
              if (iframe !== null) {
                iframe.src = $sce.trustAsResourceUrl(self.ui);
              }


            };

            getJobUI();

            self.openUiInNewWindow = function () {
              $window.open(self.ui, '_blank');
            };
            
            self.backToHome = function () {
              if (self.jobName !== undefined && self.jobName !== null && self.jobName !== "") {
                StorageService.store(self.projectId + "_jobui_" + self.jobName, self.job);
              }
              $timeout($route.reload(), 1000);
            };
            self.refresh = function () {
              var ifram = document.getElementById('ui_iframe');
              if (self.current === "grafanaUI") {
                self.grafanaUI();
              } else if (self.current === "jobUI") {
                self.jobUI();
              } else if (self.current === "yarnUI") {
                self.yarnUI();
              } else if (self.current === "kibanaUI") {
                self.kibanaUI();
              } else if (self.current === "tensorboardUI") {
                self.tfUI(self.session);
              } else if (ifram !== null) {
                ifram.contentWindow.location.reload();
              }
              getTensorBoardUrls();
            };

            var getTensorBoardUrls = function () {
              if (self.appId) {
              JobService.getTensorBoardUrls(self.projectId, self.appId).then(
                      function (success) {
                        self.tbUrls = success.data;
                      }, function (error) {
              });
              }
            };

            if(self.isLivy) {
              getTensorBoardUrls();
            }

            angular.module('hopsWorksApp').directive('bindHtmlUnsafe', function ($parse, $compile) {
              return function ($scope, $element, $attrs) {
                var compile = function (newHTML) {
                  newHTML = $compile(newHTML)($scope);
                  $element.html('').append(newHTML);
                };
                var htmlName = $attrs.bindHtmlUnsafe;
                $scope.$watch(htmlName, function (newHTML) {
                  if (!newHTML)
                    return;
                  compile(newHTML);
                });
              };
            });

          }]);
