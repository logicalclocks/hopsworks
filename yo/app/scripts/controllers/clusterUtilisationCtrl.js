/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('ClusterUtilisationCtrl', ['$scope', '$routeParams','$interval', 'growl', 'ClusterUtilService',
          function ($scope, $routeParams, $interval, growl, ClusterUtilService) {
            var self = this;
            self.pId = $routeParams.projectID;
            self.utilisation = 0.0;
            self.progressBarClass = 'progress-bar-success';
            
            var getClusterUtilisation = function () {
              ClusterUtilService.getYarnmultiplicator().then(
                      function (success) {
                        self.utilisation = parseFloat(success.data.multiplicator - 0.8).toFixed(4)*100;
                        if (self.utilisation <= 60) {
                          self.progressBarClass = 'progress-bar-success';
                        } else if (self.utilisation < 80) {
                          self.progressBarClass = 'progress-bar-warning';
                        } else {
                          self.progressBarClass = 'progress-bar-danger';
                        }
                      }, function (error) {
                        self.utilisation = 0.0;
                        self.progressBarClass = 'progress-bar-info';
              });
            };
            var getClusterUtilisationInterval = $interval(function () {
              getClusterUtilisation();
            }, 10000);
            getClusterUtilisation();
            $scope.$on("$destroy", function () {
              $interval.cancel(getClusterUtilisationInterval);
            });
          }]);


