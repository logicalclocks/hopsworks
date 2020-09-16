/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
'use strict';

angular.module('hopsWorksApp')
    .controller('DagComposerCtrl', ['$routeParams', 'growl', '$location', 'ModalService', 'AirflowService',
        'JobService',
    function($routeParams, growl, $location, ModalService, AirflowService, JobService) {
        self = this;
        self.projectId = $routeParams.projectID;

        self.jobs;
        self.newDagDefined = false;
        // DAG properties
        self.dag = {
            // Fill it in backend
            projectId: self.projectId,
            name: "",
            // Fill it in backend
            owner: "meb10000",
            scheduleInterval: "@once",
            operators: []
        };
        var getJobsPromise;

        self.showCart = true;
        self.toggleCart = function() {
            self.showCart = !self.showCart;
        }

        self.showAdvanced = false;
        self.toggleAdvanced = function() {
            self.showAdvanced = !self.showAdvanced;
        }

        self.removeOperatorFromDag = function(index) {
            self.dag.operators.splice(index, 1);
        }

        self.returnToFileManager = function() {
            $location.path("project/" + self.projectId + "/airflow");
        }

        self.init = function() {
            self.getJobsPromise = JobService.getJobs(self.projectId, 0, 0, "");
        }

        self.isUndefined = function(input) {
            return typeof input === "undefined";
        }

        // Operators
        // Attributes mask [hasJobName, hasWait2Finish, hasFeatureGroupsName]
        var Operator = function(type, name, description, attributesMask) {
            // Do NOT change the attributes order!
            this.HAS_JOB_NAME_ATTR = 0;
            this.HAS_WAIT_2_FINISH_ATTR = 1;
            this.HAS_FEATURE_GROUPS_NAME_ATTR = 2;
            this.CAN_REQUIRE_JOB_ARGUMENTS = 3

            this.type = type;
            this.name = name;
            this.description = description;
            this.attributesMask = attributesMask;
            this.jobArgs = "";
        }

        Operator.prototype.hasAttribute = function(attribute) {
            if (this.attributesMask.length < attribute || attribute < 0) {
                return false;
            }
            return this.attributesMask[attribute];
        }

        var launchJobOperator = new Operator(0, "HopsworksLaunchOperator",
            "Operator to launch a Job in Hopsworks. Job should already be defined in Jobs UI "
                + "and job name in operator must match the job name in Jobs UI.", [true, true, false, true]);
        
        var jobSensor = new Operator(1, "HopsworksJobSuccessSensor",
            "Operator which waits for the completion of a specific job. Job must be defined "
                + "in Jobs UI and job name in operator must match the job name in Jobs UI. "
                + "The task will fail too if the job which is waiting for fails.", [true, false]);

        var validationResult = new Operator(2, "HopsworksFeatureValidationResult",
            "When this task runs, it will fetch the Data Validation result for a specific feature "
                + "group. It assumes that the data validation Job has run before and it has finished. "
                + "The task will fail if the validation result is not successful.", [false, false, true]);

        self.availableOperators = [launchJobOperator, jobSensor, validationResult];

        self.finishedDagDefinition = function() {
            if (self.isUndefined(self.dag.name) || self.dag.name == "") {
                var errorMsg = "DAG name is required";
            } else if (self.isUndefined(self.dag.scheduleInterval) || self.dag.scheduleInterval == "") {
                var errorMsg = "DAG schedule interval is required";
            }
            if (self.isUndefined(errorMsg)) {
                growl.info("You can continue adding Operators",
                    {title: "Created new DAG definition", ttl: 3000, referenceId: 'dag_comp_growl'});
                self.newDagDefined = true;
            } else {
                growl.error(errorMsg,
                    {title: 'Failed to create new DAG definition', ttl: 5000, referenceId: 'dag_comp_growl'});
                self.newDagDefined = false;
            }
        }

        self.addOperator = function(operator) {
            if (!self.newDagDefined) {
                growl.error("You should define DAG properties before start adding operators",
                    {title: "Failed to add operator", ttl: 5000, referenceId: "dag_comp_growl"});
                return;
            }

            var thisthis = self;
            var newOperator = new Operator(operator.type, operator.name, operator.description, operator.attributesMask);
            if (newOperator.hasAttribute(newOperator.HAS_WAIT_2_FINISH_ATTR)) {
                newOperator.wait = true;
            }

            self.getJobsPromise.then(
                function(success) {
                    if (self.isUndefined(self.jobs)) {
                        var idx = 0;
                        self.jobs = [];
                        success.data.items.forEach(function(value, key) {
                            self.jobs[idx] = value
                            idx++;
                        })
                    }
                    ModalService.addOperator2AirflowDag('lg', newOperator, self.jobs, self.dag.operators).then(
                        function(operator) {
                            thisthis.dag.operators.push(operator);
                            self = thisthis;
                        }, function(error) {
                            self = thisthis;
                        }
                    )
                }, function(error) {
                    growl.error("Could not fetch Project's jobs. Please try again",
                        {title: "Failed to add operator", ttl: 5000, referenceId: "dag_comp_growl"});
                }
            )
        }

        self.generateAirflowDag = function() {
            if (self.dag.operators.length == 0) {
                growl.error("No tasks have been added to workflow",
                    {title: "Could not create workflow", ttl: 5000, referenceId: "dag_comp_growl"});
                return;
            }
            AirflowService.generateDag(self.projectId, self.dag).then(
                function(success) {
                    growl.info("Generated DAG " + self.dag.name,
                        {title: "Success", ttl: 3000, referenceId: "dag_comp_growl"});
                    self.returnToFileManager();
                }, function(error) {
                    growl.error(error.data.usrMsg,
                        {title: "Could not generate DAG file", ttl: 5000, referenceId: "dag_comp_growl"});
                }
            )
        }
        self.init();
    }
]);