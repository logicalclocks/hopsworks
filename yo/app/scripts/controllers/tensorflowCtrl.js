/**
 * Controller for the kafka page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('TensorflowCtrl', ['$scope', '$routeParams', 'growl', 'TensorflowService', '$location', 'ModalService', '$interval', '$mdSidenav',
          function ($scope, $routeParams, growl, TensorflowService, $location, ModalService, $interval, $mdSidenav) {
              

            var self = this;
            self.projectId = $routeParams.projectID;
            self.clusters = [];
            self.serving = [];
           
            
            self.clusterDetails = {};
            self.jobDetails = {};
            self.taskDetails = {};
            self.servingClusterDetails = {};
            self.servingJobDetails = {};
            self.servingTaskDetails = {};
                    
            self.maxNumCpus = 10;
            self.maxNumGpus = 10;
            self.numCpusUsed = 0;
            self.numGpusUsed = 0;

            self.currentCluster = "";
            self.currentJob = "";
            self.currentTask = "";
            self.clusterName = "";
            self.projectName = "";
            self.userEmail = "";
            self.project;
           
            self.showClusters = 1;
            self.showServing = -1;
            self.logs = [];
            
            self.getAllClusters = function () {
              TensorflowService.getClusters(self.projectId).then(
                      function (success) {
                        self.clusters = success.data;
                        self.numCpusUsed = self.clusters.length;
                        self.numGpusUsed = self.clusters.length;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
              });
            };

            self.getClusterDetails = function (clusterName) {
                TensorflowService.getClusterDetails(self.projectId, clusterName).then(
                        function (success) {
                            for(var i =0;i<self.clusters.length;i++){
                              if(self.clusters[i].name === clusterName){
                                  self.clusters[i].partitionDetails= success.data;
                                  return;
                              }
                          }
                        }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
               });
            };
            
           
            self.getAllServing = function () {
                
                TensorflowService.getServing(self.projectId).then(
                 function (success) {
                 self.serving = success.data;
                 var size = self.logs.length;
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
                     self.listServing();
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
                         self.listServing();
                      }, function (error) {
                //The user changed their mind.
              });
            };
            
            /**
             * Navigate to the new job page.
             * @returns {undefined}
             */
            self.createCluster = function () {

              if(self.clusters.length >10){
                  growl.info("Cluster Creation aborted", {title: 'Cluster limit reached', ttl: 2000});
                  return;
              }
              ModalService.createCluster('lg', self.projectId).then(
                      function (success) {
                          growl.success(success.data.successMessage, {title: 'New topic created successfully project.', ttl: 2000});
                          self.getAllClusters();
                      }, function (error) {
                //The user changed their mind.
              });
              self.getAllClusters();

            };

            self.removeCluster = function (clusterName) {
              ModalService.confirm("sm", "Delete Cluster (" + clusterName + ")",
                      "Do you really want to delete this topic? This action cannot be undone.")
                      .then(function (success) {
                        TensorflowService.removeCluster(self.projectId, clusterName).then(
                                function (success) {
                                  self.getAllClusters();
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Failed to remove topic', ttl: 5000});
                        });
                      }, function (cancelled) {
                        growl.info("Delete aborted", {title: 'Info', ttl: 2000});
                      });
            };

            
            self.init = function(){
                self.getAllClusters();
                self.getAllSharedClusters();              
             };
            
            self.init();

            self.showClusters = function(){
              self.showServing = -1;
              self.showClusters = 1;
            };
            
            self.showServing = function(){
              self.showServing = 1;
              self.showClusters = -1;
              self.listServing();
            };
              
          }]);



