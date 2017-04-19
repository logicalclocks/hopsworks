'use strict';

describe('Service: WorkflowJobService', function () {

  // load the service's module
  beforeEach(module('hopsWorksApp'));

  // instantiate service
  var WorkflowJobService;
  beforeEach(inject(function (_WorkflowJobService_) {
    WorkflowJobService = _WorkflowJobService_;
  }));

  it('should do something', function () {
    expect(!!WorkflowJobService).toBe(true);
  });

});
