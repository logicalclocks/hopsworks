'use strict';

describe('Controller: WorkflowcreatorCtrl', function () {

  // load the controller's module
  beforeEach(module('hopsWorksApp'));

  var WorkflowcreatorCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    WorkflowcreatorCtrl = $controller('WorkflowcreatorCtrl', {
      $scope: scope
      // place here mocked dependencies
    });
  }));

  it('should attach a list of awesomeThings to the scope', function () {
    expect(WorkflowcreatorCtrl.awesomeThings.length).toBe(3);
  });
});
