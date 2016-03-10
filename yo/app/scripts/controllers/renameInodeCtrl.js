/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';


angular.module('hopsWorksApp')
        .controller('RenameInodeCtrl', ['$scope', '$modalInstance',
          function ($scope, $modalInstance) {

            var self = this;

            self.inode = {};
            self.newName = '';

            self.cancel = function () {
              $modalInstance.dismiss('Canceled');
            };

            self.renameInode = function () {


              $modalInstance.close({
              });
            };

          }]);