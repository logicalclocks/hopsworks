'use strict';
/*
 * Controller for the file selection dialog. 
 */
angular.module('hopsWorksApp')
        .controller('SelectFileCtrl', ['$modalInstance', 'growl', 'regex', 'errorMsg',
          function ($modalInstance, growl, regex, errorMsg) {

            var self = this;

            var selectedFilePath;
            self.isDir = false;

            /**
             * Close the modal dialog.
             * @returns {undefined}
             */
            self.close = function () {
              $modalInstance.dismiss('cancel');
            };

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
              } else if (!selectedFilePath.match(regex)) {
                growl.error(errorMsg, {title: "Invalid file extension", ttl: 15000});
              } else if (self.isDir !== isDirectory) {
                var msg;
                if (isDir) {
                  msg = "You should select a directory."
                } else {
                  msg = "You should select a file."
                }
                growl.error(errorMsg, {title: msg, ttl: 10000});
              } else {
                $modalInstance.close(selectedFilePath);
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
