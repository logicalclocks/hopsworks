'use strict';

describe('Service: WorkflowService', function () {

  // load the service's module
  beforeEach(module('hopsWorksApp'));

  // instantiate service
  var WorkflowService;
  beforeEach(inject(function (_WorkflowService_) {
    WorkflowService = _WorkflowService_;
  }));

  it('should do something', function () {
    expect(!!WorkflowService).toBe(true);
  });

});
