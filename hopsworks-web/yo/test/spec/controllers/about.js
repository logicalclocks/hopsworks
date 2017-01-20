'use strict';

describe('Controller: HomeCtrl', function () {

  // load the controller's module
  beforeEach(module('hopsWorksApp'));

  var AboutCtrl,
          scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    AboutCtrl = $controller('HomeCtrl', {
      $scope: scope
    });
  }));

  it('should attach a list of awesomeThings to the scope', function () {
    expect(scope.somthing).toBe(undefined);
  });
});
