'use strict';
/*
 * Controller for the file selection in Charon.
 */
angular.module('hopsWorksApp')
  .controller('SelectFileCharonCtrl', ['growl',
    function ( growl) {

      var self = this;

      var selectedFilePath;
      self.isDir = false;

      self.select = function (filepath, isDirectory) {
        selectedFilePath = filepath;
        self.isDir = isDirectory;
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
