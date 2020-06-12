/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('hopsWorksApp')
    .controller('HwSearchCtrl', ['$scope', '$location',
        function($scope, $location) {
            var self = this;
            self.activeSuggestion = 0;
            self.searchTerm = $location.search()['q'];
            self.notSent = typeof self.searchTerm === 'undefined';
            self.searchTermSent = "";

            self.select = function (scope) {
                self.notSent = false;
                self.searchTermSent = self.searchTerm;
                $scope.clickFn({'scope': scope, 'searchTerm': self.searchTerm});
            };

            self.keyUp = function (event) {
                var code = event.which || event.keyCode || event.charCode;
                if (angular.equals(code, 13) && !$scope.searching) {
                    self.select($scope.scopes[self.activeSuggestion]);
                } else if (angular.equals(code, 27)) {
                    $scope.searchTerm = "";
                    self.searchTerm = "";
                }
                self.notSent = self.searchTermSent !== self.searchTerm;
            };

            self.keydown = function (event) {
                var code = event.which || event.keyCode || event.charCode;
                switch (code) {
                    case 38:
                        self.activeSuggestion > 0 && self.activeSuggestion--;
                        break;
                    case 40:
                        self.activeSuggestion < $scope.scopes.length - 1 && self.activeSuggestion++;
                        break;
                }
            };

            self.openDropdown = function () {
                return $scope.searchTerm.length > 0  && !$scope.searching;
            }

    }]);