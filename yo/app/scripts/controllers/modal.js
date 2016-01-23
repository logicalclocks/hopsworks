'use strict';

angular.module('hopsWorksApp')
        .controller('ModalCtrl', ['$modalInstance', '$scope', 'title', 'msg','val',
          function ($modalInstance, $scope, title, msg, val) {

            var self = this;
            self.title = title;
            self.msg = msg;
            self.val = {name:''};

            self.ok = function () {
              $modalInstance.close({val: self.val});
            };

            self.cancel = function () {
              $modalInstance.dismiss('cancel');
            };
            
            self.reject = function () {
              $modalInstance.dismiss('reject');
            };

          }]);
