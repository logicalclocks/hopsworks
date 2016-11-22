'use strict';

angular.module('hopsWorksApp').directive('hwCard' , function() {
   return {
    restrict: 'E',
    scope: {
      content: '=',
      detailsFn: '&'
    },
    templateUrl:'views/card.html'
  };
});


