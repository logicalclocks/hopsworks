/**
 * Controller for the Spark fatjar jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('SparkCtrl', ['$routeParams', 'growl', 'SparkService', 'ModalService', 'StorageService', '$scope',
          function ($routeParams, growl, SparkService, ModalService, StorageService, $scope) {

            //Set all the variables required to be a jobcontroller:
            //For fetching job history
            var self = this;
            this.projectId = $routeParams.projectID;
            this.jobType = 'SPARK';
            this.growl = growl;
            //For letting the user select a file
            this.ModalService = ModalService;
            this.selectFileRegex = /.jar\b/;
            this.selectFileErrorMsg = "Please select a jar file.";
            this.onFileSelected = function (path) {
              this.selectedJar = getFileName(path);
              SparkService.inspectJar(this.projectId, path).then(
                      function (success) {
                        self.sparkConfig = success.data;
                        self.phasekeeper.mainFileSelected(getFileName(path));
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            this.selectFile = function (phasekeeper) {
              self.phasekeeper = phasekeeper;
              selectFile(this);
            };

            var init = function () {
              var stored = StorageService.recover(self.projectId + "spark");
              if (stored) {
                self.selectedJar = stored.selectedJar;
                self.sparkConfig = stored.sparkConfig;
              }
            };
            init();

            /**
             * Close the poller if the controller is destroyed.
             */
            $scope.$on('$destroy', function () {
              var state = {
                "sparkConfig": self.sparkConfig,
                "selectedJar": self.selectedJar
              };
              StorageService.store(self.projectId + "spark", state);
            });

          }]);


