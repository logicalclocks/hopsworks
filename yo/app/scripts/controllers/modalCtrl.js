'use strict';

angular.module('hopsWorksApp')
        .controller('ModalCtrl', ['$modalInstance',  'title', 'msg',
          function ($modalInstance, title, msg) {

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
