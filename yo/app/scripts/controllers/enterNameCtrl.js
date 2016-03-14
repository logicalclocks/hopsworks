/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';


angular.module('hopsWorksApp')
        .controller('EnterNameCtrl', ['$scope', '$modalInstance', 'title', 'newName',
          function ($scope, $modalInstance,  title, newName) {
            
            var self = this;

            self.newName = newName;
            self.title = title;
            
            self.ok = function () {
              $modalInstance.close({newName: self.newName});
            };

            self.cancel = function () {
              $modalInstance.dismiss('cancel');
            };
            
            self.reject = function () {
              $modalInstance.dismiss('reject');
            };

          }]);