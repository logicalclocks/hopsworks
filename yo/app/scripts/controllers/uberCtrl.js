'use strict';

angular.module('hopsWorksApp')
        .controller('UberCtrl', ['$uibModalInstance',  'title', 'msg', 'price',
          function ($uibModalInstance, title, msg, price) {

            var self = this;
            self.title = title;
            self.msg = msg;
            self.price = price;
            
            self.ok = function () {
              $uibModalInstance.close();
            };

            self.cancel = function () {
              $uibModalInstance.dismiss('cancel');
            };
            
            self.reject = function () {
              $uibModalInstance.dismiss('reject');
            };

          }]);
