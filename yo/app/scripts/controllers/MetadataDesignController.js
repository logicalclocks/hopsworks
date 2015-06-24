/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

var mainModule = angular.module('metaUI').controller('MetadataDesignController',
        ['$scope', '$rootScope', 'BoardService', 'DialogService', 'toaster', '$location',
          function ($scope, $rootScope, BoardService, DialogService, toaster, $location) {

            $scope.$on('event:auth-loginRequired', function () {
              $location.path('/error');
            });

            $scope.$on('refreshApp', function (event, data) {

              //data is the name of the side menu item that was clicked
              angular.forEach($scope.templates, function (value, key) {
                if (angular.equals(value.name, data)) {

                  BoardService.fetchTemplate(value.id)
                          .then(function (response) {
                            $rootScope.mainBoard = BoardService.mainBoard(JSON.parse(response.board));

                            $scope.templateId = value.id;
                            $rootScope.templateId = value.id;
                            $rootScope.templateName = value.name;
                          });
                }
              });
              refresh();
            });

            $scope.$on('refreshWhenExtendedTemplate', function (data) {

              refresh();
            });

            /*
             * important board initialization with the global $rootScope board variable
             * Contains all the columns and their corresponding cards.
             */
            $scope.mainBoard = $rootScope.mainBoard;

            var refresh = function () {
              setTimeout(function () {
                //reinitialization of the mainBoard and refresh the page just to allow the board to appear
                //$scope.mainBoard = $rootScope.mainBoard;
                $scope.mainBoard = BoardService.getBoard();
                $scope.templateId = BoardService.getTemplateId();

                $rootScope.templates = BoardService.getTemplates();

                //console.log("TEMPLATES " + JSON.stringify($rootScope.templates));
                console.log("TEMPLATE ID " + $rootScope.templateId);
                $scope.$apply();
              }, 70);
            };
            refresh();

            $scope.kanbanSortOptions = {
              //restrict move across columns. move only within column.
              /*accept: function (sourceItemHandleScope, destSortableScope) {
               return sourceItemHandleScope.itemScope.sortableScope.$id !== destSortableScope.$id;
               },*/
              itemMoved: function (event) {
                event.source.itemScope.modelValue.status = event.dest.sortableScope.$parent.column.name;
              },
              /*
               Triggered when a card changes position inside the same column. Does not apply on cards
               that move from one column to another
               */
              orderChanged: function (event) {
                //alert("ORDER CHANGED");
              },
              containment: '#board'
            };

            $scope.viewMetadata = function (column) {

              var content = {view: 'views/metadataView.html',
                controller: 'MetadataViewController'};

              console.log("viewing metadata " + column.id);
              BoardService.viewMetadata(column.id)
                      .then(function (response) {

                        DialogService.launch('custom', content)
                                .result.then(function (list) {
                                });
                        //refresh();
                        //DialogService.launch('error');
                      });
            };

            $scope.addNewList = function () {
              BoardService.addNewList($scope.templateId)
                      .then(function (response) {
                        toaster.pop('success', 'Table added', null, '50', 'toast-top-full-width');
                        console.log("table saved successfully");
                        refresh();
                      });
            };

            $scope.removeList = function (column) {
              BoardService.removeList($scope.templateId, column)
                      .then(function (response) {

                        if (!angular.equals(response.response, 'negative')) {
                          console.log("deleting completed");
                          toaster.pop('success', 'Table removed', null, '50', 'toast-top-full-width');
                          refresh();
                        }
                      });
            };

            $scope.removeCard = function (column, card) {
              BoardService.removeCard($scope.templateId, column, card)
                      .then(function (response) {
                        //after delete refresh the view and ignore the response
                        refresh();
                      });
            };

            $scope.addNewCard = function (column) {

              BoardService.addNewCard($scope.templateId, column)
                      .then(function (response) {
                        refresh();
                      });
            };

            /*
             EDITING THE TEXT OF A COMPONENT
             */
            $scope.editCardText = function (card) {
              //alert('editing card : MetadataDesignController column: ' + column.text + " card " + card.title);
              BoardService.editCardText($scope, card);
            };

            $scope.doneEditing = function (card) {
              BoardService.doneEditing($scope, card);
            };

            $scope.editColumnText = function (column) {
              BoardService.editColumnText($scope, column);
            };

            /* MAKE A CARD SEARCHABLE */
            $scope.makeSearchable = function (card) {
              BoardService.makeSearchable($scope, card);
            };

            $scope.makeRequired = function (card) {
              BoardService.makeRequired($scope, card);
            };

            $scope.editSizeField = function (card) {
              BoardService.editSizeField($scope, card);
            };

            $scope.doneEditingSizeField = function (card) {
              BoardService.doneEditingSizeField($scope, card);
            };

            $scope.launch = function (type, message) {
              DialogService.launch(type, message);
            };

            $scope.modifyField = function (column, card) {

              BoardService.modifyField(column, card)
                      .then(function (response) {
                        console.log("modifying done");
                      });
            };

            $scope.submit = function () {

              BoardService.storeTemplate($scope.templateId, $rootScope.mainBoard)
                      .then(function (response) {
                        console.log("Template saved successfully " + response.status);
                        //got the new template back so update the view
                        $rootScope.mainBoard = JSON.parse(response.board);
                        refresh();
                      });
            };
          }]);


/*
 DIRECTIVES ADDED TO HANDLE KEY AND FOCUS EVENTS
 */
//Credit for ngBlur and ngFocus to https://github.com/addyosmani/todomvc/blob/master/architecture-examples/angularjs/js/directives/
mainModule.directive('ngBlur', function () {
  return function (scope, elem, attrs) {
    elem.bind('blur', function () {
      scope.$apply(attrs.ngBlur);
    });
  };
});

mainModule.directive('ngFocus', function ($timeout) {
  return function (scope, elem, attrs) {
    scope.$watch(attrs.ngFocus, function (newval) {
      if (newval) {
        $timeout(function () {
          elem[0].focus();
        }, 0, false);
      }
    });
  };
});

mainModule.directive('ngEnter', function () {
  return function (scope, element, attrs) {
    element.bind("keydown keypress", function (event) {
      if (event.which === 13) {

        scope.$apply(function () {
          scope.$eval(attrs.ngEnter);
        });

        event.preventDefault();
      }
    });
  };
});
