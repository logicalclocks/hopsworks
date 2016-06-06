'use strict';

angular.module('hopsWorksApp')
        .controller('UberCtrl', ['$modalInstance',  'title', 'msg', 'price',
          function ($modalInstance, title, msg, price) {

            var self = this;
            self.title = title;
            self.msg = msg;
            self.price = price;
            
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
