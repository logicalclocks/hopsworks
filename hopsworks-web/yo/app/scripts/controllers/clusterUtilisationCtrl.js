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
            
            var getClusterUtilisationInterval = $interval(function () {
              getClusterUtilisation();
            }, 10000);
            getClusterUtilisation();
            getGpuUtilization();
            $scope.$on("$destroy", function () {
              $interval.cancel(getClusterUtilisationInterval);
            });
          }]);


