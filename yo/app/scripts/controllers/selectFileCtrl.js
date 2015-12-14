'use strict';
/*
 * Controller for the file selection dialog. 
 */
angular.module('hopsWorksApp')
        .controller('SelectFileCtrl', ['growl',
          function ( growl) {

            var self = this;

            var selectedFilePath;
            self.isDir = false;


            /**
             * Select a file.
             * @param {type} filepath
             * @returns {undefined}elf
             */
            self.select = function (filepath, isDirectory) {
              selectedFilePath = filepath;
              self.isDir = isDirectory;
            };

            self.confirmSelection = function (isDirectory) {
              if (selectedFilePath == null) {
                growl.error("Please select a file.", {title: "No file selected", ttl: 15000});
              } else if (self.isDir !== isDirectory) {
                var msg;
                if (self.isDir) {
                  msg = "You should select a directory."
                } else {
                  msg = "You should select a file."
                }
                growl.error(errorMsg, {title: msg, ttl: 10000});
              }
            };

            self.dblClick = function (datasetsCtrl, file) {
              if (file.dir) {
                self.select(file.path, true);
                datasetsCtrl.openDir(file);
              } else {
                self.select(file.path, false);
                self.confirmSelection(false);
              }
            };

            self.back = function (datasetsCtrl) {
              if (datasetsCtrl.pathArray.length <= 1) {
                datasetsCtrl.getAllDatasets();
              } else {
                datasetsCtrl.back();
              }
            };

          }]);
