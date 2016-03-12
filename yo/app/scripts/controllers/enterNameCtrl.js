/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';


angular.module('hopsWorksApp')
        .controller('EnterNameCtrl', ['$scope', '$modalInstance', 'title', 'msg', 'newName',
          function ($scope, $modalInstance,  title, msg, newName) {
,
            var self = this;

            self.newName = newName;
            self.title = title;
            self.msg = msg;
            
            self.ok = function () {
              $modalInstance.close();
            };

            self.cancel = function () {
              $modalInstance.dismiss('cancel');
            };
            
            self.reject = function () {
              $modalInstance.dismiss('reject');
            };

          }]);