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
 *
 */

'use strict';

angular.module('hopsWorksApp').directive("hwDatePicker", function() {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function(scope, element, attrs, ctrl){

            if(!ctrl) return;

            ctrl.$render = function(){
                if (ctrl.$viewValue && moment(ctrl.$viewValue).isValid()) {
                    element.val( moment(new Date(ctrl.$viewValue)).format(attrs["format"] || 'YYYY-MM-DD') );
                } else {
                    element.val( '' );
                }
            };

            function read() {
                var value = element.val();
                ctrl.$setViewValue(value);
            }

            var options = {
                format: attrs["format"] || 'YYYY-MM-DD',
                minDate: attrs["minDate"] || false,
                maxDate: attrs["maxDate"] || false,
                disabledTimeIntervals:false
            };
            if(element.next().is('.input-group-addon')) {
                var parentElm = $(element).parent();
                parentElm.datetimepicker(options);

                parentElm.on('dp.change', function(){
                    scope.$apply(read);
                });
            } else {
                element.datetimepicker(options);

                element.on('dp.change', function(){
                    scope.$apply(read);
                });
            }

            read();
        }
    };
});