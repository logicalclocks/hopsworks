'use strict';

angular.module('hopsWorksApp')
        .controller('AlertCtrl', ['$uibModalInstance', '$scope', 'title', 'msg',
          function ($uibModalInstance, $scope, title, msg) {

            var self = this;
            self.title = title;
            self.msg = msg;

            self.ok = function () {
              $uibModalInstance.close();
            };
          }]);

