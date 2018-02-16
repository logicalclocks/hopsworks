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


