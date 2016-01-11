'use strict';

angular.module('hopsWorksApp')
    .controller('CharonCtrl', ['$scope', '$routeParams',
      'growl', 'ModalService', 'CharonService',
      function ($scope, $routeParams, growl, ModalService, CharonService) {

        var self = this;
        self.projectId = $routeParams.projectID;
        var charonService = CharonService(self.projectId);

        self.working = false;
        self.selectedHdfsPath = "";
        self.toHDFS = true;
        self.charonFilename = "";

        $scope.switchDirection = function (projectName) {
          self.toHDFS = !self.toHDFS;
          self.selectedCharonPath = "";
          self.selectedHdfsPath = "";
          if (!self.toHDFS) {
            self.selectedCharonPath = "/srv/Charon/charon_fs/" + projectName;
          }
        }
        /**
         * Callback for when the user selected a file.
         * @param {String} reason
         * @param {String} path
         * @returns {undefined}
         */
        self.onCharonPathSelected = function (path) {
          self.selectedCharonPath = path;
        };

        self.onHdfsPathSelected = function (path) {
          self.selectedHdfsPath = path;
        };

        self.copyFile = function () {
          self.working = true;
          if (self.toHDFS === true) {
            var op = {
              "charonPath": self.selectedCharonPath,
              "hdfsPath": self.selectedHdfsPath
            };
            charonService.copyFromCharonToHdfs(op)
                .then(function (success) {
                    self.working = false;
                  growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
                },
                    function (error) {
                      self.working = false;
                      growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                    });
          } else {
            var op = {
              "charonPath": self.selectedCharonPath,
              "hdfsPath": self.selectedHdfsPath
            };
            charonService.copyFromHdfsToCharon(op)
                .then(function (success) {
                    self.working = false;
                  growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
                },
                    function (error) {
                      self.working = false;
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
