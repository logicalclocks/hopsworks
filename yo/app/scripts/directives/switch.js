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
                    scope.$apply(function() {
                        ctrl.$setViewValue(state);
                    });

                });
        }
    }
});
