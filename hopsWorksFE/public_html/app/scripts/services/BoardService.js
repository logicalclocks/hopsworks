/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('metaUI').service('BoardService',
        ['$rootScope', '$location', 'DialogService', 'BoardManipulator', 'WSComm', '$q',
            function ($rootScope, $location, DialogService, BoardManipulator, WSComm, $q) {
 
                var refreshApp = function(board){

                    if(board.templates.length !== 0){
                         $rootScope.templates = board.templates;
                         var templateid = $rootScope.templates[0].id;
                         var templateName = $rootScope.templates[0].name;
                         
                         //need to update the mainBoard
                         BoardManipulator.fetchTemplate(templateid)
                            .then(function (response) {
                                $rootScope.mainBoard = mainBoardLocal(JSON.parse(response.board));
                                $rootScope.templateId = templateid;
                                $rootScope.templateName = templateName;
                                $rootScope.tabs = [];
                            });
                    }else if(board.templates.length === 0 || angular.isUndefined($rootScope.mainBoard)){
                         $rootScope.mainBoard = [];
                         $rootScope.templates = [];
                    }
                    $rootScope.$broadcast('refreshApp');
                };
                 
                var mainBoardLocal = function (board) {
                    var mainBoard = new Board(board.name, board.numberOfColumns);

                    angular.forEach(board.columns, function (column) {
                        BoardManipulator.addColumn(mainBoard, column.id, column.name);

                        angular.forEach(column.cards, function (card) {

                            BoardManipulator.addCardToColumn(mainBoard, column, card.id, card.title, card.details,
                                    card.editing, card.find, card.required, card.sizefield, card.description, card.fieldtypeid);
                        });
                    });

                    return mainBoard;
                };
                          
                return {
                    
                    /*
                     ADDED FUNCTIONALITY TO ALLOW EDITING THE TEXT OF A COMPONENT
                     */
                    editCardText: function (scope, card) {
                        //alert('editing the card: BoardService');
                        BoardManipulator.startCardEditing(scope, card);
                    },
                    
                    editColumnText: function (scope, column) {
                        BoardManipulator.startColumnEditing(scope, column);
                    },
                    
                    doneEditing: function (scope, item) {
                        BoardManipulator.doneEditing(scope, item);
                    },
                    
                    /* MAKES A CARD SEARCHABLE */
                    makeSearchable: function (scope, card) {
                        //if(confirm('Do you really want to make this card searchable?')){
                        BoardManipulator.makeSearchable(scope, card);
                        //}
                    },
                    
                    makeRequired: function (scope, card) {
                        BoardManipulator.makeRequired(scope, card);
                    },
                    
                    editSizeField: function (scope, card) {
                        BoardManipulator.editSizeField(scope, card);
                    },
                    
                    doneEditingSizeField: function (scope, card) {
                        BoardManipulator.doneEditingSizeField(scope, card);
                    },
                    
                    fetchTemplates: function () {
                        var defer = $q.defer();

                        BoardManipulator.fetchTemplates()
                            .then(function (response) {
                                $rootScope.templates = JSON.parse(response.board);
                                //pass along to the caller the response string
                                defer.resolve(response);
                            });

                        return defer.promise;
                    },
                    
                    fetchTemplate: function (templateId) {
                        return BoardManipulator.fetchTemplate(templateId);
                    },
                    
                    viewMetadata: function (tableid) {
                        var defer = $q.defer();

                        BoardManipulator.fetchMetadata(tableid)
                                .then(function (response) {
                                    console.log("METADATA RETRIEVED " + JSON.stringify(response.board));

                                    $rootScope.metadata = JSON.parse(response.board);
                                    defer.resolve($rootScope.metadata);
                                });

                        return defer.promise;
                    },
                    
                    /*
                     * Adds a new list to the board
                     */
                    addNewList: function (templateId) {
                        var defer = $q.defer();
                        var content = {view: 'views/partials/newList.html',
                            controller: 'NewlistController'};

                        DialogService.launch('custom', content)
                                .result.then(function (list) {

                                    if (!angular.isUndefined(list)) {
                                        //PERSIST THE WHOLE BOARD TO THE DATABASE (COARSE GRAINED PERSISTENCE)
                                        //TODO add specific list saving functionality
                                        BoardManipulator.addColumn($rootScope.mainBoard, list.id, list.name);
                                        BoardManipulator.storeTemplate(templateId, $rootScope.mainBoard)
                                                .then(function (response) {
                                                    //$location.path('/metaDesign');
                                                    console.log("TEMPLATE STORED SUCCESSFULLY " + JSON.stringify(response));
                                                    $rootScope.mainBoard = JSON.parse(response.board);
                                                    defer.resolve($rootScope.mainBoard);
                                                });
                                    }
                                });

                        return defer.promise;
                    },
                    
                    removeList: function (templateId, column) {
                        var defer = $q.defer();
                        var content = {header: 'List delete',
                            body: 'Are you sure you want to delete table \'' + column.name + '\'?'};

                        DialogService.launch('confirm', content)
                            .result.then(function (response) {

                            column.forceDelete = false;
                            BoardManipulator.deleteList(templateId, column)
                            .then(function (response) {
                                var status = response.status;
                                if (angular.equals(status, "ERROR")) {
                                    console.log("Could not delete table. " + response.board);
                                    DialogService.launch('confirm', {
                                        header: 'Table delete problem',
                                        body: 'Could not delete \'' + column.name + '\': ' + response.board + '.' +
                                                '<br>Do you want to remove the associated fields as well?'
                                    })
                                    .result.then(function (bttn) {
                                        console.log('You confirmed "YES."');
                                        column.forceDelete = true;

                                        BoardManipulator.deleteList(templateId, column)
                                                .then(function (resp) {
                                                    $rootScope.mainBoard = JSON.parse(resp.board);
                                                    defer.resolve({response: "OK"});
                                                });
                                        });
                                    return;
                                }
                                console.log("RESPONSE " + JSON.stringify(response.board));
                                $rootScope.mainBoard = JSON.parse(response.board);
                                //time to let the controller refresh the view
                                defer.resolve({response: "OK"});
                            });
                        }, function (btn) {
                            //handle the no answer
                            console.log('You confirmed "No."');
                            defer.resolve({response: 'negative'});
                        });

                        return defer.promise;
                    },
                    
                    /*
                     * Adds a new card to a list
                     */
                    addNewCard: function (templateId, column) {

                        var defer = $q.defer();
                        var content = {view: 'views/partials/newCardDialog.html',
                            controller: 'NewCardDialogController',
                            data: column};

                        DialogService.launch('custom', content)
                                .result.then(function (card) {
                                    //PERSIST THE CARD TO THE DATABASE (FINE GRAINED PERSISTENCE)
                                    BoardManipulator.storeCard(templateId, column, card)
                                            .then(function (response) {
                                                $rootScope.mainBoard = JSON.parse(response.board);
                                                console.log("card saved successfully " + JSON.stringify(response.board));
                                                defer.resolve($rootScope.mainBoard);
                                            });
                                }, function () {
                                    console.log("not save");
                                    defer.resolve($rootScope.mainBoard);
                                });

                        return defer.promise;
                    },
                    
                    /*
                     * removes a card (or field) from a list (or table), using the promise api. This way
                     * the calling controller knows when an action has completed and so when to refresh the view
                     * so that the changes appear
                     */
                    removeCard: function (templateId, column, card) {

                        var defer = $q.defer();
                        var content = {header: 'Card delete',
                            body: 'Are you sure you want to delete card \'' + card.title + '\'?'};

                        DialogService.launch('confirm', content)
                        .result.then(function (btn) {
                            //handle the answer 'yes'
                            card.forceDelete = false;
                            BoardManipulator.deleteCard(templateId, column, card)
                            .then(function (response) {
                                var status = response.status;
                                if (status === "ERROR") {
                                    console.log("Could not delete card. " + response.board);
                                    DialogService.launch('confirm', {
                                        header: 'Card delete problem',
                                        body: 'Could not delete \'' + card.title + '\': ' + response.board + '.' +
                                                '<br>Do you want to remove the associated data as well?'
                                    })
                                    .result.then(function (bttn) {
                                        console.log('You confirmed "YES."');
                                        card.forceDelete = true;

                                        BoardManipulator.deleteCard(templateId, column, card)
                                                .then(function (resp) {
                                                    $rootScope.mainBoard = JSON.parse(resp.board);
                                                    defer.resolve({response: "OK"});
                                                });
                                            });
                                    return;
                                }
                                console.log("RESPONSE " + JSON.stringify(response.board));
                                $rootScope.mainBoard = JSON.parse(response.board);
                                //time to let the controller refresh the view
                                defer.resolve({response: "OK"});
                            });
                        }, function (btn) {
                            //handle the no answer
                            console.log('You confirmed "No."');
                            defer.resolve({response: 'peanuts'});
                        });
                        return defer.promise;
                    },
                    
                    removeCardFromBoard: function (board, column, card) {
                        BoardManipulator.removeCardFromColumn(board, column, card);
                    },
                    
                    //TEMPLATE HANDLING FUNCTIONS
                    addNewTemplate: function (templateName) {
                        var defer = $q.defer();

                        BoardManipulator.addNewTemplate(templateName)
                            .then(function (response) {
                                var board = JSON.parse(response.board);
                                refreshApp(board);
                                
                                defer.resolve(response.board);
                            });

                        return defer.promise;
                    },
                    
                    removeTemplate: function (templateId) {
                        var defer = $q.defer();

                        BoardManipulator.removeTemplate(templateId)
                            .then(function (response) {
                                var board = JSON.parse(response.board);
                                refreshApp(board);
                                
                                defer.resolve(response.board);
                        });

                        return defer.promise;
                    },
                    
                    fetchFieldTypes: function(){
                        var defer = $q.defer();

                        BoardManipulator.fetchFieldTypes()
                            .then(function (response) {
                                
                                var fieldTypes = JSON.parse(response.board);
                                defer.resolve(fieldTypes);
                            });

                        return defer.promise;
                    },
                    
                    /* *** SCHEMA HANDLING FUNCTIONS *** */
                    storeCard: function (card) {
                        return BoardManipulator.storeCard(card);
                    },
                    
                    deleteCard: function (card) {
                        return BoardManipulator.deleteCard(card);
                    },
                    
                    storeTemplate: function (templateId, board) {
                        return BoardManipulator.storeTemplate(templateId, board);
                    },
                    
                    storeMetadata: function (metadata) {
                        return BoardManipulator.storeMetadata(metadata);
                    },
                                        
                    refreshMetadataTabs: function () {
                        $rootScope.tabs = [];
                        angular.forEach($rootScope.mainBoard.columns, function (value, key) {
                            //console.log(key + ': ' + value.name);
                            $rootScope.tabs.push({title: value.name, cards: value.cards});
                        });
                    },
                    
                    getBoard: function () {
                        return $rootScope.mainBoard;
                    },
                    
                    getMetadata: function () {
                        return $rootScope.metadata;
                    },
                    
                    getTemplates: function () {
                        return $rootScope.templates;
                    },
                    
                    getTemplateId: function () {
                        return $rootScope.templateId;
                    },
                                                            
                    /**
                     * Constructor that initializes the kanban board creating all the corresponding card columns
                     * @param board is a json object containing all the columns and their cards
                     */
                    mainBoard: function (board) {
                        var mainBoard = new Board(board.name, board.numberOfColumns);

                        angular.forEach(board.columns, function (column) {
                            BoardManipulator.addColumn(mainBoard, column.id, column.name);

                            angular.forEach(column.cards, function (card) {
                                BoardManipulator.addCardToColumn(mainBoard, column, card.id, card.title, card.details,
                                        card.editing, card.find, card.required, card.sizefield, 
                                        card.description, card.fieldtypeid, card.fieldtypeContent);
                            });
                        });

                        return mainBoard;
                    },
                    
                    /**
                     * Constructor that initializes the sprint board page creating all the corresponding card columns
                     */
                    sprintBoard: function (board) {
                        var sprintBoard = new Board(board.name, board.numberOfColumns);
                        angular.forEach(board.columns, function (column) {
                            BoardManipulator.addColumn(sprintBoard, column.name);
                        });

                        angular.forEach(board.backlogs, function (backlog) {
                            BoardManipulator.addBacklog(sprintBoard, backlog.title);
                            angular.forEach(backlog.phases, function (phase) {
                                BoardManipulator.addPhaseToBacklog(sprintBoard, backlog.title, phase);
                                angular.forEach(phase.cards, function (card) {
                                    BoardManipulator.addCardToBacklog(sprintBoard, backlog.title, phase.name, card);
                                });
                            });
                        });
                        return sprintBoard;
                    }
                };
            }]);