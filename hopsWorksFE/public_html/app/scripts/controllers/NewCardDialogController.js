/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */


'use strict';

angular.module('metaUI').controller('NewCardDialogController', ['$scope', '$modalInstance', 'data', function ($scope, $modalInstance, data) {

        //data is the actual column under processing
        $scope.columnName = data.name;
        $scope.id = -1;
        $scope.title = '';
        $scope.details = '';
        $scope.editing = false;
        $scope.find = false;
        $scope.required = false;
        $scope.sizefield = {showing: false, value: ""};

        // Dialog service
        $scope.cancel = function () {
            $modalInstance.dismiss('Canceled');
        };

        $scope.saveCard = function () {
            if (!this.newCardForm.$valid) {
                return false;
            }

            $modalInstance.close({id: this.id, title: this.title, details: this.details,
                editing: this.editing, find: this.find, required: this.required, sizefield: this.sizefield});
        };

        $scope.hitEnter = function (evt) {
            if (angular.equals(evt.keyCode, 13))
                $scope.saveCard();
        };

    }]);

