/**
 * Created by AMore on 2015-04-24.
 */

'use strict';


angular.module('hopsWorksApp')
        .controller('DatasetsCtrl', ['$rootScope', '$modal', '$scope', '$q', '$timeout', '$mdSidenav', '$mdUtil', '$log', 'WSComm',
            'DataSetService', '$routeParams', 'ModalService', 'growl', 'ProjectService', 'MetadataActionService',
            function ($rootScope, $modal, $scope, $q, $timeout, $mdSidenav, $mdUtil, $log, WSComm, DataSetService, $routeParams,
            ModalService, growl, ProjectService, MetadataActionService) {

                var self = this;

                self.datasets = [];
                self.currentDataSet = "";
                self.currentProject = "";

                self.dataSet = {};
                var file = {name: "", owner: 'André', modified: "", filesize: '4 GB', path: "", dir: ""};
                self.files = [file];
                var pId = $routeParams.projectID;
                var currentDS = $routeParams.datasetName;
                var dataSetService = DataSetService(pId);
                //this can be removed if we use project name instead of project id
                ProjectService.get({}, {'id': pId}).$promise.then(
                        function (success) {
                            console.log(success);
                            self.currentProject = success;
                        }, function (error) {
                    $location.path('/');
                });
                if (currentDS) {
                    self.currentDataSet = currentDS;
                    
                }

                var getAll = function () {
                    dataSetService.getAll().then(
                            function (success) {
                                self.datasets = success.data;
                                console.log(success);
                            }, function (error) {
                        console.log("getAll error");
                        console.log(error);
                    });
                };
                var getDir = function (name) {
                    dataSetService.getDir(name).then(
                            function (success) {
                                self.files = success.data;
                                console.log(success);
                            }, function (error) {
                        console.log("getDir error");
                        console.log(error);
                    });
                };
                var download = function (file) {
                    dataSetService.download(file).then(
                            function (success) {
                                console.log("download success");
                                console.log(success);
                            }, function (error) {
                        console.log("download error");
                        console.log(error);
                    });
                };
                var upload = function (path) {
                    dataSetService.upload(path).then(
                            function (success) {
                                console.log("upload success");
                                console.log(success);
                            }, function (error) {
                        console.log("upload error");
                        console.log(error);
                    });
                };

                var removeDataSetDir = function (path) {
                    dataSetService.removeDataSetDir(path).then(
                            function (success) {
                                growl.success(success.data.successMessage, {title: 'Success', ttl: 15000});
                                load();
                            }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                    });
                };
                //if in dataset browser show current dataset content
                //else show datasets in project
                var load = function () {
                    if (currentDS) {
                        getDir(currentDS);
                    } else {
                        getAll();
                    }
                };
                load();

                self.newDataSetModal = function () {

                    ModalService.newDataSet('md', currentDS).then(
                            function (success) {
                                growl.success(success.data.successMessage, {title: 'Success', ttl: 15000});
                                load();
                            }, function (error) {
                        growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
                    });
                };

                self.deleteFile = function (fileName) {
                    if (currentDS) {
                        removeDataSetDir(encodeURIComponent(currentDS + "/" + fileName));
                    } else {
                        removeDataSetDir(fileName);
                    }
                };


                /* Metadata designer */

                self.toggleLeft = buildToggler('left');
                self.toggleRight = buildToggler('right');

                function buildToggler(navID) {
                    var debounceFn = $mdUtil.debounce(function () {
                        $mdSidenav(navID)
                                .toggle()
                                .then(function () {
                                    self.getAllTemplates();
                                });
                    }, 300);
                    return debounceFn;
                }
                ;

                self.close = function () {
                    $mdSidenav('right').close()
                            .then(function () {
                                $log.debug("Closed metadata designer");
                            });
                };

                self.availableTemplates = [];
                self.newTemplateName = "";
                $scope.extendedFrom = {};

                self.extendedFromBoard = {};

                self.currentTemplateID = "";
                self.currentBoard = {};
                self.editedField = null;


                $scope.$watch('extendedFrom', function (newID) {
                    if (typeof newID === "string") {
                        self.selectChanged(newID);
                    }
                });
                
                self.getAllTemplates = function () {
                    MetadataActionService.fetchTemplates()
                       .then(function (data) {
                            self.availableTemplates = JSON.parse(data.board).templates;
                        }
                    );
                };

                self.addNewTemplate = function () {
                    MetadataActionService.addNewTemplate(self.newTemplateName)
                        .then(function (data) {
                            self.newTemplateName = "";
                            self.getAllTemplates();
                            console.log(data);
                        }
                    );
                };

                self.removeTemplate = function (templateId) {
                    MetadataActionService.removeTemplate(templateId)
                        .then(function (data) {
                            self.getAllTemplates();
                            console.log(data);
                        }
                    );
                };

                self.selectChanged = function (extendFromThisID) {
                    console.log('selectChanged - start: ' + extendFromThisID);
                    
                    MetadataActionService.fetchTemplate(parseInt(extendFromThisID))
                        .then(function (success) {
                            console.log('Fetched data - success.board.column:');
                            self.extendedFromBoard = JSON.parse(success.board);
                            console.log(self.extendedFromBoard);

                            console.log('Fetched data - success:');
                            console.log(success);

                        }, function (error) {
                        console.log('Fetched data - error:');
                        console.log(error);
                    });
                };

                self.extendTemplate = function () {
                    MetadataActionService.addNewTemplate(self.newTemplateName)
                        .then(function (data) {
                            var tempTemplates = JSON.parse(data.board);
                            var newlyCreatedID = tempTemplates.templates[tempTemplates.numberOfTemplates - 1].id;
                            console.log('add_new_templatE');
                            console.log(data);

                            console.log('Sent message: ');
                            console.log(self.extendedFromBoard);

                            MetadataActionService.extendTemplate(newlyCreatedID, self.extendedFromBoard)
                                .then(function (data) {
                                    self.newTemplateName = "";
                                    self.getAllTemplates();

                                    console.log('Response from extending template: ');
                                    console.log(data);
                                }
                            );
                        }
                    );
                };

                self.fetchTemplate = function (templateId) {
                    self.currentTemplateID = templateId;
                    
                    MetadataActionService.fetchTemplate(templateId)
                        .then(function (success) {
                            console.log('fetchTemplate - success');
                            self.currentBoard = JSON.parse(success.board);
                            console.log(self.currentBoard);
                        }, function (error) {
                            console.log('fetchTemplate - error');
                            console.log(JSON.parse(error));
                        }
                    );
                };

                self.storeTemplate = function () {
                                        
                    MetadataActionService.storeTemplate(self.currentTemplateID, self.currentBoard)
                        .then(function(response){
                           console.log("TEMPLATE SAVED SUCCESSFULLY " + JSON.stringify(response));
                           self.currentBoard = JSON.parse(response.board);
                           self.close();
                        }, function (error) {
                            console.log(error);
                        });
                };

                self.deleteList = function (column) {
                    MetadataActionService.deleteList(self.currentTemplateID, column)
                        .then(function (success) {
                            console.log(success);
                            self.fetchTemplate(self.currentTemplateID);
                        }, function (error) {
                            console.log(error);
                        }
                    );
                };

                self.storeCard = function (templateId, column, card) {
                    
                    return MetadataActionService.storeCard(templateId, column, card);
                };

                self.addCard = function (column) {
                    $scope.currentColumn = column;

                    var modalInstance = $modal.open({
                        templateUrl: 'views/metadata/newCardModal.html',
                        controller: 'NewCardCtrl',
                        scope: $scope
                    }).result.then(
                            function (card) {

                                console.log('Created card, ready to send:');
                                console.log(card);

                                MetadataActionService.storeCard(column, card, self.currentTemplateID)
                                    .then(function (success) {
                                        console.log(success);
                                        self.fetchTemplate(self.currentTemplateID);
                                    }, function (error) {
                                        console.log(error);
                                    });

                            }, function (error) {
                        console.log(error);
                    });
                };


                self.deleteCard = function (column, card) {
                    MetadataActionService.deleteCard(self.currentTemplatID, column, card)
                        .then(function (success) {
                            console.log(success);
                            self.fetchTemplate(self.currentTemplateID);
                        }, function (error) {
                            console.log(error);
                        });
                };

                self.addNewList = function () {
                    $scope.template = self.currentTemplateID;

                    $modal.open({
                        templateUrl: 'views/metadata/newListModal.html',
                        controller: 'NewlistCtrl',
                        scope: $scope
                    })
                    .result.then(function (list) {

                        if (!angular.isUndefined(list)) {

                            //{tempid:2,bd:{name:MainBoard,numberOfColumns:3,columns:[{id:-1,name:newTable,cards:[]}],backlogs:[]}}}

                            //we need to add the new table into the mainboard object
                            self.currentBoard.columns.push(list);
                            console.log("CURRENT LIST AFTER TABLE ADDITION " + JSON.stringify(self.currentBoard));

                            MetadataActionService.storeTemplate()
                                .then(function (response) {
                                    console.log("TEMPLATE STORED SUCCESSFULLY " + JSON.stringify(response));
                                    self.currentBoard = JSON.parse(response.board);
                                    //defer.resolve($rootScope.mainBoard);
                                }, function (error) {
                                    console.log(error);
                                });
                        }
                    });
                };

                
                /* TESTING RECEIVE BROADCAST FROM WEBSOCKET SERVICE */
                $rootScope.$on('andreTesting', function (event, data) {
                    console.log('BroadcastReceived BOARD:');
                    console.log(JSON.parse(data.response.board));
                    //self.getAllTemplates();
                });

                /* CARD MANIPULATION FUNCTIONS */
                self.makeSearchable = function (card) {
                    
                    card.find = !card.find;
                    console.log("Card " + card.title + " became searchable " + card.find);
                };

                self.makeRequired = function (card) {
                    card.required = !card.required;
                    console.log("Card " + card.title + " became required " + card.required);
                };

                self.editSizeField = function (card) {

                    card.sizefield.showing = !card.sizefield.showing;
                    console.log("Card " + card.title + " showing " + card.sizefield.showing + " max size " + card.sizefield.value);

                    self.editedField = card;
                };

                self.doneEditingSizeField = function (card) {

                    card.sizefield.showing = false;
                    self.editedField = null;
                };
                
                self.modifyField = function (column, field) {
                    console.log("SAVING " + JSON.stringify(column));
                    $scope.tableid = column.id;
                    $scope.field = field;
                    
                    var defer = $q.defer();

                        //necessary data to modify the field definition
                        //data: {table: column.id, field: field}};

                        $modal.open({
                            templateUrl: 'views/partials/modifyFieldDialog.html',
                            controller: 'ModifyFieldCtrl',
                            scope: $scope
                        })
                        .result.then(function (dialogResponse) {

                            //PERSIST THE CARD TO THE DATABASE - dialogResponse is the modified field
                            self.storeCard(self.currentTemplateID, column, dialogResponse)
                                    .then(function (response) {
                                        $rootScope.mainBoard = JSON.parse(response.board);

                                        defer.resolve($rootScope.mainBoard);
                                        $rootScope.$broadcast('refreshApp', $rootScope.mainBoard);
                                    });
                        }, function (dialogResponse) {
                            console.log("don't modify " + JSON.stringify(dialogResponse));
                            //hand off the control back to the caller 
                        });

                        return defer.promise;
                    };
            }]);
        

/*

 
 deleteList: function (templateId, column) {
 return WSComm.send({
 sender: 'evsav',
 type: 'TablesMessage',
 action: 'delete_table',
 message: JSON.stringify({
 tempid: templateId,
 id: column.id,
 name: column.name,
 forceDelete: column.forceDelete
 })
 });
 },
  
 
 storeMetadata: function(data){
 return WSComm.send({
 sender: 'evsav',
 type: 'MetadataMessage',
 action: 'store_metadata',
 message: JSON.stringify(data)
 });
 },
 
 
 */
