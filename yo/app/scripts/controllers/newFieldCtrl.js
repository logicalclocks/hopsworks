/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';


angular.module('hopsWorksApp')
        .controller('NewFieldCtrl', ['$cookies', '$scope', '$modalInstance', 'MetadataActionService', '$filter',
          function ($cookies, $scope, $modalInstance, MetadataActionService, $filter) {

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

            MetadataActionService.fetchFieldTypes($cookies['email'])
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
              $modalInstance.dismiss('Canceled');
            };

            self.saveField = function () {
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

              $modalInstance.close({
                id: this.id,
                title: this.name,
                details: this.details,
                editing: this.editing,
                find: this.find,
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