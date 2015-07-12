/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

var mainModule = angular.module('hopsWorksApp')
        .controller('ModifyFieldCtrl',
                ['$scope', '$modalInstance', 'MetadataActionService',
                  function ($scope, $modalInstance, MetadataActionService) {

                    $scope.fieldName = $scope.field.title;
                    $scope.fieldDescription = $scope.field.description;
                    $scope.fieldType = "";
                    $scope.fieldTypeValues = "";

                    $scope.fieldSelectItems = [];
                    $scope.yesNoItems = [];
                    $scope.selectedItem = "";
                    $scope.existingRawData = false;

                    $scope.items = [];

                    MetadataActionService.fetchFieldTypes()
                            .then(function (response) {
                              var board = JSON.parse(response.board);

                              //construct the select component's contents
                              angular.forEach(board.fieldTypes, function (value, key) {
                                $scope.items.push({id: value.id, name: value.description});
                              });
                              $scope.selectedItem = $scope.items[0];
                            });

                    MetadataActionService.fetchTableMetadata($scope.tableid)
                            .then(function (response) {
                              console.log("TABLE METADATA RETRIEVED " + JSON.stringify(response.board));
                              angular.forEach(response.fields, function (field, key) {

                                if (field.data.length !== 0 && field.id === $scope.field.id) {
                                  $scope.existingRawData = true;
                                }
                              });
                            });

                    switch ($scope.field.fieldtypeid) {
                      case 1:
                        $scope.fieldType = "'a text field'";
                        break;
                      case 2:
                        $scope.fieldType = "'a dropdown list'";
                        $scope.fieldTypeValues = "Existing values: ";
                        angular.forEach($scope.field.fieldtypeContent, function (value, key) {
                          $scope.fieldTypeValues += value.value + ", ";
                        });
                        break;
                      case 3:
                        $scope.fieldType = "'a true/false field'";
                        $scope.fieldTypeValues = "Existing values: ";
                        angular.forEach($scope.field.fieldtypeContent, function (value, key) {
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

                      $modalInstance.close({id: $scope.field.id, title: $scope.fieldName, details: $scope.field.details,
                        editing: $scope.field.editing, find: $scope.field.find, required: $scope.field.required,
                        sizefield: $scope.field.sizefield, description: $scope.fieldDescription,
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
                      $scope.fieldSelectItems.push({id: -1, fieldid: $scope.field.id, value: ""});
                    };

                    $scope.removeSelectChoice = function () {
                      var lastItem = $scope.fieldSelectItems.length - 1;
                      //$scope.choices.splice(lastItem);
                      $scope.fieldSelectItems.splice(lastItem);
                    };

                    $scope.addYesnoChoice = function () {
                      $scope.yesNoItems.push({id: -1, fieldid: $scope.field.id, value: ""});
                      $scope.yesNoItems.push({id: -1, fieldid: $scope.field.id, value: ""});
                    };
                  }]);