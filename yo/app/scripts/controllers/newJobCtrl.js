/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('NewJobCtrl', ['$routeParams', 'growl', 'JobService', '$location',
          function ($routeParams, growl, JobService, $location) {

            var self = this;
            this.projectId = $routeParams.projectID;
            this.jobtype; //Will hold the selection of which job to create.
            this.jobname; //Will hold the name of the job
            this.phase = 0; //The phase of creation we are in.

            this.accordion1 = {"isOpen": false, "visible": false, "value": "", "title": "Choose the job type"};
            this.accordion2 = {"isOpen": false, "visible": false, "value": "", "title": ""};
            this.accordion3 = {"isOpen": false, "visible": false, "value": "", "title": ""};
            this.accordion4 = {"isOpen": false, "visible": false, "value": "", "title": "Configure"};
            this.accordion5 = {"isOpen": false, "visible": false, "value": "", "title": "Set a schedule (optional)"};

            this.createJob = function (type, config) {
              config.appName = self.jobname;
              JobService.createNewJob(self.projectId, type, config).then(
                      function (success) {
                        $location.path('project/' + self.projectId + '/jobs');
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            this.nameFilledIn = function () {
              if (self.phase == 0) {
                self.phase = 1;
                self.accordion1.isOpen = true;
                self.accordion1.visible = true;
              }
            };

            this.jobTypeChosen = function () {
              self.phase = 2;
              self.accordion2.isOpen = true;
              var type;
              switch (self.jobtype) {
                case "0":
                  self.accordion2.title = "Select a workflow file";
                  self.accordion3.title = "Bind input variables";
                  type = "Cuneiform";
                  break;
                case "1":
                  self.accordion2.title = "Select a JAR file";
                  self.accordion3.title = "Fill in job details";
                  type = "Spark";
                  break;
                case "2":
                  self.accordion2.title = "Select an ADAM command";
                  self.accordion3.title = "Set job arguments";
                  type = "ADAM";
                  break;
              }
              self.accordion2.visible = true;
              self.accordion1.value = " - " + type;
              self.accordion1.isOpen = false;
              self.accordion3.isOpen = false;
              self.accordion3.visible = false;
              self.accordion4.visible = false;
              self.acoordion5.visible = false;
              self.accordion2.value = "";
            };

            this.mainFileSelected = function (path) {
              self.phase = 3;
              self.accordion3.isOpen = true;
              self.accordion3.visible = true;
              self.accordion4.visible = true;
              self.accordion2.value = " - " + path;
              self.accordion2.isOpen = false;
              self.accordion5.visible = true;
            };

            this.jobDetailsFilledIn = function () {
              self.phase = 4;
            }

            // Methods for schedule updating
            this.schedule = {
              "unit": "hour", 
              "number": 1, 
              "addition": "", 
              "startDate": "",
              "startTime":"",
              "minStart":new Date()
            };
            this.updateNumberOfScheduleUnits = function () {
              self.schedule.addition = self.schedule.number == 1 ? "" : "s";
            };  
            self.datePickerOpen = false;
            this.openDatePicker = function($event){
              $event.preventDefault();
              $event.stopPropagation();
              self.datePickerOpen = true;
            };


          }]);


