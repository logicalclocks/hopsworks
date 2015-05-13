/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('metaUI')
        .controller('ListController', ['$scope', 'BoardService', function ($scope, BoardService) {

//    var refresh = function(){
//        setTimeout( function(){
//            $scope.mainBoard = BoardService.getBoard();
//            $scope.$apply();
//        }, 70 );
//    }
//    refresh();

                if ($scope.showing && $scope.counter <= 1) {
                    //alert("this showing is " + $scope.showing + " counter " + $scope.counter);
                    $scope.counter = $scope.counter + 1;

                    BoardService.addNewList()
                            .then(function (response) {
                                console.log("list saved successfully");
//        refresh();
                            });
                }
            }]);