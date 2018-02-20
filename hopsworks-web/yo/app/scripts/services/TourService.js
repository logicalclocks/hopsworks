/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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

                tourService.getKafkaGuideJarPath = function (projectName, filename) {
                  return "hdfs:///Projects/" + projectName +
                   "/TestJob/" + filename;
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
