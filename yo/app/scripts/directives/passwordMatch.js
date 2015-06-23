'use strict';

angular.module('hopsWorksApp').directive('match', function () {
  return {
    require: 'ngModel',
    restrict: 'A',
    link: function (scope, element, attributes, ctrl) {
      if (!ctrl) {
        if (console && console.warn) {
          console.warn('Match validation requires ngModel to be on the element');
        }
        return;
      }
      var checker = function () {

        //get the value of the first password
        var p1 = scope.$eval(attributes.ngModel);

        //get the value of the 2nd password
        var p2 = scope.$eval(attributes.match);

        return p1 === p2;
      };


      scope.$watch(checker, function (n) {
        ctrl.$setValidity("unique", n);
      });
    }
  };
});
