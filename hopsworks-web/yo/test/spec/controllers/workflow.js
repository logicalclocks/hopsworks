'use strict';

describe('Controller: WorkflowCtrl', function () {

  // load the controller's module
  beforeEach(module('hopsWorksApp'));

  var WorkflowCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    WorkflowCtrl = $controller('WorkflowCtrl', {
      $scope: scope
      // place here mocked dependencies
    });
  }));

  it('should attach a list of awesomeThings to the scope', function () {
    expect(WorkflowCtrl.awesomeThings.length).toBe(3);
  });
});
