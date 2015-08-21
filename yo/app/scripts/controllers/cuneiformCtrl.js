/**
 * Created by stig on 2015-05-25.
 * Controller for the Cuneiform jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('CuneiformCtrl', ['$routeParams', 'growl', 'ModalService', 'CuneiformService','StorageService','$scope',
          function ($routeParams, growl, ModalService, CuneiformService,StorageService,$scope) {
            //Set all the variables required to be a jobcontroller:
            //For fetching job history
            var self = this;
            this.projectId = $routeParams.projectID;
            this.jobType = 'CUNEIFORM';
            this.growl = growl;
            //For letting the user select a file
            this.ModalService = ModalService;
            this.selectFileRegex = /.cf\b/;
            this.selectFileErrorMsg = "Please select a Cuneiform workflow. The file should have the extension '.cf'.";
            this.onFileSelected = function (path) {
              CuneiformService.inspectStoredWorkflow(this.projectId, path).then(
                      function (success) {
                        self.runConfig = success.data;
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
              var stored = StorageService.recover(self.projectId + "cuneiform");
              if (stored) {
                self.runConfig = stored.runConfig;
              }
            };
            init();

            /**
             * Close the poller if the controller is destroyed.
             */
            $scope.$on('$destroy', function () {
              var state = {
                "runConfig": self.runConfig
              };
              StorageService.store(self.projectId + "cuneiform", state);
            });

          }]);


