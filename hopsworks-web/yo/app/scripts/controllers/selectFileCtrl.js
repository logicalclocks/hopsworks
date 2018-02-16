/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

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
                self.select(file.path, true);
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
