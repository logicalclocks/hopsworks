/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

var mainModule = angular.module('hopsWorksApp')
        .controller('ModifyFieldCtrl',
                ['$cookies', '$scope', '$modalInstance', 'MetadataActionService',
                  function ($cookies, $scope, $modalInstance, MetadataActionService) {

                    var self = this;
                    
                    self.card = $scope.field;
                    self.fieldName = $scope.field.title;
                    self.fieldDescription = $scope.field.description;
                    self.fieldType = "";
                    self.fieldTypeValues = "";

                    self.fieldSelectItems = [];
                    self.yesNoItems = [];
                    self.selectedItem = "1";
                    self.existingRawData = false;

                    self.items = [];

                    MetadataActionService.fetchFieldTypes($cookies['email'])
                            .then(function (response) {
                              var content = JSON.parse(response.board);
                              
                              //construct the select component's contents
                              angular.forEach(content.fieldTypes, function (value, key) {
                                self.items.push({id: value.id, name: value.description});
                                
                                if(value.id === $scope.field.fieldtypeid){
                                  console.log("selected item id " + JSON.stringify(value));
                                  self.selectedItem = self.items[key];
                                }
                              });
                            });

                    MetadataActionService.isFieldEmpty($cookies['email'], $scope.field.id)
                            .then(function (response) {

                              if (response.board !== "EMPTY") {
                                self.existingRawData = true;
                              }
                            });

                    switch ($scope.field.fieldtypeid) {
                      case 1:
                        self.fieldType = "'a text field'";
                        break;
                      case 2:
                        self.fieldType = "'a dropdown list'";
                        self.fieldTypeValues = "Existing values: ";
                        angular.forEach($scope.field.fieldtypeContent, function (value, key) {
                          self.fieldTypeValues += value.value + ", ";
                        });
                        break;
                      case 3:
                        self.fieldType = "'a true/false field'";
                        self.fieldTypeValues = "Existing values: ";
                        angular.forEach($scope.field.fieldtypeContent, function (value, key) {
                          self.fieldTypeValues += value.value + ", ";
                        });
                    }

                    self.modifyField = function () {
                      if (!$scope.modifyFieldForm.$valid) {
                        return false;
                      }

                      var fieldTypeContent = [];
                      switch (self.selectedItem.id) {
                        case 1:
                          fieldTypeContent = [{id: -1, fieldid: -1, value: ""}];
                          break;
                        case 2:
                          fieldTypeContent = self.fieldSelectItems;
                          break;
                        case 3:
                          fieldTypeContent = self.yesNoItems;
                      }

                      $modalInstance.close({id: $scope.field.id, title: self.fieldName, details: $scope.field.details,
                        editing: $scope.field.editing, find: $scope.field.find, required: $scope.field.required,
                        sizefield: $scope.field.sizefield, description: self.fieldDescription,
                        fieldtypeid: self.selectedItem.id, fieldtypeContent: fieldTypeContent});
                    };

                    self.cancel = function () {
                      $modalInstance.dismiss('canceled');
                    };

                    self.update = function () {

                      switch (self.selectedItem.id) {
                        case 1:
                          self.yesNoItems = [];
                          self.fieldSelectItems = [];
                          break;
                        case 2:
                          self.yesNoItems = [];
                          self.addNewSelectChoice();
                          break;
                        case 3:
                          self.fieldSelectItems = [];
                          self.addYesnoChoice();
                      }
                    };

                    self.addNewSelectChoice = function () {
                      var newItemNo = self.fieldSelectItems.length + 1;
                      //$scope.choices.push({'id': 'choice' + newItemNo});
                      self.fieldSelectItems.push({id: -1, fieldid: $scope.field.id, value: ""});
                    };

                    self.removeSelectChoice = function () {
                      var lastItem = self.fieldSelectItems.length - 1;
                      //$scope.choices.splice(lastItem);
                      self.fieldSelectItems.splice(lastItem);
                    };

                    self.addYesnoChoice = function () {
                      self.yesNoItems.push({id: -1, fieldid: $scope.field.id, value: ""});
                      self.yesNoItems.push({id: -1, fieldid: $scope.field.id, value: ""});
                    };
                  }]);