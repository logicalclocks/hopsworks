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
      //get the value of the first password
      var firstPwd = '#' + attributes.match; 
      
      element.add(firstPwd).on('keyup', function () {
        scope.$apply(function () {
          console.info(firstPwd);
          console.info(element +" === "+ $(firstPwd));
          console.info(element.val() +" === "+ $(firstPwd).val());
          console.info(element.val() === $(firstPwd).val());
          var v = element.val()=== $(firstPwd).val();
          ctrl.$setValidity('pwmatch', v);
        });
      });
    }
  };
});
