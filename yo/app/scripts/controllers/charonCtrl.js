'use strict';

angular.module('hopsWorksApp')
    .controller('CharonCtrl', ['$routeParams',
      'growl', 'ModalService', 'DataSetService',
      function ($routeParams, growl, ModalService, DataSetService) {

        var self = this;
        self.projectId = $routeParams.projectID;

        self.selectedFile = "";
        self.selectedDir = "";
        self.toHDFS = false;
        self.fromHDFS = true;
        self.charonFilename = "";

        self.setToHDFS = function() {
          self.toHDFS = true;
          self.fromHDFS = false;
          self.selectedFile = "";
        }
        
        self.setFromHDFS = function() {
          self.toHDFS = false;
          self.fromHDFS = true;
          self.selectedFile = "";
        }
        /**
         * Callback for when the user selected a file.
         * @param {String} reason
         * @param {String} path
         * @returns {undefined}
         */
        self.onFileSelected = function (path) {
          var filename = getFileName(path);
          self.selectedFile = filename;
        };
        
        self.onDirSelected = function (path) {
          self.selectedDir = path;
        };

        self.selectFile = function () {
          ModalService.selectFile('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onFileSelected(success);
              }, function (error) {
            //The user changed their mind.
          });
        };
        
        self.selectDir = function () {
          ModalService.selectDir('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onDirSelected(success);
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
