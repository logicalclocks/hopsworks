/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('metaUI')
        .controller('NewCardController', ['$scope', '$modalInstance', 'column', function ($scope, $modalInstance, column) {

                function initScope(scope) {
                    scope.id = -1;
                    scope.columnName = column.name;
                    scope.column = column;
                    scope.title = '';
                    scope.description = '';
                    scope.type = '';
                    scope.details = '';
                    scope.editing = false;
                    scope.find = false;
                    scope.required = false;
                    scope.sizefield = {showing: false, value: ""};
                }

                $scope.addNewCard = function () {
                    if (!this.newCardForm.$valid) {
                        return false;
                    }

                    $modalInstance.close({id: this.id, title: this.title, column: column, details: this.details,
                        editing: this.editing, find: this.find, required: this.required, sizefield: this.sizefield});
                };

                $scope.close = function () {
                    $modalInstance.close();
                };

                initScope($scope);

            }]);