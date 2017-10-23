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


