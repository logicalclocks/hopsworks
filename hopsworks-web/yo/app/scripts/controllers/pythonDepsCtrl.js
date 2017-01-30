/**
 * Controller for the kafka page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('PythonDepsCtrl', ['$scope', '$routeParams', 'growl', 'ProjectService', '$location', 'PythonDepsService', '$interval',
          function ($scope, $routeParams, growl, ProjectService, $location, PythonDepsService, $interval) {
              

            var self = this;
            self.projectId = $routeParams.projectID;
            
            self.channels = [
              {"https://repo.continuum.io/pkgs/free/linux-64/" : [{"pandas" : "installed"}]
              }
              ];
            
            self.installing = true;
            
            $scope.numInstalled = 10;
            $scope.numToInstall = 100;
           
            self.currentCluster = "";
            self.clusterName = "";
            self.projectName = "";
            self.userEmail = "";
            self.project;
            
            self.condaUrl = "https://repo.continuum.io/pkgs/free/linux-64/";
            self.library = "";
           
            self.logs = [];
            
            self.init = function(){

             };
            
            self.init();
              
          }]);



