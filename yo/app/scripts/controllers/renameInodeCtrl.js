/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';


angular.module('hopsWorksApp')
        .controller('RenameInodeCtrl', ['$scope', '$modalInstance', 'newName',
          function ($scope, $modalInstance, newName) {

            var self = this;

            self.newName = newName;

            self.cancel = function () {
              $modalInstance.dismiss('Canceled');
            };

            self.renameInode = function () {
              $modalInstance.close({
              });
            };

          }]);