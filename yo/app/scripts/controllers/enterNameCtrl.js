/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';


angular.module('hopsWorksApp')
        .controller('EnterNameCtrl', ['$scope', '$uibModalInstance', 'title', 'newName',
          function ($scope, $uibModalInstance,  title, newName) {
            
            var self = this;

            self.newName = newName;
            self.title = title;
            
            self.ok = function () {
              $uibModalInstance.close({newName: self.newName});
            };

            self.cancel = function () {
              $uibModalInstance.dismiss('cancel');
            };
            
            self.reject = function () {
              $uibModalInstance.dismiss('reject');
            };

          }]);