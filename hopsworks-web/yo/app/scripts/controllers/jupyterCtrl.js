'use strict';

angular.module('hopsWorksApp')
        .controller('JupyterCtrl', ['$scope', '$routeParams', '$route',
          'growl', 'ModalService', 'JupyterService', '$location', '$timeout', '$window', '$sce',
          function ($scope, $routeParams, $route, growl, ModalService, JupyterService, $location, $timeout, $window, $sce) {

            var self = this;
            self.loading = false;
            self.loadingText = "";
            $scope.tgState = true;
            self.jupyterServer;
            self.toggleValue;
            var projectId = $routeParams.projectID;
            var statusMsgs = ['stopped    ', "running    ", 'stopping...', 'restarting...'];
            var loaded = false;
            self.ui = "";

            self.token;
            self.port;

//            var toggleJupyter = function () {
//              if (self.toggleValue === false) {
//                JupyterService.start(projectId).then(
//                        function (success) {
//                          growl.info("Jupyter Started successfully");
//                          self.toggleValue = true;
//                        }, function (error) {
//                  growl.error("Could not start Jupyter.");
//                }
//                );
//              } else {
//                JupyterService.stop(projectId).then(
//                        function (success) {
//                          growl.info("Jupyter Stopped successfully");
//                          self.toggleValue = false;
//                        }, function (error) {
//                  growl.error("Could not stop Jupyter.");
//                }
//                );
//              }
//            };

            var init = function () {
              startLoading("Connecting to Jupyter...");
              $scope.tgState = true;
              JupyterService.start(projectId).then(
                      function (success) {
                        self.toggleValue = true;
                        stopLoading();
                        self.token = success.data.hashedPasswd;
                        self.port = success.data.port;
//                                self.ui = "http://" + $location.host() + ":" + self.port + "/?token=" + self.token;
                        self.ui = "http://192.168.56.101:" + self.port + "/?token=" + self.token;
                        var iframe = document.getElementById('ui_iframe');
                        if (iframe) {
                          iframe.src = $sce.trustAsResourceUrl(self.ui);
                        }
                        $window.open(self.ui, '_blank');
                        $timeout(stopLoading(), 1000);

                      }, function (error) {
                growl.error("Could not start Jupyter.");
                stopLoading();
              }
              );
            };


            var startLoading = function (label) {
              self.loading = true;
              self.loadingText = label;
            };
            var stopLoading = function () {
              self.loading = false;
              self.loadingText = "";
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
