'use strict';
/*
 * Controller for the file selection dialog. 
 */
angular.module('hopsWorksApp')
        .controller('SelectFileCtrl', ['$modalInstance', '$scope',
          function ($modalInstance, $scope) {

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
            self.select = function(filepath){
              selectedFilePath = filepath;
            }
            
            self.confirmSelection = function(){
              $modalInstance.close(selectedFilePath);
            }

          }]);
