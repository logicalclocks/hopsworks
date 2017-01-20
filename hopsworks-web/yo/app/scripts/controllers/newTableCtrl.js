/**
 * Created by AMore on 2015-05-15.
 */
angular.module('hopsWorksApp').controller('NewListCtrl',
        ['$scope', '$uibModalInstance', function ($scope, $uibModalInstance) {


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
              $uibModalInstance.close({id: listId, name: this.name});
            };

          }]);
