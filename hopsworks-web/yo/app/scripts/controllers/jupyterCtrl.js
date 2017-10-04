'use strict';

angular.module('hopsWorksApp')
        .controller('JupyterCtrl', ['$scope', '$routeParams', '$route',
          'growl', 'ModalService', 'JupyterService', 'TensorFlowService', 'SparkService', 'StorageService', '$location', '$timeout', '$window', '$sce', 'PythonDepsService', 'TourService',
          function ($scope, $routeParams, $route, growl, ModalService, JupyterService, TensorFlowService, SparkService, StorageService,
                  $location, $timeout, $window, $sce, PythonDepsService, TourService) {

            var self = this;
            self.connectedStatus = false;
            self.loading = false;
            self.advanced = false;
            self.details = true;
            self.loadingText = "";
            self.jupyterServer;
            self.projectName;
            self.toggleValue = false;
            self.tourService = TourService;
            self.projectId = $routeParams.projectID;
            var statusMsgs = ['stopped    ', "running    ", 'stopping...', 'restarting...'];
            self.ui = "";
            self.sparkStatic = false;
            self.sparkDynamic = false;
            self.tensorflow = false;
            $scope.sessions = null;
            self.val = {};
            $scope.tgState = true;
            self.config = {};
            self.dirs = [
              {id: 1, name: '/'},
              {id: 2, name: '/Jupyter/'}
            ];
            self.selected = self.dirs[1];

            self.log_levels = [
              {id: 1, name: 'FINE'},
              {id: 2, name: 'DEBUG'},
              {id: 3, name: 'INFO'},
              {id: 4, name: 'WARN'},
              {id: 5, name: 'ERROR'}
            ];
            self.logLevelSelected;
            self.job = {'type': '',
                        'name': '',
                        'id': '',
                        'project': 
                                  { 'name': '',
                                    'id': self.projectId
                                  }
                      };




            self.changeLogLevel = function () {
              self.val.logLevel = self.logLevelSelected.name;
            };

            self.changeBaseDir = function () {
              self.val.baseDir = self.selected.name;
            };

            self.deselect = function () {
            };

            window.onfocus = function () {
              self.livySessions(self.projectId);
            };

            self.livySessions = function (projectId) {
              JupyterService.livySessions(projectId).then(
                      function (success) {
                        $scope.sessions = success.data;
                      }, function (error) {
                $scope.sessions = null;
              }
              );

            };

            self.showLivyUI = function (appId) {
              self.job.type = "TENSORFLOW";
              self.job.appId = appId;
              StorageService.store(self.projectId + "_jobui_TENSORFLOW", self.job);
              $location.path('project/' + self.projectId + '/jobMonitor-app/' + appId + "/true");

            };

            self.sliderVisible = false;

            self.sliderOptions = {
              min: 1,
              max: 10,
              options: {
                floor: 0,
                ceil: 1500
              },
              getPointerColor: function (value) {
                return '#4b91ea';
              }
            };

            self.refreshSlider = function () {
              $timeout(function () {
                $scope.$broadcast('rzSliderForceRender');
              });
            };

            self.toggleSlider = function () {
              self.sliderVisible = !self.sliderVisible;
              if (self.sliderVisible)
                self.refreshSlider();
            };

            self.setInitExecs = function () {
              if (self.sliderOptions.min >
                      self.val.dynamicInitialExecutors) {
                self.val.dynamicInitialExecutors =
                        parseInt(self.sliderOptions.min);
              } else if (self.sliderOptions.max <
                      self.val.dynamicInitialExecutors) {
                self.val.dynamicInitialExecutors =
                        parseInt(self.sliderOptions.max);
              }
              self.val.dynamicMinExecutors = self.sliderOptions.min;
              self.val.dynamicMaxExecutors = self.sliderOptions.max;
            };


            //Set some (semi-)constants
            self.selectFileRegexes = {
              "JAR": /.jar\b/,
              "PY": /.py\b/,
              "*": /[^]*/,
              "ZIP": /[^]*/
            };
            self.selectFileErrorMsgs = {
              "JAR": "Please select a JAR file.",
              "PY": "Please select a Python file.",
              "ZIP": "Please select a file.",
              "*": "Please select a folder."
            };


            this.selectFile = function (reason) {

              ModalService.selectFile('lg', self.selectFileRegexes[reason.toUpperCase()],
                      self.selectFileErrorMsgs[reason.toUpperCase()]).then(
                      function (success) {
                        self.onFileSelected(reason, "hdfs://" + success);
                      }, function (error) {
                //The user changed their mind.
              });
            };


            /**
             * Callback for when the user selected a file.
             * @param {String} reason
             * @param {String} path
             * @returns {undefined}
             */
            self.onFileSelected = function (reason, path) {
              var re = /(?:\.([^.]+))?$/;
              var ext = re.exec(path)[1];
//              switch (reason.toUpperCase()) {
              switch (ext.toUpperCase()) {
                case "JAR":
                  if (reason.toUpperCase() !== ".JAR") {
                    growl.error("Invalid file type selected. Expecting " + reason + " - Found: " + ext);
                  } else {
                    if (self.val.jars === "") {
                      self.val.jars = path;
                    } else {
                      self.val.jars = self.val.jars.concat(",").concat(path);
                    }
                  }
                  break;
                case "PY":
                  if (reason.toUpperCase() !== ".PY") {
                    growl.error("Invalid file type selected. Expecting " + reason + " - Found: " + ext);
                  } else {
                    if (self.val.py === "") {
                      self.val.py = path;
                    } else {
                      self.val.py = self.val.py.concat(",").concat(path);
                    }
                  }
                  break;
                case "ZIP":
                case "TGZ":
                case "TAR.GZ":
                case "GZ":
                case "BZIP":
                  if (reason.toUpperCase() !== ".ZIP") {
                    growl.error("Invalid file type selected. Found: " + ext);
                  } else {
                    if (self.val.archives === "") {
                      self.val.archives = path;
                    } else {
                      self.val.archives = self.val.archives.concat(",").concat(path);
                    }
                  }
                  break;
                case "*":
                  if (self.val.files === "") {
                    self.val.files = path;
                  } else {
                    self.val.files = self.val.files.concat(",").concat(path);
                  }
                  break;
                default:
                  growl.error("Invalid file type selected: " + reason);
                  break;
              }
            };


            $window.uploadDone = function () {
              stopLoading();
            };

            $scope.trustSrc = function (src) {
              return $sce.trustAsResourceUrl(self.ui);
            };

            self.tensorflow = function () {
              $scope.mode = "tensorflow";
            };

            self.spark = function () {
              $scope.mode = "spark";
            };

            self.restart = function () {
              $location.path('/#!/project/' + self.projectId + '/jupyter');
            };


            var installTourLibs = function () {
              //Install numpy
              var data = {"channelUrl": "default", "lib": "numpy", "version": "1.13.1"};
              PythonDepsService.install(self.projectId, data).then(
                      function (success) {
                        console.log("success numpy");
                        growl.info("Preparing Python Anaconda environment, please wait...", {ttl: 10000});
                      }, function (error) {
                console.log("failure numpy");
              });
            };
            var init = function () {
              JupyterService.running(self.projectId).then(
                      function (success) {
                        self.config = success.data;
                        self.ui = "/hopsworks-api/jupyter/" + self.config.port + "/?token=" + self.config.token;
                        self.toggleValue = true;
                      }, function (error) {
                // nothing to do
              }
              );
              JupyterService.settings(self.projectId).then(
                      function (success) {
                        self.val = success.data;
                        self.projectName = self.val.project.name;
                        if (self.val.dynamicMinExecutors < 1) {
                          self.val.dynamicMinExecutors = 1;
                        }
                        self.sliderOptions.min = self.val.dynamicMinExecutors;
                        self.sliderOptions.max = self.val.dynamicMaxExecutors;
                        self.toggleValue = true;
                        if (self.val.project.name.startsWith("demo_tensorflow")) {
                          self.tensorflow();
                          self.val.mode = "tensorflow";
                          self.advanced = true;
                          //Activate anaconda
                          PythonDepsService.enabled(self.projectId).then(
                                  function (success) {
                                  }, function (error) {
                            growl.info("Preparing Python Anaconda environment, please wait...", {ttl: 20000});
                            PythonDepsService.enable(self.projectId, "2.7", "true").then(
                                    function (success) {
                                      setTimeout(installTourLibs, 20000);

                                    }, function (error) {
                              growl.error("Could not enable Anaconda", {title: 'Error', ttl: 5000});
                            });
                          });

                        } else {
                          self.val.mode = "sparkDynamic";
                        }
                        if (self.val.logLevel === "FINE") {
                          self.logLevelSelected = self.log_levels[0];
                        } else if (self.val.logLevel === "DEBUG") {
                          self.logLevelSelected = self.log_levels[1];
                        } else if (self.val.logLevel === "INFO") {
                          self.logLevelSelected = self.log_levels[2];
                        } else if (self.val.logLevel === "WARN") {
                          self.logLevelSelected = self.log_levels[3];
                        } else if (self.val.logLevel === "ERROR") {
                          self.logLevelSelected = self.log_levels[4];
                        } else {
                          self.logLevelSelected = self.log_levels[2];
                        }
                      }, function (error) {
                growl.error("Could not get Jupyter Notebook Server Settings.");
              }
              );
              self.livySessions(self.projectId);

            };

            self.openWindow = function () {
              $window.open(self.ui, '_blank');
            }


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

              JupyterService.stop(self.projectId).then(
                      function (success) {
                        self.ui = "";
                        stopLoading();
                        self.mode = "dynamicSpark";
                      }, function (error) {
                growl.error("Could not stop the Jupyter Notebook Server.");
                stopLoading();
              }
              );



            };

            self.stopDataOwner = function (hdfsUsername) {
              startLoading("Stopping Jupyter...");
              JupyterService.stopDataOwner(self.projectId, hdfsUsername).then(
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
              JupyterService.stopAdmin(self.projectId, hdfsUsername).then(
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



            self.start = function () {
              startLoading("Connecting to Jupyter...");
              $scope.tgState = true;
              self.setInitExecs();

              JupyterService.start(self.projectId, self.val).then(
                      function (success) {
                        self.toggleValue = true;
                        self.config = success.data;

                        self.ui = "/hopsworks-api/jupyter/" + self.config.port + "/?token=" + self.config.token;
                        $window.open(self.ui, '_blank');
                        $timeout(stopLoading(), 5000);

                      }, function (error) {
                if (error.data !== undefined && error.status === 404) {
                  growl.error("Anaconda not enabled yet - retry starting Jupyter again in a few seconds.");
                } else if (error.data !== undefined && error.status === 400) {
                  growl.error("Anaconda not enabled yet - retry starting Jupyter again in a few seconds.");
                } else {
                  growl.error("Could not start Jupyter.");
                }
                stopLoading();
                self.toggleValue = true;
              }
              );

            };



          }]);
