/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

angular.module('hopsWorksApp').directive('showHidePassword', ['$compile', function ($compile) {
    return {
        restrict: 'A',
        scope: true, //Isolating scope
        link: function (scope, element) {
            var template = '<span class="{{icon}} form-control-feedback text-muted" title="{{tooltip}}"' +
            ' ng-click="toggle()" style="z-index: 1000; pointer-events: auto; cursor: pointer;"></span>';
            scope.icon = "fa fa-eye";
            scope.tooltip = "Show";
            var toggle = true;
            scope.toggle = function () {
                toggle = !toggle;
                element[0].type = element[0].type === 'text' ? 'password' : 'text';
                if (toggle) {
                    scope.icon = "fa fa-eye";
                    scope.tooltip = "Show";
                } else {
                    scope.icon = "fa fa-eye-slash";
                    scope.tooltip = "Hide";
                }
            }
            element.after($compile(angular.element(template))(scope));
        }
    }
}]);