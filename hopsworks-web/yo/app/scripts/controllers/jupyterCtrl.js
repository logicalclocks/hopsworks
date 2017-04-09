'use strict';

angular.module('hopsWorksApp')
        .controller('JupyterCtrl', ['$scope', '$routeParams','$route',
          'growl', 'ModalService', 'JupyterService','$location',
          function ($scope, $routeParams, $route, growl, ModalService, JupyterService, $location) {

            var self = this;
            self.loading = false;
            self.loadingText = "";
            $scope.tgState = true;
            self.selectedInterpreter;
            
            var projectId = $routeParams.projectID;
            var statusMsgs = ['stopped    ', "running    ", 'stopping...', 'restarting...'];
            var loaded = false;



            var init = function () {
              startLoading("Connecting to zeppelin...");
              $scope.tgState = true;
            };
            
            var load = function () {
              $scope.tgState = true;
            };
            
            init();


            $scope.$on("$destroy", function () {
//              JupyterService.wsDestroy();
              loaded = false;
            });
            
            //refresh interpreter status when we return to zeppelin dashbord. 
            window.onfocus = function () {
              if (loaded) {
                refresh();                
              }
            };
          }]);
