'use strict';
/*
 * Controller for the file selection dialog. 
 */
angular.module('hopsWorksApp')
        .controller('SelectFileCtrl', ['$modalInstance', 'growl', 'regex', 'errorMsg',
          function ($modalInstance, growl, regex, errorMsg) {

            var self = this;

            var selectedFilePath;

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
             * @returns {undefined}
             */
            self.select = function (filepath) {
              selectedFilePath = filepath;
            };

            self.confirmSelection = function () {
              if (selectedFilePath == null) {
                growl.error("Please select a file.", {title: "No file selected", ttl: 15000});
              } else if (!selectedFilePath.match(regex)) {
                growl.error(errorMsg, {title: "Invalid file extension", ttl: 15000});
              } else {
                $modalInstance.close(selectedFilePath);
              }
            };

            self.dblClick = function (datasetsCtrl, file) {
              if (file.dir) {
                datasetsCtrl.openDir(file.name, file.dir);
              } else {
                self.select(file.path);
                self.confirmSelection();
              }
            };

          }]);
