/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('ClusterUtilisationCtrl', ['$scope', '$routeParams','$interval', 'ClusterUtilService',
          function ($scope, $routeParams, $interval, ClusterUtilService) {
            var self = this;
            self.pId = $routeParams.projectID;
            self.utilisation = 0.0;
            self.availableGPUs = 0;
            self.allocatedGPUs = 0;
            self.reservedGPUs = 0;
            self.gpusPercent = 0;
            self.progressBarClass = 'progress-bar-success';
            self.gpuBarClass = 'progress-bar-success';
            self.HdfsStatus = 'text-success';
            self.YarnStatus = 'text-success';
            self.KafkaStatus = 'text-success';
            
            var getClusterUtilisation = function () {
              ClusterUtilService.getYarnmultiplicator().then(
                      function (success) {
                        var usage = parseFloat(success.data.multiplicator - 0.8).toFixed(4)*100;
                        self.utilisation = Math.min(usage, 100);
                        if (self.utilisation <= 50) {
                          self.progressBarClass = 'progress-bar-success';
                        } else if (self.utilisation <= 80) {
                          self.progressBarClass = 'progress-bar-warning';
                        } else {
                          self.progressBarClass = 'progress-bar-danger';
                        }
                      }, function (error) {
                        self.utilisation = 0.0;
                        self.progressBarClass = 'progress-bar-info';
              });
            };
            var getGpuUtilization = function () {
              ClusterUtilService.getYarnGpu().then(
                      function (success) {
                        self.allocatedGPUs = success.data.clusterMetrics.allocatedGPUs;
                        self.reservedGPUs = success.data.clusterMetrics.reservedGPUs;
                        self.availableGPUs = success.data.clusterMetrics.availableGPUs;
                        var totalGpus = self.availableGPUs + self.allocatedGPUs;
                        if (self.availableGPUs > 0 && self.reservedGPUs == 0) {
                          self.gpuBarClass = 'progress-bar-success';
                        } else if (self.availableGPUs > 0 ) {
                          self.gpuBarClass = 'progress-bar-warning';
                        } else {
                          self.gpuBarClass = 'progress-bar-danger';
                        }
                        self.gpusPercent = (self.allocatedGPUs/totalGpus);

                      }, function (error) {
                        console.log("Problem getting GPU utilization");
              });
              
            };
            
            var getHdfsStatus = function () {
              ClusterUtilService.getHdfsStatus().then(
                      function (success) {
                        var nbDataNodes = 0;
                        var nbRunningDataNodes = 0;
                        var nbNameNodes = 0;
                        var nbRunningNameNodes = 0;
                        success.data.forEach(function (status) {
                          if (status.role === "datanode") {
                            nbDataNodes++;
                            if (status.status === "Started") {
                              nbRunningDataNodes++;
                            }
                          }
                          if (status.role === "namenode") {
                            nbNameNodes++;
                            if (status.status === "Started") {
                              nbRunningNameNodes++;
                            }
                          }
                        });
                        if (nbRunningDataNodes > nbDataNodes * 2 / 3 && nbRunningNameNodes > nbNameNodes / 2) {
                          self.HdfsStatus = 'status-success';
                        } else if (nbRunningDataNodes > nbDataNodes * 1 / 3 && nbRunningNameNodes >= 1) {
                          self.HdfsStatus = 'status-warning';
                        } else {
                          self.HdfsStatus = 'status-danger';
                        }

                      }, function (error) {
                console.log("problem getting HDFS status");
              });
            };

            var getYarnStatus = function () {
              ClusterUtilService.getYarnStatus().then(
                      function (success) {
                        var nbNodeManagers = 0;
                        var nbRunningNodeManagers = 0;
                        var nbRMs = 0;
                        var nbRunningRMs = 0;
                        success.data.forEach(function (status) {
                          if (status.role === "nodemanager") {
                            nbNodeManagers++;
                            if (status.status === "Started") {
                              nbRunningNodeManagers++;
                            }
                          }
                          if (status.role === "resourcemanager") {
                            nbRMs++;
                            if (status.status === "Started") {
                              nbRunningRMs++;
                            }
                          }
                        });
                        if (nbRunningNodeManagers > nbNodeManagers * 2 / 3 && nbRunningRMs > nbRMs / 2) {
                          self.YarnStatus = 'status-success';
                        } else if (nbRunningNodeManagers > nbNodeManagers * 1 / 3 && nbRunningRMs >= 1) {
                          self.YarnStatus = 'status-warning';
                        } else {
                          self.YarnStatus = 'status-danger';
                        }

                      }, function (error) {
                console.log("problem getting HDFS status");
              });
            };

            var getKafkaStatus = function () {
              ClusterUtilService.getKafkaStatus().then(
                      function (success) {
                        var nbInstances = 0;
                        var nbRunningInstances = 0;
                        success.data.forEach(function (status) {
                          nbInstances++;
                          if (status.status === "Started") {
                            nbRunningInstances++;
                          }
                        });
                        if (nbRunningInstances > nbInstances * 2 / 3) {
                          self.KafkaStatus = 'status-success';
                        } else if (nbRunningInstances > nbInstances * 1 / 3) {
                          self.KafkaStatus = 'status-warning';
                        } else {
                          self.KafkaStatus = 'status-danger';
                        }

                      }, function (error) {
                console.log("problem getting HDFS status");
              });
            };
            
            var getClusterUtilisationInterval = $interval(function () {
              getClusterUtilisation();
            }, 10000);
            getClusterUtilisation();
            getGpuUtilization();
            getHdfsStatus();
            getYarnStatus();
            getKafkaStatus();
            $scope.$on("$destroy", function () {
              $interval.cancel(getClusterUtilisationInterval);
            });
          }]);


