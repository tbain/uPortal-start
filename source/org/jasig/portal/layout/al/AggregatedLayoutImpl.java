/* Copyright 2004 The JA-SIG Collaborative.  All rights reserved.
*  See license distributed with this file and
*  available online at http://www.uportal.org/license.html
*/

package org.jasig.portal.layout.al;

import java.util.Set;
import org.jasig.portal.PortalException;
import org.jasig.portal.layout.al.common.ILayoutCommandManager;
import org.jasig.portal.layout.al.common.ILayoutSubtree;
import org.jasig.portal.layout.al.common.LayoutEventListener;
import org.jasig.portal.layout.al.common.node.ILayoutNode;
import org.jasig.portal.layout.al.common.node.INode;
import org.jasig.portal.layout.al.common.node.INodeDescription;
import org.jasig.portal.layout.al.common.node.INodeId;
import org.jasig.portal.layout.al.common.node.NodeType;
import org.jasig.portal.layout.al.common.restrictions.IRestrictionManager;
import org.xml.sax.ContentHandler;

/**
 * Aggregated user layout manager implementation
 * 
 * @author Michael Ivanov <a href="mailto:">mvi@immagic.com</a>
 * @author Peter Kharchenko: pkharchenko at unicon.net
 * @version $Revision$
 */
public class AggregatedLayoutImpl implements  IAggregatedLayout {
	
    private ILayoutCommandManager layoutCommandManager;
    private IRestrictionManager restrictionManager;
    
    // internal representation of an assembled layout
    private ILayoutSubtree currentLayout;
    
    // fragment manager
    private IFragmentRegistry fragmentRegistry;    
	
    // assemble user layout from scratch
    public void loadUserLayout() {
        // obtain user fragment, set it to be root
        // should lost folder (i.e. loose nodes be obtained separately?)
        
        // obtain list of all pushed fragments
        
        // obtain list of operations
        
        // perform operations, recording which pushed fragments
        // has been successfully attached
        
    }
    
    protected ILayoutNode addNodeToLostFolder(INode node) throws PortalException {
      return currentLayout.addNode(node,IALFolderDescription.LOST_FOLDER_ID,null);	
    }
    
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.ILayout#addNodes(org.jasig.portal.layout.al.common.node.ILayoutNode, org.jasig.portal.layout.al.common.node.INodeId, org.jasig.portal.layout.al.common.node.INodeId)
     */
    public ILayoutNode addNode(INode node, INodeId parentId, INodeId nextId) throws PortalException {
    	
        // attach newly constructed copy to the active layout, checking restrictions first
    	if ( node != null && restrictionManager.checkAddRestrictions(node,parentId,nextId) ) {
         IALNode parentNode = (IALNode) getNode(parentId);		
    	 if ( ((IALNode)node).getFragmentId().equals(parentNode.getFragmentId())) {	
    	   return currentLayout.addNode(node,parentId,nextId);
           // if fragmentId of the nodes being attached is not the same as the
           // fragmentId of the attachment point, place the subtree under the "lost folder" for that fragment
           // and record appropriate "move" operation.
    	 } else { 
    	 	ILayoutNode newNode = addNodeToLostFolder(node);
    	 	if (layoutCommandManager.moveNode(newNode.getId(),parentId,nextId) &&
    	 	    currentLayout.moveNode(newNode.getId(),parentId,nextId) );
    	 	  return newNode;
    	 }
    	} 
        return null;
    }
    
    /* (non-Javadoc)
	 * @see org.jasig.portal.layout.al.ILayout#deleteNode(org.jasig.portal.layout.node.INodeId)
	 */
    public boolean deleteNode(INodeId nodeId) throws PortalException {
        // check restrictions
        if(nodeId!=null && restrictionManager.checkDeleteRestrictions(nodeId)) {
            // if central modifications
            if(isCentralModification(nodeId)) {
                IALNode node=(IALNode) getNode(nodeId);
                IFragmentId fragmentId=node.getFragmentId();		        
                //	if fragment root, delete fragment ?
                if(node.isFragmentRoot()) {
                    // TODO: peterk: not sure what to do here. In central
                    // modification mode this, most likely implies that
                    // user is attempting to delete the fragment. In that
                    // case we have to worry about left-over nodes belonging
                    // to that fragment that are not situated under the root
                    // subtree. Then again, in central modification mode, all
                    // node fragments should be (somehow) under the fragment
                    // root node.
                    //fragmentManager.deleteFragment(fragmentId);
                }
                //  otherwise: perform node deletion
                IFragment fragment=fragmentRegistry.getFragment(fragmentId);
                if(fragment.deleteNode(node.getFragmentNodeId())) {
                    // notify command manager that a node has been centrally deleted
                    layoutCommandManager.notifyCentralDeleteNode(nodeId);
                    // perform deletion in local layout snapshot
                    return currentLayout.deleteNode(nodeId);
                }
            } else {
                //  record "delete" operation
                if(layoutCommandManager.deleteNode(nodeId)) {
                    return currentLayout.deleteNode(nodeId);
                }
            }
        }
        return false;
    }
	
