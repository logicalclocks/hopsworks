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

var directives = angular.module("directives", []);

directives.directive('pwCheck', function () {
  return {
    require: 'ngModel',
    link: function (scope, element, attributes, ctrl) {
      if (!ctrl) {
        if (console && console.warn) {
          console.warn('Match validation requires ngModel to be on the element');
        }
        return;
      }
      //get the value of the first password
      var firstPwd = '#' + attributes.pwCheck; 
      var secondPwd = '#' + attributes.id;
      var otherElement = $(firstPwd);
      var thisElement = $(secondPwd);
      var setCustomValidation = function (elem, text) {
        if (elem.get(0) && elem.get(0).setCustomValidity) { // HTML5 validation
          elem.get(0).setCustomValidity(text);
        } else {
          ctrl.$setValidity('pwmatch', text === '');
        }
      };
      element.on('keyup', function () {
        scope.$apply(function () {
          var v = element.val() === otherElement.val();
          if (!v) { 
            setCustomValidation(thisElement, "Passwords must match");
          } else {
            setCustomValidation(thisElement, "");
          }
        });
      });
    }
  };
});


