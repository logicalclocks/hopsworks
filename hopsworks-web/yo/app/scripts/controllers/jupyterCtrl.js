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
            self.toggleValue = false;
            var projectId = $routeParams.projectID;
            var statusMsgs = ['stopped    ', "running    ", 'stopping...', 'restarting...'];
            self.ui = "";

            self.config = {};


            $window.uploadDone = function () {
              stopLoading();
            }

            $scope.trustSrc = function (src) {
              return $sce.trustAsResourceUrl(self.ui);
            };

            self.restart = function () {
              $location.path('/#!/project/' + self.projectId + '/jupyter');
            }



            var init = function () {
              JupyterService.running(projectId).then(
                      function (success) {
                        self.config = success.data;
                        self.ui = "/hopsworks-api/jupyter/" + self.config.port + "/?token=" + self.config.token;
                        $window.open(self.ui, '_blank');			  
                        self.toggleValue = true;
                      }, function (error) {
                configure();
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

            self.goBack = function () {
              $window.history.back();
            };

            self.stop = function () {
              startLoading("Stopping Jupyter...");

              JupyterService.stop(projectId).then(
                      function (success) {
                        self.ui = "";
                        stopLoading();
                      }, function (error) {
                growl.error("Could not stop the Jupyter Notebook Server.");
                stopLoading();
              }
              );



            };

            self.stopDataOwner = function (hdfsUsername) {
              startLoading("Stopping Jupyter...");
              JupyterService.stopDataOwner(projectId, hdfsUsername).then(
                      function (success) {
                        self.ui = ""
                        stopLoading();
                      }, function (error) {
                growl.error("Could not stop the Jupyter Notebook Server.");
                stopLoading();
              }
              );
            };
            self.stopAdmin = function (hdfsUsername) {
              startLoading("Stopping Jupyter...");
              JupyterService.stopAdmin(projectId, hdfsUsername).then(
                      function (success) {
                        self.ui = ""
                        stopLoading();
                      }, function (error) {
                growl.error("Could not stop the Jupyter Notebook Server.");
                stopLoading();
              }
              );
            };

            var load = function () {
              $scope.tgState = true;
            };

            init();



            var start = function () {
              startLoading("Connecting to Jupyter...");
              $scope.tgState = true;

              JupyterService.start(projectId, self.config).then(
                      function (success) {
                        self.toggleValue = true;
                        self.config = success.data;

                        self.ui = "/hopsworks-api/jupyter/" + self.config.port + "/?token=" + self.config.token;
                        $window.open(self.ui, '_blank');
                        $timeout(stopLoading(), 5000);

                      }, function (error) {
                growl.error("Could not start Jupyter.");
                stopLoading();
                self.toggleValue = true;
              }
              );

            };


            var configure = function () {
              var val = {};
              val.driverMemory = "500M";
              val.executorMemory = "500M";
              val.gpus = 1;
              val.driverCores = 1;
              val.executorCores = 1;
              val.archives = "";
              val.jars = "";
              val.files = "";
              val.pyFiles = "";
              ModalService.jupyterConfig('md', '', '', val).then(
                      function (success) {
                        self.config = success.val;
                        start();
                      },
                      function (error) {
                        growl.error("Could not activate Jupyter.");
                        self.toggleValue = true;

                      });

            };


          }]);
