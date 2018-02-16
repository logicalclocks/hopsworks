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
