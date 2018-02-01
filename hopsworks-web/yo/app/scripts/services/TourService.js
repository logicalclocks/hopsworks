/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('hopsWorksApp')
        .factory('TourService', ['$timeout',
            function ($timeout) {

                var tourService = this;

                tourService.kafkaSchemaName = "DemoAvroSchema";
                tourService.kafkaTopicName = "DemoKafkaTopic";
                tourService.kafkaProjectPrefix = "demo_kafka";
                tourService.sparkProjectPrefix = "demo_spark";
                tourService.tensorflowProjectPrefix = "demo_tensorflow";
                tourService.distributedtensorflowProjectPrefix = "demo_distributed_tensorflow";
                tourService.tensorflowRunTip = "";
                tourService.informAndTips = true;
                tourService.tipsOnly = false;
                tourService.informOnly = false;
                tourService.showNothing = false;

                tourService.toursInfoStep = 0;
                tourService.currentStep_TourOne = -1;
                tourService.currentStep_TourTwo = -1;
                tourService.currentStep_TourThree = -1;
                tourService.currentStep_TourFour = -1;
                tourService.currentStep_TourFive = -1;
                tourService.currentStep_TourSix = -1;
                tourService.currentStep_TourSeven = -1;
                tourService.alive_TourOne = 15;
                tourService.createdJobName = null;
                tourService.activeTour = null;
                tourService.kafkaJobCreationState = "producer";
                tourService.counter = 0;

                tourService.getTensorFlowJobTip = function(){
                  if(tourService.activeTour === "tensorflow"){
                    tourService.tensorflowRunTip = "An inference job was created for you! Go on and run it after the model is finished.";
                  }
                  return tourService.tensorflowRunTip;
                };

                tourService.setActiveTour = function (tourName) {
                    tourService.activeTour = tourName;
                };

                tourService.setInformAndTipsState = function () {
                  tourService.informAndTips = true;
                  tourService.tipsOnly = false;
                  tourService.informOnly = false;
                  tourService.showNothing = false;
                };

                tourService.setTipsOnlyState = function () {
                  tourService.informAndTips = false;
                  tourService.tipsOnly = true;
                  tourService.informOnly = false;
                  tourService.showNothing = false;
                };

                tourService.setInformOnly = function () {
                  tourService.informAndTips = false;
                  tourService.tipsOnly = false;
                  tourService.informOnly = true;
                  tourService.showNothing = false;
                };

                tourService.setShowNothingState = function () {
                  tourService.informAndTips = false;
                  tourService.tipsOnly = false;
                  tourService.informOnly = false;
                  tourService.showNothing = true;
                };
                
                tourService.setDefaultTourState = function () {
                  tourService.informAndTips = true;
                  tourService.tipsOnly = false;
                  tourService.informOnly = false;
                  tourService.showNothing = false;
                };

                tourService.printDebug = function () {
                  console.log("Counter: " + tourService.counter);
                  tourService.counter++;
                  console.log(">> kafka state: " + tourService
                  .kafkaJobCreationState);
                  console.log(">> TourSix: " + tourService.currentStep_TourSix);
                  console.log(">> TourSeven: " + tourService
                  .currentStep_TourSeven);

                  return true;
                };

                tourService.getKafkaGuideJarPath = function (projectName) {
                  return "hdfs:///Projects/" + projectName +
                   "/TestJob/hops-spark.jar";
                };

                tourService.resetTours = function () {

                    tourService.toursInfoStep = 0;
                    tourService.currentStep_TourOne = -1;
                    tourService.alive_TourOne = 15;
                    tourService.currentStep_TourTwo = -1;
                    tourService.currentStep_TourThree = -1;
                    tourService.currentStep_TourFour = -1;
                    tourService.currentStep_TourFive = -1;
                    tourService.currentStep_TourSix = -1;
                    tourService.currentStep_TourSeven = -1;
                    tourService.createdJobName = null;
                };

                tourService.KillTourOneSoon = function ()
                {
                    $timeout(function () {
                        tourService.alive_TourOne--;
                    }, 1000).then(function () {
                        if (tourService.alive_TourOne === 0)
                        {
                            tourService.currentStep_TourOne = -1;
                            tourService.alive_TourOne = 15;
                        } else
                        {
                            tourService.KillTourOneSoon();
                        }
                    });
                };
                return tourService;
            }
        ]);
