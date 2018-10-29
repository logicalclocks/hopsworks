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
 * Controller for the job detail dialog. 
 */
angular.module('hopsWorksApp')
        .controller('JobDetailCtrl', ['$scope', '$uibModalInstance', 'growl', 'JobService', 'job', 'projectId', '$interval', 'StorageService', '$routeParams', '$location',
          function ($scope, $uibModalInstance, growl, JobService, job, projectId, $interval, StorageService, $routeParams, $location) {

            var self = this;
            this.job = job;
            this.jobtype; //Holds the type of job.
            this.execFile; //Holds the name of the main execution file
            this.showExecutions = false;
            this.projectId = $routeParams.projectID;

            self.unscheduling=false;
            
            this.availableschedule = {
              "start": "-1",
              "number": 1,
              "unit": ""
            };

            this.schedule = {
              "unit": "hour",
              "number": 1,
              "addition": "",
              "startDate": ""
            };
            self.hasJobScheduled=false;
            
            var initScheduler = function() {
                if(!$.isEmptyObject(self.job.runConfig.schedule)){
                    self.hasJobScheduled=true;
                    var d=new Date(self.job.runConfig.schedule.start);
                    self.availableschedule.start=d.getFullYear()+"-"+d.getMonth()+'-'+d.getDate()+' '+d.getHours()+':'+d.getMinutes()+':'+d.getSeconds();
                    self.availableschedule.number=self.job.runConfig.schedule.number;  
                    self.availableschedule.unit=self.job.runConfig.schedule.unit.toLowerCase()+(self.job.runConfig.schedule.number > 1?'s':'');
                }else{
                    self.hasJobScheduled=false;
                }
            };

            var getConfiguration = function () {
              JobService.getConfiguration(projectId, job.id).then(
                      function (success) {
                        self.job.runConfig = success.data;
                        self.setupInfo();
                        initScheduler();
                      }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
              });
            };

            var getExecutions = function () {
              JobService.getAllExecutions(projectId, job.id).then(
                      function (success) {
                        self.job.executions = success.data;
                        self.showExecutions = success.data.length > 0;
                      }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
              });
            };
            
           this.updateNumberOfScheduleUnits = function () {
              self.schedule.addition = self.schedule.number == 1 ? "" : "s";
            };

            this.setupInfo = function () {

              if (self.job.runConfig.type === "sparkJobConfiguration") {
                self.jobtype = "Spark";
                self.execFile = getFileName(job.runConfig.appPath);
              } else if (self.job.runConfig.type === "flinkJobConfiguration") {
                self.jobtype = "Flink";
                self.execFile = getFileName(job.runConfig.jarPath);
              }
            };
            
            this.updateSchedule = function() {
                if ($('#schedulePicker').data("DateTimePicker").date()) {
                self.job.runConfig.schedule = {
                  "start": $('#schedulePicker').data("DateTimePicker").date().valueOf(),
                  "unit": self.schedule.unit.toUpperCase(),
                  "number": self.schedule.number};
                self.job.runConfig.type=self.jobtype.toUpperCase();
                JobService.updateSchedule(self.projectId, self.jobtype.toUpperCase(), self.job.runConfig.schedule,job.id).then(
                        function (success) {
                          getConfiguration();
                          getExecutions();
                          self.close()
                          growl.success(success.data.successMessage, {title: 'Success', ttl: 3000});
                        }, function (error) {
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                });             
              } else {
                growl.info("Select a date", {title: 'Required', ttl: 3000});
              }
            };

            this.unscheduleJob = function(jobId) {
            self.unscheduling=true;
                        JobService.unscheduleJob(self.projectId, jobId).then(
                                function (success) {
                                  self.unscheduling=false;
                                  getExecutions();
                                  self.close()
                                  growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                                }, function (error) {
                                  self.unscheduling=false;
                                if (typeof error.data.usrMsg !== 'undefined') {
                                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                                } else {
                                    growl.error("", {title: error.data.errorMsg, ttl: 8000});
                                }
                        });
            };

            getConfiguration();
            getExecutions();            

            /**
             * Close the modal dialog.
             * @returns {undefined}
             */
            self.close = function () {
              $uibModalInstance.dismiss('cancel');
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

            /**
             * Converts the colon-separated list of topics to a nicer human friendly format
             * @returns comma-separated list of topics
             */
            self.viewTopics = function(topics){
              var view = "";
              for(var i=0; i<topics.length;i++){
                view+=topics[i]['name']+", ";
              }
              if(view.length>1){
                view = view.substr(0, view.length-2);
              }
              return view;
            };
            
            self.hasKafka = function(){
              if(typeof self.job.runConfig !== "undefined" && typeof self.job.runConfig.kafka !== "undefined"){
                return true;
              }
              return false;
            };
            self.hasConsumerGroups = function(){
              if(typeof self.job.runConfig.kafka.consumergroups !== "undefined"){
                return true;
              }
              return false;
            };

          }]);
