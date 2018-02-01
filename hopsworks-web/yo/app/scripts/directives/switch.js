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

'use strict';
/**
 * This directive can be used to update ngModel value on a switch.
 * after creating the switch element add 'hopsworks-switch' as an attribute.
 * ng-model is required.
 */
angular.module('hopsWorksApp').directive('hopsworksSwitch', function() {
    return {
        require: 'ngModel',
        restrict: 'A',
        link: function(scope, element, attributes, ctrl) {
            if (!ctrl) {
                if (console && console.warn) {
                    console.warn('hopsworks Switch requires ngModel to be on the element');
                }
                return;
            }
            var model = scope.$eval(attributes.ngModel);
            angular.element(element)
                .bootstrapSwitch('state', model)
                .on('switchChange.bootstrapSwitch', function(event, state) {
                    if(!scope.$$phase) {// check if $digest is already in progress
                        scope.$apply(function () {
                            ctrl.$setViewValue(state);
                        });
                    }

                });
            // When the model changes
            scope.$watch(attributes.ngModel, function (newValue) {
                if (newValue !== undefined) {
                    element.bootstrapSwitch('state', newValue);
                }
            }, true);
        }
    };
});
