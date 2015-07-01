/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('metaUI').controller('ListController', ['$scope', 'BoardService', function ($scope, BoardService) {

        if ($scope.showing && $scope.counter <= 1) {
            //alert("this showing is " + $scope.showing + " counter " + $scope.counter);
            $scope.counter = $scope.counter + 1;

            BoardService.createNewList();
        }
    }]);