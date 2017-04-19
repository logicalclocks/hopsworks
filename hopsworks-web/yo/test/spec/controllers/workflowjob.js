'use strict';

describe('Controller: WorkflowjobCtrl', function () {

  // load the controller's module
  beforeEach(module('hopsWorksApp'));

  var WorkflowjobCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    WorkflowjobCtrl = $controller('WorkflowjobCtrl', {
      $scope: scope
      // place here mocked dependencies
    });
  }));

  it('should attach a list of awesomeThings to the scope', function () {
    expect(WorkflowjobCtrl.awesomeThings.length).toBe(3);
  });
});
