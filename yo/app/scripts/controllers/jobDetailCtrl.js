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
            
           this.updateNumberOfScheduleUnits = function () {
              self.schedule.addition = self.schedule.number == 1 ? "" : "s";
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
                          growl.success(success.data.successMessage, {title: 'Success', ttl: 3000});
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                });             
              } else {
                growl.info("Select a date", {title: 'Required', ttl: 3000});
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

          }]);
