'use strict';

describe('Controller: WorkflowexecutionCtrl', function () {

  // load the controller's module
  beforeEach(module('hopsWorksApp'));

  var WorkflowexecutionCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    WorkflowexecutionCtrl = $controller('WorkflowexecutionCtrl', {
      $scope: scope
      // place here mocked dependencies
    });
  }));

  it('should attach a list of awesomeThings to the scope', function () {
    expect(WorkflowexecutionCtrl.awesomeThings.length).toBe(3);
  });
});
