/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('metaUI').factory('BoardManipulator', function (WSComm) {

    return {
        /*
         ADDED FUNCTIONALITY TO SUPPORT EDITING THE CARD TEXT
         */
        startCardEditing: function (scope, card) {

            card.editing = true;
            scope.editedItem = card;

            //alert('started editing ' + card.title + " editing " + card.editing);
        },
        
        startColumnEditing: function (scope, column) {

            column.editing = true;
            scope.editedItem = column;
        },
        
        doneEditing: function (scope, item) {
            item.editing = false;
            item.size = false;

            scope.editedItem = null;
            //dong some background ajax calling for persistence...

            if (item.title === null) {
                console.log("submitting text " + item.name);
            }
            else {
                console.log("submitting text: " + item.title + " | details: " + item.details + "| find: "
                        + item.find + " | editing: " + item.editing);
            }
        },
        
        /* MAKES A CARD SEARCHABLE */
        makeSearchable: function (scope, card) {

            card.find = !card.find;
            console.log("Card " + card.title + " became searchable " + card.find);
        },
        
        makeRequired: function (scope, card) {
            card.required = !card.required;
            console.log("Card " + card.title + " became required " + card.required);
        },
        
        editSizeField: function (scope, card) {
            //angular.forEach(card, function(value, key) {
            //  alert(key + ': ' + value);
            //});
            card.sizefield.showing = !card.sizefield.showing;
            console.log("Card " + card.title + " showing " + card.sizefield.showing + " max size " + card.sizefield.value);

            scope.editedField = card;
        },
        
        doneEditingSizeField: function (scope, card) {

            card.sizefield.showing = false;
            scope.editedField = null;
        },
        
        addColumn: function (board, columnId, columnName) {
            console.log("pushing a new column into " + board.name + ": id " + columnId + " name " + columnName);
            board.columns.push(new Column(columnId, columnName));
        },
        
        /* CREATES ALL CARD OBJECTS AND ADDS THEM INTO THE CORRESPONDING LIST ARRAY */
        addCardToColumn: function (board, column, cardId, cardTitle, details, 
                                        editing, find, required, sizefield, 
                                        description, fieldtypeid, fieldtypeContent) {
            angular.forEach(board.columns, function (col) {
                if (col.name === column.name) {

                    /* CREATES ALL CARD OBJECTS AND ADDS THEM INTO THE CORRESPONDING LIST ARRAY */
                    col.cards.push(new Card(cardId, cardTitle, column.name, details, 
                                            editing, find, required, sizefield, 
                                            description, fieldtypeid, fieldtypeContent));
                }
            });
        },
        
        /* currently useless - all the view state is being handled by the back end */
        removeCardFromColumn: function (board, column, card) {

            angular.forEach(board.columns, function (col) {
                if (col.name === column.name) {
                    col.cards.splice(col.cards.indexOf(card), 1);
                }
            });
        },
        
        addBacklog: function (board, backlogName) {
            board.backlogs.push(new Backlog(backlogName));
        },
        
        addPhaseToBacklog: function (board, backlogName, phase) {
            angular.forEach(board.backlogs, function (backlog) {
                if (backlog.name === backlogName) {
                    backlog.phases.push(new Phase(phase.name));
                }
            });
        },
        
        addCardToBacklog: function (board, backlogName, phaseName, task) {
            angular.forEach(board.backlogs, function (backlog) {
                if (backlog.name === backlogName) {
                    angular.forEach(backlog.phases, function (phase) {
                        if (phase.name === phaseName) {
                            phase.cards.push(new Card(task.title, task.status, task.details));
                        }
                    });
                }
            });
        },
        
        /* COMMUNICATION WITH THE SERVER */
        fetchTemplates: function () {

            return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'fetch_templates',
                message: JSON.stringify({})
            });
        },
        
        /**
         * Fetches the schema from the database
         */
        fetchTemplate: function (templateId) {
            return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'fetch_template',
                message: JSON.stringify({tempid: templateId})
            });
        },
        
        storeTemplate: function (templateId, board) {
            return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'store_template',
                message: JSON.stringify({tempid: templateId, bd: board})
            });
        },
        
        addNewTemplate: function(templateName){
            return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'add_new_template',
                message: JSON.stringify({templateName: templateName})
            });
        },
        
        removeTemplate: function(templateId){
            return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'remove_template',
                message: JSON.stringify({templateId: templateId})
            });
        },
        
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
        
        storeCard: function (templateId, column, card) {
            return WSComm.send({
                sender: 'evsav',
                type: 'FieldsMessage',
                action: 'store_field',
                message: JSON.stringify({
                    tempid: templateId,
                    tableid: column.id,
                    tablename: column.name,
                    id: card.id,
                    name: card.title,
                    type: 'VARCHAR(50)',
                    searchable: card.find,
                    required: card.required,
                    sizefield: card.sizefield,
                    description: card.description,
                    fieldtypeid: card.fieldtypeid,
                    fieldtypeContent: card.fieldtypeContent
                })
            });
        },
        
        deleteCard: function (templateId, column, card) {
            
            return WSComm.send({
                sender: 'evsav',
                type: 'FieldsMessage',
                action: 'delete_field',
                message: JSON.stringify({
                    tempid: templateId,
                    id: card.id,
                    tableid: column.id,
                    tablename: column.name,
                    name: card.title,
                    type: 'VARCHAR(50)',
                    sizefield: card.sizefield,
                    searchable: card.find,
                    required: card.required,
                    forceDelete: card.forceDelete,
                    description: card.description,
                    fieldtypeid: card.fieldtypeid,
                    fieldtypeContent: card.fieldtypeContent
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
        
        fetchMetadata: function (tableId) {
            return WSComm.send({
                sender: 'evsav',
                type: 'MetadataMessage',
                action: 'fetch_metadata',
                message: JSON.stringify({tableid: tableId})
            });
        },
        
        fetchFieldTypes: function(){
            return WSComm.send({
                sender: 'evsav',
                type: 'FieldTypesMessage',
                action: 'fetch_field_types',
                message: 'null'
            });
        }
    };
});
