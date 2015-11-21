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
        
        $scope.switchDirection = function(projectName) {
          self.toHDFS = ! self.toHDFS;
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
              "charonPath" : self.selectedFile,
              "hdfsPath" : self.selectedDir
            }; 
            console.log("Copy from Charon from HDFS: " + JSON.stringify(op));
            charonService.copyFromCharonToHdfs(op)
          } else {
            var op = {
              "charonPath" : self.selectedDir,
              "hdfsPath" : self.selectedFile
            };             
            console.log("Copy from HDFS to Chron: " + JSON.stringify(op));
            charonService.copyFromHdfsToCharon(op)
          }
        };
        
        self.selectHdfsFile = function () {
          ModalService.selectFile('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onFileSelected("hdfs:/" + success);
              }, function (error) {
            //The user changed their mind.
          });
        };
        
        self.selectHdfsDir = function () {
          ModalService.selectDir('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onDirSelected("hdfs:/" + success);
              }, function (error) {
            //The user changed their mind.
          });
        };


        self.selectCharonFile = function () {
          ModalService.selectLocalFile('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onFileSelected(success);
              }, function (error) {
            //The user changed their mind.
          });
        };
        
        self.selectCharonDir = function () {
          ModalService.selectLocalDir('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onDirSelected(success);
              }, function (error) {
            //The user changed their mind.
          });
        };



        self.init = function () {
        };

        self.init();

      }]);
