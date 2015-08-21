/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('NewJobCtrl', ['$routeParams', 'growl', 'JobService', '$location','ModalService',
          function ($routeParams, growl, JobService, $location, ModalService) {

            var self = this;
            this.projectId = $routeParams.projectID;
            this.ModalService = ModalService;
            this.selectFileRegex = /.jar\b/;
            this.selectFileErrorMsg = "Please select a JAR file.";
            this.jobtype; //Will hold the selection of which job to create.
            this.jobname; //Will hold the name of the job
            this.localResources = {"entry":[]}; //Will hold extra libraries
            this.phase = 0; //The phase of creation we are in.

            this.accordion1 = {"isOpen": true, "visible": true, "value": "", "title": "Job name"};
            this.accordion2 = {"isOpen": false, "visible": false, "value": "", "title": "Job type"};
            this.accordion3 = {"isOpen": false, "visible": false, "value": "", "title": ""};
            this.accordion4 = {"isOpen": false, "visible": false, "value": "", "title": ""};
            this.accordion5 = {"isOpen": false, "visible": false, "value": "", "title": "Configure and create"};


            this.createJob = function (type, config) {
              config.appName = self.jobname;
              config.localResources = localResources;
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
                self.accordion2.isOpen = true;
                self.accordion2.visible = true;
              }
            };

            this.jobTypeChosen = function () {
              self.phase = 2;
              self.accordion3.isOpen = true;
              var type;
              switch (self.jobtype) {
                case 0:
                  self.accordion3.title = "Workflow file";
                  self.accordion4.title = "Input variables";
                  type = "Cuneiform";
                  break;
                case 1:
                  self.accordion3.title = "JAR file";
                  self.accordion4.title = "Job details";
                  type = "Spark";
                  break;
                case 2:
                  self.accordion3.title = "ADAM command";
                  self.accordion4.title = "Job arguments";
                  type = "ADAM";
                  break;
              }
              self.accordion1.isOpen = false;
              self.accordion1.value = " - " + self.jobname;
              self.accordion3.visible = true;
              self.accordion2.value = " - " + type;
              self.accordion2.isOpen = false;
              self.accordion4.isOpen = false;
              self.accordion4.visible = false;
              self.accordion5.visible = false;
              self.accordion3.value = "";
            };

            this.mainFileSelected = function (path) {
              self.phase = 3;
              self.accordion4.isOpen = true;
              self.accordion4.visible = true;
              self.accordion5.visible = true;
              self.accordion3.value = " - " + path;
              self.accordion3.isOpen = false;
            };

            this.jobDetailsFilledIn = function () {
              self.phase = 4;
            };
            
            this.selectFile = function(){
              selectFile(this);
            };
            
            this.onFileSelected = function(path) {
              var filename = getFileName(path);
              localResources.entry.push({"key":filename,"value":path});
            }; 
          }]);


