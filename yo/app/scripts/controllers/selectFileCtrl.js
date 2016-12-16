'use strict';
/*
 * Controller for the file selection dialog. 
 */
angular.module('hopsWorksApp')
        .controller('SelectFileCtrl', ['$uibModalInstance', 'growl', 'regex', 'errorMsg',
          function ($uibModalInstance, growl, regex, errorMsg) {

            var self = this;

            var selectedFilePath;
            self.isDir = false;

            /**
             * Close the modal dialog.
             * @returns {undefined}
             */
            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };

            /**
             * Select a file.
             * @param {type} filepath
             * @param {type} isDirectory
             * @returns {undefined}
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
              } else {
                $uibModalInstance.close(selectedFilePath);
              }
            };

            self.click = function (datasetsCtrl, file, isDirectory) {
              if (file.dir) {
                self.select(file.name, true);
                datasetsCtrl.openDir(file);
              } else {
                self.select(file.path, false);
                if(!isDirectory){
                  self.confirmSelection(false);
                } else {
                  growl.warning("", {title: "Please select a directory", ttl: 5000});
                  self.back(datasetsCtrl);
                }
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
