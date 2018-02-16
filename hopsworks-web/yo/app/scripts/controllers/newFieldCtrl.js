/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';


angular.module('hopsWorksApp')
        .controller('NewFieldCtrl', ['$cookies', '$scope', '$uibModalInstance', 'MetadataActionService', '$filter',
          function ($cookies, $scope, $uibModalInstance, MetadataActionService, $filter) {

            var self = this;

            //data is the actual column under processing
            self.columnName = $scope.currentColumn.name;
            self.id = -1;
            self.name = '';
            self.details = '';
            self.description = '';
            self.cardType = '';
            self.editing = false;
            self.find = false;
            self.required = false;
            self.sizefield = {showing: false, value: ""};

            self.items = [];
            self.fieldSelectItems = [];
            self.multiSelectItems = [];
            self.selectedItem = "";

            MetadataActionService.fetchFieldTypes($cookies.get('email'))
                    .then(function (success) {
                      success = JSON.parse(success.board);
                      /*
                       * sort the field types so that text option is always 
                       * the first one
                       */
                      var ordered = $filter('orderBy')(success.fieldTypes, 'id', false);

                      //construct the select component's contents
                      angular.forEach(ordered, function (type) {
                        self.items.push({id: type.id, name: type.description});
                      });

                      self.selectedItem = self.items[0];
                    },
                            function (error) {
                              console.log(error);
                            });

            // Dialog service
            self.cancel = function () {
              $uibModalInstance.dismiss('Canceled');
            };

            self.saveField = function () {
              $scope.newFieldForm.$submitted=true
              if (!$scope.newFieldForm.$valid) {
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

              $uibModalInstance.close({
                id: this.id,
                title: this.name,
                details: this.details,
                editing: this.editing,
                find: true,
                required: this.required,
                sizefield: this.sizefield,
                description: this.description,
                fieldtypeid: this.selectedItem.id,
                fieldtypeContent: fieldTypeContent,
                position: 0 //a new field hasn't gotten any user defined position yet
              });
            };

            self.hitEnter = function (evt) {
              if (angular.equals(evt.keyCode, 13)) {
                self.saveField();
                console.log("enter was hit");
              }
            };

            self.update = function () {

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
                  self.fieldSelectItems.push({id: -1, fieldid: -1, value: ""});
                  break;
                case 2:
                  self.multiSelectItems.push({id: -1, fieldid: -1, value: ""});
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