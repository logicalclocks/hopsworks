/**
 * Created by AMore on 2015-05-15.
 */

'use strict';


angular.module('hopsWorksApp')
  .controller('NewCardCtrl',
  ['$scope', '$modalInstance', 'WSComm',
    function ($scope, $modalInstance, WSComm) {

      console.log('got in: ');
      console.log($scope.currentColumn.name);
      //data is the actual column under processing
      $scope.columnName = $scope.currentColumn.name;
      $scope.id = -1;
      $scope.title = '';
      $scope.details = '';
      $scope.description = '';
      $scope.cardType = '';
      $scope.editing = false;
      $scope.find = false;
      $scope.required = false;
      $scope.sizefield = {showing: false, value: ""};

      $scope.items = [];
      $scope.fieldSelectItems = [];
      $scope.yesNoItems = [];
      $scope.selectedItem = "";

      WSComm.send({
          sender: 'evsav',
          type: 'FieldTypesMessage',
          action: 'fetch_field_types',
          message: 'null'
        }).then(
        function (success) {
          console.log('success from fetch_field_types');
          success = JSON.parse(success.board);
          console.log(success);

          //construct the select component's contents
          angular.forEach(success.fieldTypes, function (type) {
            $scope.items.push({
              id: type.id,
              name: type.description
            });
          });

          $scope.selectedItem = $scope.items[0];
        },
      function(error){
        console.log(error);
      });

      // Dialog service
      $scope.cancel = function () {
        $modalInstance.dismiss('Canceled');
      };

      $scope.saveCard = function () {
        if (!this.newCardForm.$valid) {
          console.log('false');
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

        $modalInstance.close({
          id: this.id,
          title: this.title,
          details: this.details,
          editing: this.editing,
          find: this.find,
          required: this.required,
          sizefield: this.sizefield,
          description: this.description,
          fieldtypeid: this.selectedItem.id,
          fieldtypeContent: fieldTypeContent
        });
      };

      $scope.hitEnter = function (evt) {
        if (angular.equals(evt.keyCode, 13))
          $scope.saveCard();
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
        $scope.fieldSelectItems.push({id: -1, fieldid: -1, value: ""});
      };

      $scope.removeSelectChoice = function () {
        var lastItem = $scope.fieldSelectItems.length - 1;
        //$scope.choices.splice(lastItem);
        $scope.fieldSelectItems.splice(lastItem);
      };

      $scope.addYesnoChoice = function () {
        $scope.yesNoItems.push({id: -1, fieldid: -1, value: ""});
        $scope.yesNoItems.push({id: -1, fieldid: -1, value: ""});
      };
    }]);
