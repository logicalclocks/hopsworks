'use strict';

angular.module('hopsWorksApp')
        .controller('InputModalCtrl', ['$uibModalInstance', 'title', 'msg','val',
          function ($uibModalInstance, title, msg, val) {

            var self = this;
            self.title = title;
            self.msg = msg;
            self.val = {name:''};

            self.ok = function () {
              $uibModalInstance.close({val: self.val});
            };

            self.cancel = function () {
              $uibModalInstance.dismiss('cancel');
            };
            
            self.reject = function () {
              $uibModalInstance.dismiss('reject');
            };

          }]);


