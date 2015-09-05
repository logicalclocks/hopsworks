'use strict';

angular.module('hopsWorksApp')
        .controller('ModalCtrl', ['$modalInstance', '$scope', 'title', 'msg',
          function ($modalInstance, $scope, title, msg) {

            var self = this;
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
