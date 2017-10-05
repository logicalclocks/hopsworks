

angular.module('hopsWorksApp')
        .controller('SetupDownloadCtrl', ['$uibModalInstance', 'DataSetService', 'KafkaService', 'DelaProjectService', 'growl', 'defaultDatasetName', 'projectId', 'datasetId', 'bootstrap', 'ModalService',
            function ($uibModalInstance, DataSetService, KafkaService, DelaProjectService, growl, defaultDatasetName, projectId, datasetId, bootstrap, ModalService) {

                var self = this;
                self.projectId = projectId;
                self.datasetId = datasetId;
                self.bootstrap = bootstrap;

                self.datasetDestination = defaultDatasetName;

                var dataSetService = DataSetService(self.projectId);
                self.delaService = new DelaProjectService(self.projectId);

                self.manifestAvailable = false;
                self.manifest;

                self.DownloadTypeKafka = false;
                self.typeChosen = false;

                self.topicValues = [];
                self.topicDone = [];
                self.showCheck = [];
                self.showCreate = [];
                self.showDownload = false;
                self.topicsRemainingForCreation = 0;
                self.limit = 5;

                self.topicsMap = {};


                self.initTopic = function (index, fileName) {
                    self.topicValues[index] = self.projectId + '_' + self.datasetDestination + '_' + fileName;
                    self.topicsRemainingForCreation++;
                    self.showCreate[index] = true;
                };
                
                self.validTopicName = function (fileName, topicName, schema) {
                    var topics = [];
                    KafkaService.getTopics(self.projectId).then(function (success) {
                        topics = success.data;
                        topics.forEach(function (topic, index) {
                          if (topicName === topic.name) {
                            KafkaService.getSchemaContent(self.projectId, topic.schemaName, topic.schemaVersion)
                              .then(function (success) {
                                console.log("Schema Content: ", success.data.contents);
                                console.log("Schema: ", schema);
                                console.log("Schema Content === Schema: ", schema === success.data.contents);
                                if(schema === success.data.contents) {
                                  self.topicDone[index] = true;
                                  self.topicsMap[fileName] = topicName;
                                  self.showCreate[index] = false;
                                  self.topicsRemainingForCreation--;
                                  if (self.topicsRemainingForCreation === 0) {
                                    self.showDownload = true;
                                  }
                                }
                              }, function (error) {
                                growl.error(error.data.errorMsg, {title: 'Failed to get Schema Content', ttl: 5000});
                              });
                            return false;
                          }
                          
                        });
                    }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Failed to get Topics', ttl: 5000});
                    });
                    return true;
                };

                self.createTopic = function (topicName, schema, fileName, index) {

                    var schemaDetail = {};
                    schemaDetail.name = topicName;
                    schemaDetail.contents = schema;
                    schemaDetail.version = 1;
                    schemaDetail.versions = [];

                    KafkaService.createSchema(self.projectId, schemaDetail).then(
                      function (success) {
                          var topicDetails = {};
                          topicDetails.name = topicName;
                          topicDetails.numOfPartitions = 2;
                          topicDetails.numOfReplicas = 1;
                          topicDetails.schemaName = topicName;
                          topicDetails.schemaVersion = 1;
                          KafkaService.createTopic(self.projectId, topicDetails).then(
                            function (success) {
                                self.topicsRemainingForCreation--;
                                self.topicDone[index] = true;
                                self.topicsMap[fileName] = topicName;
                                if (self.topicsRemainingForCreation === 0) {
                                    self.showDownload = true;
                                }
                            }, function (error) {
                                growl.error(error.data.errorMsg, {title: 'Failed to create topic', ttl: 5000});
                          });
                      }, function (error) {
                         growl.error(error.data.errorMsg, {title: 'Failed to create schema', ttl: 5000});
                    });
                };

                self.myFilter = function (item) {

                    return item.schema !== '';

                };

                self.isNameOk = function () {

                    dataSetService.getAllDatasets().then(function (success) {

                        var data = success.data;
                        for (var i = 0; i < data.length; i++) {
                            if (data[i].name === self.datasetDestination) {
                                self.datasetNameOk = false;
                                return;
                            }
                        }

                        self.datasetNameOk = true;

                    }, function (error) {

                    });

                };

                self.datasetNameOk = self.isNameOk();


                self.DownloadRequest = function () {

                    var json = {};
                    json.projectId = self.projectId;
                    json.name = self.datasetDestination;
                    json.publicDSId = self.datasetId;
                    json.bootstrap = self.bootstrap;
                    self.delaService.downloadMetadata(self.datasetId, json).then(function (success) {
                        self.manifest = success.data;
                        self.manifestAvailable = true;
                        },
                        function (error) {
                          $uibModalInstance.close(error);
                          growl.error(error.data.details, {title: 'Failed to start initiate Download', ttl: 5000});
                        });
                };

                self.downloadTypeHdfs = function () {
                    self.DownloadTypeKafka = false;
                    self.typeChosen = true;
                };

                self.downloadTypeKafkaHdfs = function () {
                    self.DownloadTypeKafka = true;
                    self.typeChosen = true;
                };

                self.download = function () {

                    var json = {};
                        json.projectId = self.projectId;
                        json.name = self.datasetDestination;
                        json.publicDSId = self.datasetId;
                        json.bootstrap = self.bootstrap;
                        
                        for(var i = 0;i<self.manifest.fileInfos.length;i++){
                            var keyName = self.manifest.fileInfos[i].fileName;
                            self.topicsMap[keyName] = "";
                        }
                        
                        json.topics = JSON.stringify(self.topicsMap);
                        
                    if (!self.DownloadTypeKafka) {
                        self.delaService.downloadHdfs(self.datasetId, json).then(function (success) {
                            growl.success(success.data.details, {title: 'Success', ttl: 1000});
                            $uibModalInstance.close(success);
                        }, function (error) {
                            growl.error(error.data.details, {title: 'Failed to start download', ttl: 5000});
                        });

                    } else {
                        self.delaService.downloadKafka(self.datasetId, json).then(function (success) {
                            growl.success(success.data.details, {title: 'Success', ttl: 1000});
                            $uibModalInstance.close(success);
                        }, function (error) {
                            growl.error(error.data.details, {title: 'Failed to start download', ttl: 5000});
                        });
                    }

                };

                self.showSchema = function (schema) {

                    ModalService.json('md', 'Schema', schema).then(function (success) {

                    });

                };
                
                self.showMore = function(){
                    
                    self.limit = self.limit + 5;
                };

            }]);