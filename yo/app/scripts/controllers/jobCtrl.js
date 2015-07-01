/*
 * This file defines some general JobController functions. Each function takes 
 * as a parameter the controller that invokes it.
 */

/**
 * Get all job history objects for the given type.
 * @returns {undefined}
 */
var getHistory = function (controller) {
  controller.JobHistoryService.getByProjectAndType(controller.projectId, controller.jobType)
          .then(function (success) {
            controller.jobs = success.data;
          }, function (error) {
            controller.growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
            controller.jobs = null;
          });
};

/**
 * Open a modal dialog to select a file.
 * @returns {undefined}
 */
var selectFile = function (controller) {
  controller.ModalService.selectFile('lg', controller.selectFileRegex,
          controller.selectFileErrorMsg).then(
          function (success) {
            controller.onFileSelected(success);
          }, function (error) {
    //The user changed their mind.
  });
};

/**
 * Execute the job.
 * @returns {undefined}
 */
var execute = function (controller) {
  //First: stop polling for previous jobs.
  controller.$interval.cancel(controller.poller);
  //Then: submit job.
  controller.callExecute().then(
          function (success) {
            //get the resulting job
            controller.job = success.data;
            //Add it to the history array
            controller.jobs.unshift(controller.job);
            //Call any job-specific operations
            controller.onExecuteSuccess(success);
            //Start polling
            controller.poller = controller.$interval(function () {
              pollStatus(controller)
            }, 3000);
          }, function (error) {
    controller.growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
  });
};

/**
 * Polls for the current status of the selected job.
 * @returns {undefined}
 */
var pollStatus = function (controller) {
  controller.JobHistoryService.pollStatus(controller.projectId, controller.job.id).then(
          function (success) {
            var oldindex = controller.jobs.indexOf(controller.job);
            controller.job = success.data;
            //Replace the old element in the jobs array
            if (oldindex !== -1) {
              controller.jobs[oldindex] = controller.job;
            }
            //check if job finished
            if (controller.job.state == 'FINISHED'
                    || controller.job.state == 'FAILED'
                    || controller.job.state == 'KILLED'
                    || controller.job.state == 'FRAMEWORK_FAILURE'
                    || controller.job.state == 'APP_MASTER_START_FAILED') {
              //if so: stop executing
              controller.$interval.cancel(controller.poller);
            }
          }, function (error) {
    controller.$interval.cancel(controller.poller);
    controller.growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
  });
};


/**
 * Select a job to start polling for its status.
 * @param {type} job
 * @returns {undefined}
 */
var selectJob = function (controller, job) {
  //Stop polling
  controller.$interval.cancel(controller.poller);
  //Set self.job
  controller.job = job;
  //check if need to start polling
  if (controller.job.state !== 'FINISHED'
          && controller.job.state !== 'FAILED'
          && controller.job.state !== 'KILLED'
          && controller.job.state !== 'FRAMEWORK_FAILURE'
          && controller.job.state !== 'APP_MASTER_START_FAILED') {
    //if so: start executing
    controller.poller = controller.$interval(function () {
      pollStatus(controller);
    }, 3000);
  }
};

