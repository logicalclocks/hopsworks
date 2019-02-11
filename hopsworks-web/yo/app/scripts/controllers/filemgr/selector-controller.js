/* 
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 joni2back
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
(function(angular) {
    'use strict';
    angular.module('hopsWorksApp').controller('ModalFileManagerCtrl', 
        ['$scope', '$rootScope', 'fileNavigator', function($scope, $rootScope, FileNavigator) {

        $scope.reverse = false;
        $scope.predicate = ['model.type', 'model.name'];
        $scope.fileNavigator = new FileNavigator();
        $rootScope.selectedModalPath = [];

        $scope.order = function(predicate) {
            $scope.reverse = ($scope.predicate[1] === predicate) ? !$scope.reverse : false;
            $scope.predicate[1] = predicate;
        };

        $scope.select = function(item) {
            $rootScope.selectedModalPath = item.model.fullPath().split('/').filter(Boolean);
            $scope.modal('selector', true);
        };

        $scope.selectCurrent = function() {
            $rootScope.selectedModalPath = $scope.fileNavigator.currentPath;
            $scope.modal('selector', true);
        };

        $scope.selectedFilesAreChildOfPath = function(item) {
            var path = item.model.fullPath();
            return $scope.temps.find(function(item) {
                var itemPath = item.model.fullPath();
                if (path == itemPath) {
                    return true;
                }
                /*
                if (path.startsWith(itemPath)) {
                    fixme names in same folder like folder-one and folder-one-two
                    at the moment fixed hidding affected folders
                }
                */
            });
        };

        $rootScope.openNavigator = function(path) {
            $scope.fileNavigator.currentPath = path;
            $scope.fileNavigator.refresh();
            $scope.modal('selector');
        };

        $rootScope.getSelectedPath = function() {
            var path = $rootScope.selectedModalPath.filter(Boolean);
            var result = '/' + path.join('/');
            if ($scope.singleSelection() && !$scope.singleSelection().isFolder()) {
                result += '/' + $scope.singleSelection().tempModel.name;
            }
            return result.replace(/\/\//, '/');
        };

    }]);
})(angular);
