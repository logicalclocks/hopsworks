'use strict';

angular.module('hopsWorksApp')
        .controller('UberCtrl', ['$uibModalInstance',  'title', 'msg', 'generalPrice','gpuPrice',
          function ($uibModalInstance, title, msg, generalPrice, gpuPrice) {

            var self = this;
            self.title = title;
            self.msg = msg;
            self.generalPrice = generalPrice;
            self.gpuPrice = gpuPrice;
            
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
