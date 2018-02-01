/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Controller for the kafka page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('TensorflowCtrl', ['$scope', '$routeParams', 'growl', 'TensorFlowService', '$location', 'ModalService', '$interval', '$mdSidenav',
          function ($scope, $routeParams, growl, TensorFlowService, $location, ModalService, $interval, $mdSidenav) {
              

            var self = this;
            self.projectId = $routeParams.projectID;
            self.closeAll = false;
            
            self.resources = [];
            self.clusters = [
              { "name": "cluster1",
                "isExpanded" : true,
                "jobs" : [ 
                  { "name" : "params",
                    "program" : "/path/to/prog.py",
                    "tasks" : [
                      { "host" : "server1",
                        "params" : "10"
                      },
                      { "host" : "server2",
                        "params" : "-f 22"
                      }
                    ]
                  }
                  ]
              }
            ];
            
            self.serving = [];
           
            
            self.clusterDetails = {};
            self.jobDetails = {};
            self.taskDetails = {};
            self.servingClusterDetails = {};
            self.servingJobDetails = {};
            self.servingTaskDetails = {};
                    
            self.cpuQuota = 10;
            self.gpuQuota = 10;
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
            
            self.getResources = function () {
              TensorFlowService.getResources(self.projectId).then(
                      function (success) {
                        self.resources = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 2000});
              });
            };
            
            self.getClusters = function () {
              TensorFlowService.getClusters(self.projectId).then(
                      function (success) {
                        self.clusters = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 2000});
              });
            };

            self.getClusterDetails = function (clusterName) {
                TensorFlowService.getClusterDetails(self.projectId, clusterName).then(
                        function (success) {
                            for(var i =0;i<self.clusters.length;i++){
                              if(self.clusters[i].name === clusterName){
                                  self.clusters[i].partitionDetails= success.data;
                                  return;
                              }
                          }
                        }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 2000});
               });
            };
            
           
            self.getServing = function () {
                
                TensorFlowService.getServing(self.projectId).then(
                 function (success) {
                 self.serving = success.data;
                 var size = self.logs.length;
                 }, function (error) {
                 growl.error(error.data.errorMsg, {title: 'Could not get logs for cluster', ttl: 2000, referenceId: 21});
                 });
            
                
            };
            
            self.deleteTasksLogs = function(cluster, jobId, taskId, executionId){
                
                if(!self.logs[index]>0){
                  growl.info("Delete aborted", {title: 'Log version not selected', ttl: 2000});  
                    return;
                }
                 ModalService.confirm("sm", "Delete Logs (" + cluster + ")",
                      "Do you really want to delete this Log File? This action cannot be undone.")
                      .then(function (success) {
                          TensorFlowService.deleteLogs(self.projectId, logName, self.logVersions[index]).then(
                 function (success) {
                     self.listServing();
                 }, function (error) {
                 growl.error(error.data.errorMsg, {title: 'Log is not removed', ttl: 2000, referenceId: 21});
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
                          self.getClusters();
                      }, function (error) {
                //The user changed their mind.
              });
              self.getClusters();

            };

            self.removeCluster = function (clusterName) {
              ModalService.confirm("sm", "Delete Cluster (" + clusterName + ")",
                      "Do you really want to delete this topic? This action cannot be undone.")
                      .then(function (success) {
                        TensorFlowService.removeCluster(self.projectId, clusterName).then(
                                function (success) {
                                  self.getClusters();
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Failed to remove topic', ttl: 2000});
                        });
                      }, function (cancelled) {
                        growl.info("Delete aborted", {title: 'Info', ttl: 2000});
                      });
            };

            self.toggle = function (cluster, index) {
              //reset all jobs showing flag
              angular.forEach(self.clusters, function (temp, key) {
                if (job.id !== temp.id) {
                  temp.showing = false;
                }
              });

              //handle the clicked job accordingly
              cluster.showing = true;
              self.hasSelectCluster = true;
              $scope.selectedIndex = index;
              self.currentToggledIndex = index;
              self.currentCluster = cluster;
            };
            self.untoggle = function (cluster, index) {
              //reset all jobs showing flag
              angular.forEach(self.clusters, function (temp, key) {
                temp.showing = false;
              });

              if (self.currentToggledIndex !== index) {
                self.hasSelectCluster = false;
                $scope.selectedIndex = -1;
                self.currentToggledIndex = -1;
              } else {
                cluster.showing = true;
              }
            };



            
            self.init = function(){
                self.getClusters();
                self.getAllSharedClusters();              
             };
            
//            self.init();

          
            self.showTab = function(serving){
              if(serving === true){
                self.showServing = 1;
                self.showClusters = -1;
            } else {
                self.showServing = -1;
                self.showClusters = 1;
            }
//              self.listServing();
            };
              
          }]);



