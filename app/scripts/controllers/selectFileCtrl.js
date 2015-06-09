'use strict';

angular.module('hopsWorksApp')
        .controller('SelectFileCtrl', ['$modalInstance', '$scope',
          function ($modalInstance, $scope) {

            var self = this;

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };

          }]);
