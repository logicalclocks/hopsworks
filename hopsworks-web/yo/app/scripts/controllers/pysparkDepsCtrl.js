/**
 * Controller for the kafka page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('PysparkDepsCtrl', ['$scope', '$routeParams', 'growl', 'ProjectService', '$location', 'ModalService', '$interval', '$mdSidenav',
          function ($scope, $routeParams, growl, ProjectService, $location, ModalService, $interval, $mdSidenav) {
              

            var self = this;
            self.projectId = $routeParams.projectID;
            self.closeAll = false;
            
            self.deps = [];
           
            self.currentCluster = "";
            self.clusterName = "";
            self.projectName = "";
            self.userEmail = "";
            self.project;
           
            self.logs = [];
            
            self.init = function(){

             };
            
            self.init();
              
          }]);



