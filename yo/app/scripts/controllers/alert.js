'use strict';

angular.module('hopsWorksApp')
        .controller('AlertCtrl', ['$modalInstance', '$scope', 'title', 'msg',
          function ($modalInstance, $scope, title, msg) {

            var self = this;
            self.title = title;
            self.msg = msg;

            self.ok = function () {
              $modalInstance.close();
            };
          }]);