	/* (non-Javadoc)
	 * @see org.jasig.portal.layout.al.ILayout#moveNode(org.jasig.portal.layout.node.INodeId, org.jasig.portal.layout.node.INodeId, org.jasig.portal.layout.node.INodeId)
	 */
	public boolean moveNode(INodeId nodeId, INodeId parentId, INodeId nextId) {
		// check restrictions
	    // 
	    // edit-oriented logic:
	    // if cm(d) {
	    //   if(cm(s)) {
	    //     remove node from s, add to d fragments
	    //   } else {
	    //     perform local delete on s
	    //     add node to d
	    //   }
	    // } else {
	    //    if(cm(s)) {
	    //      remove node from s, add to user fragment
	    //      record attachment node operation on d
	    //    } else {
	    //      record move operation from s to d
	    //    }
	    // }
	    // 
	    // // simplified central mod logic:
	    // if(cm(d) && cm(s)) {
	    //   remove node from s, add to d
	    // } else {
	    //   record move operation from d to s
	    // }
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.jasig.portal.layout.al.ILayout#updateNode(org.jasig.portal.layout.node.INodeDescription)
	 */
	public INodeDescription updateNode(INodeDescription nodeDesc) {
		 
		return null;
	}

    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayoutManagerCommands#updateNode(org.jasig.portal.layout.al.common.node.INodeId, org.jasig.portal.layout.al.common.node.INodeDescription)
     */
    public boolean updateNode(INodeId nodeId, INodeDescription nodeDescription) throws PortalException {
        //  check restrictions
        if(nodeId!=null && nodeDescription!=null && restrictionManager.checkUpdateRestrictions(nodeDescription,nodeId)) {
            if(isCentralModification(nodeId)) {
                IALNode node=(IALNode) getNode(nodeId);
                IFragmentId fragmentId=node.getFragmentId();
                IFragment fragment=fragmentRegistry.getFragment(fragmentId);
                if(fragment.updateNode(node.getFragmentNodeId(),nodeDescription)) {
                    boolean result = currentLayout.updateNode(nodeId,nodeDescription);
                    // TODO: peterk: we should probably reload the layout, otherwise
                    // we can not gurantee that the next time user logs in the
                    // layout will look the same (i.e. some restrictions could've
                    // changed that can prevent current state from being achieved
                    return result;
                }
            } else {
                if(layoutCommandManager.updateNode(nodeId,nodeDescription)) {
                    return currentLayout.updateNode(nodeId,nodeDescription);
                }
            }
        }
        return false;
    }
    
    /**
     * Import a subtree by constructing a local copy with
     * assigned ids.
     * @param node
     * @return a local aggregated layout node copy with assigned ids
     */
    public IALNode importNodes(INode node) throws PortalException {
        // construct subtree copy, assigning layout ids.
        // if the layout node implements IALNode, make use of the available
        // restriction information, otherwise assign default restrictions (none?)
        
        return null;
    }
    	
	/**
	 * Determine if a fragment to which a given node belongs is currently being centrally modified.
	 * In other words, both permissions and user preferences flags allow central modification.
	 * @param nodeId node id
	 * @return <code>true</code> if the fragment to which the node belongs is being centrally modified
	 */
	protected boolean isCentralModification(INodeId nodeId) {
	    return false;
	}

	/**
     * @return Returns the layoutCommandManager.
     */
    public ILayoutCommandManager getLayoutCommandManager() {
        return layoutCommandManager;
    }
    /**
     * Secify layout command manager to be used
     * @param layoutCommandManager The layoutCommandManager to set.
     */
    public void setLayoutCommandManager(ILayoutCommandManager layoutCommandManager) {
        this.layoutCommandManager = layoutCommandManager;
    }
    /**
     * @return Returns the restrictionManager.
     */
    public IRestrictionManager getRestrictionManager() {
        return restrictionManager;
    }
    /**
     * @param restrictionManager The restrictionManager to set.
     */
    public void setRestrictionManager(IRestrictionManager restrictionManager) {
        this.restrictionManager = restrictionManager;
    }
    
    /**
     * @return Returns the fragmentRegistry.
     */
    public IFragmentRegistry getFragmentRegistry() {
        return fragmentRegistry;
    }
    /**
     * @param fragmentRegistry The fragmentRegistry to set.
     */
    public void setFragmentRegistry(IFragmentRegistry fragmentRegistry) {
        this.fragmentRegistry = fragmentRegistry;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#addLayoutEventListener(org.jasig.portal.layout.al.common.LayoutEventListener)
     */
    public boolean addLayoutEventListener(LayoutEventListener l) {
        // TODO Auto-generated method stub
        return false;
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#canAddNode(org.jasig.portal.layout.al.common.node.INode, org.jasig.portal.layout.al.common.node.INodeId, org.jasig.portal.layout.al.common.node.INodeId)
     */
    public boolean canAddNode(INode node, INodeId parentId, INodeId nextSiblingId) throws PortalException {
        return (node!=null && parentId!=null && restrictionManager.checkAddRestrictions(node,parentId,nextSiblingId));
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#canDeleteNode(org.jasig.portal.layout.al.common.node.INodeId)
     */
    public boolean canDeleteNode(INodeId nodeId) throws PortalException {
        return (nodeId!=null && restrictionManager.checkDeleteRestrictions(nodeId));
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#canMoveNode(org.jasig.portal.layout.al.common.node.INodeId, org.jasig.portal.layout.al.common.node.INodeId, org.jasig.portal.layout.al.common.node.INodeId)
     */
    public boolean canMoveNode(INodeId nodeId, INodeId parentId, INodeId nextSiblingId) throws PortalException {
        return (nodeId!=null && parentId!=null && restrictionManager.checkMoveRestrictions(nodeId,parentId,nextSiblingId));
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#canUpdateNode(org.jasig.portal.layout.al.common.node.INodeId, org.jasig.portal.layout.al.common.node.INodeDescription)
     */
    public boolean canUpdateNode(INodeId nodeId, INodeDescription nodeDescription) throws PortalException {
        return (nodeId!=null && nodeDescription!=null && restrictionManager.checkUpdateRestrictions(nodeDescription,nodeId));
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#createNodeDescription(org.jasig.portal.layout.al.common.node.NodeType)
     */
    public INodeDescription createNodeDescription(NodeType nodeType) throws PortalException {
        // TODO Auto-generated method stub
        return null;
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#getCacheKey()
     */
    public String getCacheKey() throws PortalException {
        // TODO Auto-generated method stub
        return null;
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#getDepth(org.jasig.portal.layout.al.common.node.INodeId)
     */
    public int getDepth(INodeId nodeId) throws PortalException {
        // TODO Auto-generated method stub
        return 0;
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#getNode(org.jasig.portal.layout.al.common.node.INodeId)
     */
    public INode getNode(INodeId nodeId) throws PortalException {
        // TODO Auto-generated method stub
        return null;
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#markAddTargets(org.jasig.portal.layout.al.common.node.INode)
     */
    public void markAddTargets(INode node) throws PortalException {
        // TODO Auto-generated method stub
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#markMoveTargets(org.jasig.portal.layout.al.common.node.INodeId)
     */
    public void markMoveTargets(INodeId nodeId) throws PortalException {
        // TODO Auto-generated method stub
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#outputLayout(org.xml.sax.ContentHandler)
     */
    public void outputLayout(ContentHandler ch) throws PortalException {
        // TODO Auto-generated method stub
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#outputLayout(org.jasig.portal.layout.al.common.node.INodeId, org.xml.sax.ContentHandler)
     */
    public void outputLayout(INodeId nodeId, ContentHandler ch) throws PortalException {
        // TODO Auto-generated method stub
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayout#removeLayoutEventListener(org.jasig.portal.layout.al.common.LayoutEventListener)
     */
    public boolean removeLayoutEventListener(LayoutEventListener l) {
        // TODO Auto-generated method stub
        return false;
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.common.ILayoutSubtree#getRootNodeId()
     */
    public INodeId getRootNodeId() {
        // TODO Auto-generated method stub
        return null;
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.layout.al.IAggregatedLayout#getFragmentIds()
     */
    public Set getFragmentIds() throws PortalException {
        // TODO Auto-generated method stub
        return null;
    }
}
