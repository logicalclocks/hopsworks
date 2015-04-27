
package se.kth.meta.wscomm;

import se.kth.meta.db.Dbao;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.Fields;
import se.kth.meta.entity.RawData;
import se.kth.meta.entity.Tables;
import se.kth.meta.exception.ApplicationException;
import se.kth.meta.exception.DatabaseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.kth.meta.entity.Templates;
import se.kth.meta.entity.TupleToFile;

/**
 *
 * @author Vangelis
 */
public class Utils {

    private Dbao db;

    public Utils(Dbao db) {
        this.db = db;
    }

    public void addNewTemplate(Templates template) throws ApplicationException {

        try {
            this.db.addTemplate(template);
        } catch (DatabaseException e) {
            throw new ApplicationException("Could not add new template " + template.getName() + ""
                    + " " + e.getMessage());
        }
    }

    public void removeTemplate(Templates template) throws ApplicationException {
        try{
            this.db.removeTemplate(template);
        }catch(DatabaseException e){
            throw new ApplicationException("Could not remove template " + template.getName() + ""
                    + " " + e.getMessage());
        }
    }

    public void addTables(List<EntityIntf> list) throws ApplicationException {

        for (EntityIntf entry : list) {
            Tables t = (Tables) entry;
            String tableName = t.getName();
            int tid = t.getId();
            List<Fields> tableFields = t.getFields();

            try {
                int tableId = this.db.addTable(t);

                System.out.println("TABLE: " + tableName);
                //if the table is new, persist the fields manually so that they get an id
                if (tid == -1) {
                    for (Fields field : tableFields) {
                        field.setTableid(tableId);
                        //t.addField(field);
                        this.db.addField(field);
                    }
                }
            } catch (DatabaseException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                throw new ApplicationException("Could not add table " + t.getName() + " " + ex.getMessage());
            }
        }
    }

    public void addFields(List<EntityIntf> list) throws ApplicationException {

    }

    public void deleteTable(Tables table) throws ApplicationException {
        try {
            System.err.println("DELETING TABLE " + table.getName());
            this.db.deleteTable(table);
        } catch (DatabaseException e) {
            throw new ApplicationException("Utils.java: method deleteTable "
                    + "encountered a problem", e.getMessage());
        }
    }

    public void deleteField(Fields field) throws ApplicationException {

        try {
            System.err.println("DELETING FIELD " + field);
            this.db.deleteField(field);
        } catch (DatabaseException e) {
            throw new ApplicationException("Utils.java: method deleteField "
                    + "encountered a problem", e.getMessage());
        }
    }

    public void storeMetadata(List<EntityIntf> list) throws ApplicationException {

        try {
            int tupleid = this.db.getLastInsertedTupleId() + 1;
            int inodeid = -1;

            //every rawData entity carries the same inodeid
            for (EntityIntf raw : list) {

                RawData r = (RawData) raw;
                r.setData(r.getData().replaceAll("\"", ""));
                r.setTupleid(tupleid);
                inodeid = r.getInodeid();

                System.out.println(r);
                ((Dbao) this.db).addRawData(r);
            }

            TupleToFile ttf = new TupleToFile(tupleid, inodeid);
            ((Dbao) this.db).addTupleToFile(ttf);

        } catch (DatabaseException e) {
            throw new ApplicationException("Utils.java: storeMetadata(List<?> list) "
                    + "encountered a problem", e.getMessage());
        }
    }

}
