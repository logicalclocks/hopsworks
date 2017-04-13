/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp').controller('NewlistCtrl',
        ['$scope', '$uibModalInstance', '$location', function ($scope, $uibModalInstance, $location) {

            /* handle the close button */
            $scope.close = function () {
              $uibModalInstance.close();
              //$location.path('/metaDesign');
            };

            /* handle the form submit */
            $scope.createNewList = function () {
              if (!this.newListForm.$valid) {
                return false;
              }
              var listId = -1;
              $uibModalInstance.close({id: listId, name: this.name, cards: []});
            };
          }]);