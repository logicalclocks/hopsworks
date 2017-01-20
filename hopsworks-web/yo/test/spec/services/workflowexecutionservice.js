'use strict';

describe('Service: WorkflowExecutionService', function () {

  // load the service's module
  beforeEach(module('hopsWorksApp'));

  // instantiate service
  var WorkflowExecutionService;
  beforeEach(inject(function (_WorkflowExecutionService_) {
    WorkflowExecutionService = _WorkflowExecutionService_;
  }));

  it('should do something', function () {
    expect(!!WorkflowExecutionService).toBe(true);
  });

});
