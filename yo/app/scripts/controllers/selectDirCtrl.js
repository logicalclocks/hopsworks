'use strict';
/*
 * Controller for the file selection dialog. 
 */
angular.module('hopsWorksApp')
        .controller('SelectDirCtrl', ['$modalInstance', 'growl', 'regex', 'errorMsg',
          function ($modalInstance, growl, regex, errorMsg) {

            var self = this;

            var selectedPath;

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
              selectedPath = filepath;
              if (filepath.dir) {
                self.confirmSelection();
              }
            };

            self.confirmSelection = function () {
              if (selectedPath == null) {
                growl.error("Please select a directory.", {title: "No directory selected", ttl: 2000});
              } else {
                $modalInstance.close("hdfs://" + selectedPath);
              }
            };

            self.dblClick = function (datasetsCtrl, file) {
              if (file.dir) {
                datasetsCtrl.openDir(file);
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
