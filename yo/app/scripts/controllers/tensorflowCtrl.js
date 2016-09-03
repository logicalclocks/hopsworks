/**
 * Controller for the kafka page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('TensorflowCtrl', ['$scope', '$routeParams', 'growl', 'TensorflowService', '$location', 'ModalService', '$interval', '$mdSidenav',
          function ($scope, $routeParams, growl, TensorflowService, $location, ModalService, $interval, $mdSidenav) {
              

            var self = this;
            self.projectId = $routeParams.projectID;
            self.topics = [];
           
            
            self.topicDetails = {};
                    
            self.maxNumPrograms = 10;
            self.numProgramsUsed = 0;

            self.currentProgram = "";
            self.topicName = "";
            self.numReplicas = "";
            self.numPartitions = "";
            self.projectName = "";
            self.userEmail = "";
            self.permission_type = "Allow";
            self.operation_type = "Read";
            self.host = "*";
            self.role = "*";
           // self.activeId = -1;
            self.selectedProjectName="";
            
            self.users =[];
            self.project;
           
            self.showPrograms = 1;
            self.showLogs = -1;
            self.logs = [];
            
            self.getAllPrograms = function () {
              TensorflowService.getPrograms(self.projectId).then(
                      function (success) {
                        self.topics = success.data;
                        self.numProgramsUsed = self.topics.length;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
              });
            };

            self.getProgramDetails = function (topicName) {
                TensorflowService.getProgramDetails(self.projectId, topicName).then(
                        function (success) {
                            for(var i =0;i<self.topics.length;i++){
                              if(self.topics[i].name === topicName){
                                  self.topics[i].partitionDetails= success.data;
                                  return;
                              }
                          }
                        }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
               });
            };
            
           
            self.listLogs = function () {
                
                TensorflowService.getLogsForPrograms(self.projectId, self.clusterId).then(
                 function (success) {
                 self.logs = success.data;
                 var size = self.logs.length;
//                for(var i =0; i<size;i++){
//                    self.logVersions[i] = Math.max.apply(null, self.logs[i].versions);
//                }
                 }, function (error) {
                 growl.error(error.data.errorMsg, {title: 'Could not get logs for cluster', ttl: 5000, referenceId: 21});
                 });
            
                
            };
            
            self.deleteLogs = function(cluster, index){
                
                if(!self.logs[index]>0){
                  growl.info("Delete aborted", {title: 'Log version not selected', ttl: 2000});  
                    return;
                }
                 ModalService.confirm("sm", "Delete Logs (" + cluster + ")",
                      "Do you really want to delete this Log File? This action cannot be undone.")
                      .then(function (success) {
                          TensorflowService.deleteLogs(self.projectId, logName, self.logVersions[index]).then(
                 function (success) {
                     self.listLogs();
                 }, function (error) {
                 growl.error(error.data.errorMsg, {title: 'Log is not removed', ttl: 5000, referenceId: 21});
                 });
                }, function (cancelled) {
                    growl.info("Delete aborted", {title: 'Info', ttl: 2000});
                    });
            };
            
            self.viewLogContent = function(logName, index){
                
                if(!self.logVersions[index]>0){
                     growl.info("Please select log version", {title: 'Log version not selected', ttl: 2000});
                return;
                }
                
               ModalService.viewLogContent('lg', self.projectId, logName, self.logVersions[index]).then(
                      function (success) {
                         
                      }, function (error) {
                //The user changed their mind.
              });
            };
            
            self.updateLogContent = function(log){
                
                //increment the version number
                self.version = Math.max.apply(null,log.versions);
                
                 ModalService.updateLogContent('lg', self.projectId, log.name, self.version).then(
                      function (success) {
                         self.listLogs();
                      }, function (error) {
                //The user changed their mind.
              });
            };
            
            /**
             * Navigate to the new job page.
             * @returns {undefined}
             */
            self.createProgram = function () {

              if(self.topics.length >10){
                  growl.info("Program Creation aborted", {title: 'Program limit reached', ttl: 2000});
                  return;
              }
              ModalService.createProgram('lg', self.projectId).then(
                      function (success) {
                          growl.success(success.data.successMessage, {title: 'New topic created successfully project.', ttl: 2000});
                          self.getAllPrograms();
                      }, function (error) {
                //The user changed their mind.
              });
              self.getAllPrograms();

            };

            self.removeProgram = function (topicName) {
              ModalService.confirm("sm", "Delete Program (" + topicName + ")",
                      "Do you really want to delete this topic? This action cannot be undone.")
                      .then(function (success) {
                        TensorflowService.removeProgram(self.projectId, topicName).then(
                                function (success) {
                                  self.getAllPrograms();
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Failed to remove topic', ttl: 5000});
                        });
                      }, function (cancelled) {
                        growl.info("Delete aborted", {title: 'Info', ttl: 2000});
                      });
            };

            
            self.init = function(){
                self.getAllPrograms();
                self.getAllSharedPrograms();              
             };
            
            self.init();

            self.showProgram = function(){
              self.showLogs = -1;
              self.showPrograms = 1;
            };
            
            self.showLog = function(){
              self.showLogs = 1;
              self.showPrograms = -1;
              self.listLogs();
            };
              
          }]);



