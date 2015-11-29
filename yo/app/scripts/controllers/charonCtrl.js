'use strict';

angular.module('hopsWorksApp')
    .controller('CharonCtrl', ['$scope', '$routeParams',
      'growl', 'ModalService', 'CharonService',
      function ($scope, $routeParams, growl, ModalService, CharonService) {

        var self = this;
        self.projectId = $routeParams.projectID;
        var charonService = CharonService(self.projectId);

        self.selectedFile = "";
        self.selectedDir = "";
        self.toHDFS = true;
        self.charonFilename = "";

        $scope.switchDirection = function (projectName) {
          self.toHDFS = !self.toHDFS;
          self.selectedFile = "";
          self.selectedDir = "";
          if (!self.toHDFS) {
            self.selectedDir = "/srv/charon_fs/" + projectName;
          }
        }
        /**
         * Callback for when the user selected a file.
         * @param {String} reason
         * @param {String} path
         * @returns {undefined}
         */
        self.onFileSelected = function (path) {
          self.selectedFile = path;
        };

        self.onDirSelected = function (path) {
          self.selectedDir = path;
        };

        self.copyFile = function () {

          if (self.toHDFS === true) {
            var op = {
              "charonPath": self.selectedFile,
              "hdfsPath": self.selectedDir
            };
            charonService.copyFromCharonToHdfs(op)
                .then(function (success) {
                  growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
                },
                    function (error) {
                      growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                    });
          } else {
            var op = {
              "charonPath": self.selectedDir,
              "hdfsPath": self.selectedFile
            };
            charonService.copyFromHdfsToCharon(op)
                .then(function (success) {
                  growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
                },
                    function (error) {
                      growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                    });
          }
        };

        self.selectHdfsFile = function () {
          ModalService.selectFile('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onFileSelected("hdfs:/" + success);
              }, function (error) {
            //The user changed their mind.
            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
          });
        };

        self.selectHdfsDir = function () {
          ModalService.selectDir('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onDirSelected("hdfs:/" + success);
              }, function (error) {
            //The user changed their mind.
            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
          });
        };


        self.selectCharonFile = function () {
          ModalService.selectLocalFile('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onFileSelected(success);
              }, function (error) {
            //The user changed their mind.
            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
          });
        };

        self.selectCharonDir = function () {
          ModalService.selectLocalDir('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onDirSelected(success);
              }, function (error) {
            //The user changed their mind.
            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
          });
        };



        self.init = function () {
        };

        self.init();

      }]);
