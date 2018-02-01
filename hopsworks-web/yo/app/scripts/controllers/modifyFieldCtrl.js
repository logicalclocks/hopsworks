/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

var mainModule = angular.module('hopsWorksApp')
        .controller('ModifyFieldCtrl',
                ['$cookies', '$scope', '$uibModalInstance', 'MetadataActionService',
                  function ($cookies, $scope, $uibModalInstance, MetadataActionService) {

                    var self = this;

                    self.card = $scope.field;
                    self.fieldName = $scope.field.title;
                    self.fieldDescription = $scope.field.description;
                    self.fieldType = "";
                    self.fieldTypeValues = "";

                    self.fieldSelectItems = [];
                    self.multiSelectItems = [];
                    self.selectedItem = "1";
                    self.existingRawData = false;
                    self.updating = false;
                    
                    self.items = [];

                    MetadataActionService.fetchFieldTypes($cookies.get('email'))
                            .then(function (response) {
                              console.log(JSON.stringify(response));
                              var content = JSON.parse(response.board);

                              //construct the select component's contents
                              angular.forEach(content.fieldTypes, function (value, key) {
                                self.items.push({id: value.id, name: value.description});

                                if (value.id === $scope.field.fieldtypeid) {
                                  self.selectedItem = self.items[key];
                                }
                              });
                            });

                    MetadataActionService.isFieldEmpty($cookies.get('email'), $scope.field.id)
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
                        self.fieldType = "'a single-select dropdown list'";
                        self.fieldTypeValues = "Existing values: ";
                        angular.forEach($scope.field.fieldtypeContent, function (value, key) {
                          self.fieldTypeValues += value.value + ", ";
                        });
                        break;
                      case 3:
                        self.fieldType = "'a multi-select dropdown list'";
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
                          fieldTypeContent = self.multiSelectItems;
                      }

                      $uibModalInstance.close({id: $scope.field.id, title: self.fieldName, details: $scope.field.details,
                        editing: $scope.field.editing, find: $scope.field.find, required: $scope.field.required,
                        sizefield: $scope.field.sizefield, description: self.fieldDescription,
                        fieldtypeid: self.selectedItem.id, fieldtypeContent: fieldTypeContent, position: $scope.field.position});
                    };

                    self.hitEnter = function (evt) {
                      if (angular.equals(evt.keyCode, 13)) {
                        self.modifyField();
                      }
                    };

                    self.cancel = function () {
                      $uibModalInstance.dismiss('canceled');
                    };

                    self.update = function () {
                      self.updating = true;
                      switch (self.selectedItem.id) {
                        case 1:
                          self.multiSelectItems = [];
                          self.fieldSelectItems = [];
                          break;
                        case 2:
                          self.multiSelectItems = [];
                          self.addNewSelectChoice(1);
                          break;
                        case 3:
                          self.fieldSelectItems = [];
                          self.addNewSelectChoice(2);
                      }
                    };

                    self.addNewSelectChoice = function (index) {

                      switch (index) {
                        case 1:
                          //add a single selection item
                          self.fieldSelectItems.push({id: -1, fieldid: $scope.field.id, value: ""});
                          break;
                        case 2:
                          self.multiSelectItems.push({id: -1, fieldid: $scope.field.id, value: ""});
                      }

                    };

                    self.removeSelectChoice = function (index) {

                      switch (index) {
                        case 1:
                          var lastItem = self.fieldSelectItems.length - 1;
                          self.fieldSelectItems.splice(lastItem);
                          break;
                        case 2:
                          var lastItem = self.multiSelectItems.length - 1;
                          self.multiSelectItems.splice(lastItem);
                      }
                    };

                  }]);