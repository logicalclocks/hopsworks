/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.project.fb;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import se.kth.bbc.fileoperations.Operation;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author vangelis
 */
@Stateless
public class InodeOpsFacade extends AbstractFacade<InodeOps> {

    private static final Logger logger = Logger.getLogger(InodeOpsFacade.class.getName());

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public InodeOpsFacade() {
        super(InodeOps.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /**
     * persists an operation for a specific inode only if it doesn't exist.
     * Otherwise updates an already existing operation
     *
     * @param inodeops
     */
    public void persist(InodeOps inodeops) {
        Query query = this.em.createNamedQuery("InodeOps.findByInodeid");
        query.setParameter("inodeid", inodeops.getInodeid());

        List list = query.getResultList();

        if (list.isEmpty()) {
            this.em.persist(inodeops);
            this.em.flush();
        } else {
            logger.log(Level.SEVERE, "Could not add operation for file {0} ", inodeops.getInodeid());
            this.update(inodeops);
        }
    }

    public void update(InodeOps inodeops) {
        this.em.merge(inodeops);
        this.em.flush();
    }

    public void createAndStoreOperation(Inode inode, Operation operation) {

        int intOperation = -1;

        switch (operation) {
            case ADD:
                intOperation = 1;
                break;
            case REMOVE:
                intOperation = 0;
                break;
            default:
                intOperation = -100;
        }

        String event = intOperation + "|" + inode;

        //update the 'modified' field with the current timestamp 
        //so that it can be fetched for indexing
        InodeOps inodeops = new InodeOps(inode.getId(),
                inode.getParent().getId(), inode.getRoot(),
                new Date(), intOperation, 0);
        
        System.out.println(inodeops);
        this.persist(inodeops);
    }
}
