/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

var mainModule = angular.module('metaUI').controller('ModifyFieldController',
        ['$scope', '$rootScope', '$modalInstance', 'data', 'BoardService',
            function ($scope, $rootScope, $modalInstance, data, BoardService) {

                $scope.fieldName = data.field.title;
                $scope.fieldDescription = data.field.description;
                $scope.fieldType = "";
                $scope.fieldTypeValues = "";

                $scope.fieldSelectItems = [];
                $scope.yesNoItems = [];
                $scope.selectedItem = "";
                $scope.existingRawData = false;

                $scope.items = [];

                BoardService.fetchFieldTypes()
                        .then(function (response) {
                            //construct the select component's contents
                            angular.forEach(response, function (value, key) {
                                angular.forEach(value, function (innerValue) {
                                    $scope.items.push({id: innerValue.id, name: innerValue.description});
                                });
                            });
                            $scope.selectedItem = $scope.items[0];
                        });

                BoardService.viewMetadata(data.table)
                        .then(function (response) {

                            angular.forEach(response.fields, function (field, key) {

                                if (field.data.length !== 0 && field.id === data.field.id) {
                                    $scope.existingRawData = true;
                                }
                            });
                        });

                switch (data.field.fieldtypeid) {
                    case 1:
                        $scope.fieldType = "'a text field'";
                        break;
                    case 2:
                        $scope.fieldType = "'a dropdown list'";
                        $scope.fieldTypeValues = "Existing values: ";
                        angular.forEach(data.field.fieldtypeContent, function (value, key) {
                            $scope.fieldTypeValues += value.value + ", ";
                        });
                        break;
                    case 3:
                        $scope.fieldType = "'a true/false field'";
                        $scope.fieldTypeValues = "Existing values: ";
                        angular.forEach(data.field.fieldtypeContent, function (value, key) {
                            $scope.fieldTypeValues += value.value + ", ";
                        });
                }

                $scope.modifyField = function () {
                    if (!this.modifyFieldForm.$valid) {
                        return false;
                    }

                    var fieldTypeContent = [];
                    switch ($scope.selectedItem.id) {
                        case 1:
                            fieldTypeContent = [{id: -1, fieldid: -1, value: ""}];
                            break;
                        case 2:
                            fieldTypeContent = $scope.fieldSelectItems;
                            break;
                        case 3:
                            fieldTypeContent = $scope.yesNoItems;
                    }

                    $modalInstance.close({id: data.field.id, title: $scope.fieldName, details: data.field.details,
                        editing: data.field.editing, find: data.field.find, required: data.field.required,
                        sizefield: data.field.sizefield, description: $scope.fieldDescription,
                        fieldtypeid: $scope.selectedItem.id, fieldtypeContent: fieldTypeContent});
                };

                $scope.cancel = function () {
                    $modalInstance.dismiss('canceled');
                };

                $scope.update = function () {

                    switch ($scope.selectedItem.id) {
                        case 1:
                            $scope.yesNoItems = [];
                            $scope.fieldSelectItems = [];
                            break;
                        case 2:
                            $scope.yesNoItems = [];
                            $scope.addNewSelectChoice();
                            break;
                        case 3:
                            $scope.fieldSelectItems = [];
                            $scope.addYesnoChoice();
                    }
                };

                $scope.addNewSelectChoice = function () {
                    var newItemNo = $scope.fieldSelectItems.length + 1;
                    //$scope.choices.push({'id': 'choice' + newItemNo});
                    $scope.fieldSelectItems.push({id: -1, fieldid: data.field.id, value: ""});
                };

                $scope.removeSelectChoice = function () {
                    var lastItem = $scope.fieldSelectItems.length - 1;
                    //$scope.choices.splice(lastItem);
                    $scope.fieldSelectItems.splice(lastItem);
                };

                $scope.addYesnoChoice = function () {
                    $scope.yesNoItems.push({id: -1, fieldid: data.field.id, value: ""});
                    $scope.yesNoItems.push({id: -1, fieldid: data.field.id, value: ""});
                };
            }]);


