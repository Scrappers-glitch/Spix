/*
 * $Id$
 * 
 * Copyright (c) 2016, Simsilica, LLC
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package spix.swing;

import groovy.util.ObservableList.*;
import spix.core.Action;
import spix.core.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.util.List;

import static javax.swing.SwingUtilities.getWindowAncestor;
import static spix.core.ToggleAction.TOGGLED_LARGE_ICON;

/**
 *
 *
 *  @author    Paul Speed
 */
public class ActionUtils {

    public static JToolBar createActionToolbar(ActionList actions, Spix spix, int orientation) {
        if (actions == null) {
            throw new IllegalArgumentException("Cannot create toolbar for null action list.");
        }

        JToolBar result = new JToolBar(orientation);//new SwingAction(actions, spix));
        for (Iterator i = actions.iterator(); i.hasNext(); ) {
            Action a = (Action) i.next();
            if (a == null) {
                result.addSeparator();
            } else {
                result.add(createActionToolbarButton(a, spix));
            }
        }

        // Setup an object to keep the menu in synch with the
        // list.
        actions.addPropertyChangeListener(new ActionMenuCoordinator(result, spix));

        return result;
    }


    public static AbstractButton createActionToolbarButton(Action a, Spix spix) {

        if (a instanceof ActionList) {
            JMenu menu = createActionMenu((ActionList) a, spix);
            JButton b = new JButton((String) a.get(Action.NAME));
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Point p = SwingUtilities.convertPoint(b, b.getLocation(), getWindowAncestor(b));
                    Rectangle bounds = SwingUtilities.getWindowAncestor(b).getBounds();
                    int x = 0, y = (int) b.getPreferredSize().getHeight() + 3;
                    if (p.getY() > bounds.getHeight() / 2) {
                        y = (int) -menu.getPopupMenu().getPreferredSize().getHeight() - 3;
                    }
                    menu.getPopupMenu().show(b, x, y);
                }
            });
            return b;
        }

        if (a instanceof ToggleAction) {
            JToggleButton tb = new JToggleButton(new SwingAction(a, spix));
            tb.setSelected(((ToggleAction) a).isToggled());
            tb.setHideActionText(true);
            if (a.get(TOGGLED_LARGE_ICON) != null) {
                tb.setSelectedIcon((ImageIcon) a.get(TOGGLED_LARGE_ICON));
            }
            return tb;
        }

        return new JButton(new SwingAction(a, spix));
    }



    public static JMenu createActionMenu( ActionList actions, Spix spix ) {
        if( actions == null ) {
            throw new IllegalArgumentException( "Cannot create menu for null action list." );
        }
        
        JMenu result = new JMenu(new SwingAction(actions, spix));
        for( Iterator i = actions.iterator(); i.hasNext(); ) {
            Action a = (Action)i.next();
            if( a == null ) {
                result.addSeparator();
            } else {
                result.add(createActionMenuItem(a, spix));
            }
        }

        // Setup an object to keep the menu in synch with the
        // list.
        actions.addPropertyChangeListener(new ActionMenuCoordinator(result, spix));

        return result;
    } 

    public static JMenuBar createActionMenuBar( ActionList actions, Spix spix ) {
        if( actions == null ) {
            throw new IllegalArgumentException( "Cannot create menu for null action list." );
        }
        
        JMenuBar result = new JMenuBar();
        for( Iterator i = actions.iterator(); i.hasNext(); ) {
            Action a = (Action)i.next();
            if( a == null ) {
                result.add(new JSeparator(SwingConstants.VERTICAL));
            } else {
                result.add(createActionMenuItem(a, spix));
            }
        }
               
        // Setup an object to keep the menu in synch with the
        // list.
        actions.addPropertyChangeListener(new ActionMenuCoordinator(result, spix));

        return result;
    } 
         
    public static JMenuItem createActionMenuItem( Action a, Spix spix ) {
        if( a instanceof ActionList ) {
            return createActionMenu((ActionList)a, spix);
        }

        if( a instanceof ToggleAction ) {
            JCheckBoxMenuItem cb = new JCheckBoxMenuItem(new SwingAction(a, spix));
            return cb;
        }

        return new JMenuItem(new SwingAction(a, spix));
    }
    
    private static class ActionMenuCoordinator implements PropertyChangeListener {
        private Spix spix;
        private JComponent menu;

        public ActionMenuCoordinator( JComponent menu, Spix spix ) {
            this.menu = menu;
            this.spix = spix;
        }

        public void propertyChange( final PropertyChangeEvent event ) {
System.out.println("ActionMenuCoordinator.propertyChange() on thread:" + Thread.currentThread());        
            if( event instanceof ElementEvent ) {
                // run the event on the swing thread
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        processElementEvent((ElementEvent)event);
                    }
                });
            }
        }
        
        protected void processElementEvent( ElementEvent event ) {
        
System.out.println("ActionMenuCoordinator.Process event on thread:" + Thread.currentThread());        
            switch( event.getChangeType() ) {
                case ADDED:
                    addMenu(event.getIndex(), (Action)event.getNewValue()); 
                    break; 
                case CLEARED:
                    clearMenus(); 
                    break; 
                case MULTI_ADD:
                    addMenus((MultiElementAddedEvent)event);                    
                    break; 
                case MULTI_REMOVE: 
                    removeMenus((MultiElementRemovedEvent)event);                    
                    break; 
                case NONE: 
System.out.println("element event:" + event);
                    break; 
                case REMOVED: 
                    removeMenu(event.getIndex()); 
                    break; 
                case UPDATED:
                    addMenu(event.getIndex(), (Action)event.getNewValue()); 
                    break; 
            }
        }
        
        protected void addMenu( int index, Action a ) {
            if( menu instanceof JMenu ) {
                if( a == null ) {
                    ((JMenu)menu).add(new JSeparator(), index);
                } else {
                    ((JMenu)menu).add(createActionMenuItem(a, spix), index);
                }
            } else if( menu instanceof JPopupMenu ) {
                if( a == null ) {
                    ((JPopupMenu)menu).add(new JSeparator(), index);
                } else {
                    ((JPopupMenu)menu).add(createActionMenuItem(a, spix), index);
                }
            } else if( menu instanceof JMenuBar ) {
                if( a == null ) {
                    ((JMenuBar)menu).add(new JSeparator(SwingConstants.VERTICAL), index);
                } else {
                    ((JMenuBar)menu).add(createActionMenuItem(a, spix), index);
                }
            } else {
                throw new RuntimeException("Unhandled menu type:" + menu.getClass());
            }
        }

        protected void replaceMenu( int index, Action a ) {
            removeMenu(index);
            addMenu(index, a);
        }

        protected void detach( Component c ) {
            if( c instanceof AbstractButton ) {
                // Then clear the action so it doesn't keep sending events for a dead
                // component
                ((AbstractButton)c).setAction(null);
            }
        }

        protected int getChildCount() {
            if( menu instanceof JMenu ) {
                return ((JMenu)menu).getItemCount();
            } else {
                return menu.getComponentCount();
            }
        }
        
        protected Component getChild( int index ) {
            if( menu instanceof JMenu ) {
                return ((JMenu)menu).getItem(index);
            } else {
                return menu.getComponent(index);
            }
        }

        protected void removeMenu( int index ) {
            detach(getChild(index));                    
            
            // Need to clear the action listener from it
            menu.remove(index);
        }
        
        protected void addMenus( MultiElementAddedEvent event ) {
            List values = event.getValues();
            for( int i = 0; i < values.size(); i++ ) {
                addMenu(event.getIndex() + i, (Action)values.get(i));
            }
        }
        
        protected void removeMenus( MultiElementRemovedEvent event ) {
            List values = event.getValues();
            for( int i = 0; i < values.size(); i++ ) {
                removeMenu(i); // not efficient but sufficient
            }
        }
        
        protected void clearMenus() {
            int count = getChildCount();
            for( int i = 0; i < count; i++ ) { 
                detach(getChild(i));
            }
            menu.removeAll();
        }
    }
}


/*
package org.progeeks.util.swing;

import java.awt.Insets;
import java.beans.*;
import java.util.*;
import javax.swing.*;

import org.progeeks.util.*;
import org.progeeks.util.log.*;

 *  A set of utility methods for dealing with actions and action lists.
 *
 *  @version   $Revision: 1.25 $
 *  @author    Paul Speed
public class ActionUtils
{
    static Log log = Log.getLog( ActionUtils.class );

    private static final Insets ZERO_INSETS = new Insets( 0, 0, 0, 0 );

     *  Creates a menu for the specified action list.
    public static JMenu createActionMenu( ActionList actions )
    {
        if( actions == null )
            throw new IllegalArgumentException( "Cannot create menu for null action list." );

        JMenu main = new JMenu( actions );
        for( Iterator i = actions.iterator(); i.hasNext(); )
            {
            Action a = (Action)i.next();
            if( a == null )
                main.addSeparator();
            else
                main.add( createActionMenuItem( a ) );
            }

        // Setup an object to keep the menu in synch with the
        // list.
        actions.addPropertyChangeListener( new ActionMenuCoordinator( main ) );

        return( main );
    }

     *  Creates a menu bar for the specified action list.
    public static JMenuBar createActionMenuBar( ActionList actions )
    {
        if( actions == null )
            throw new IllegalArgumentException( "Cannot create menu for null action list." );

        JMenuBar main = new JMenuBar();
        for( Iterator i = actions.iterator(); i.hasNext(); )
            {
            Action a = (Action)i.next();
            if( a instanceof ActionList )
                {
                main.add( createActionMenu( (ActionList)a ) );
                }
            else if( a == null )
                {
                main.add( new JSeparator( SwingConstants.VERTICAL ) );
                }
            else
                {
                main.add( new JMenu( a ) );
                }
            }

        // Setup an object to keep the menu in synch with the
        // list.
        actions.addPropertyChangeListener( new ActionMenuCoordinator( main ) );

        return( main );
    }

     *  Creates a pop-up menu for the specified action list.
    public static JPopupMenu createPopupMenu( ActionList actions )
    {
        if( actions == null )
            throw new IllegalArgumentException( "Cannot create menu for null action list." );

        JPopupMenu main = new JPopupMenu();

        for( Iterator i = actions.iterator(); i.hasNext(); )
            {
            Action a = (Action)i.next();
            if( a == null )
                main.addSeparator();
            else
                main.add( createActionMenuItem( a ) );
            }

        // Setup an object to keep the menu in synch with the
        // list.
        actions.addPropertyChangeListener( new ActionMenuCoordinator( main ) );

        return( main );
    }

     *  Creates a JToolBar for the specified action list.
    public static JToolBar createToolBar( ActionList actions )
    {
        if( actions == null )
            throw new IllegalArgumentException( "Cannot create menu for null action list." );

        String name = (String)actions.getValue( Action.NAME );
        if( name == null )
            name = "Tools";
        JToolBar tools = new JToolBar(name);
        //tools.setBorder( new LightBevelBorder( LightBevelBorder.RAISED ) );
        //tools.setMargin( ZERO_INSETS );
        tools.setRollover( true );

        // Setup an object to keep the toolbar in synch with the list.
        ActionToolBarCoordinator coordinator = new ActionToolBarCoordinator( tools );
        actions.addPropertyChangeListener( coordinator );

        // Use it to help us add the initial tools
        int index = 0;
        for( Iterator i = actions.iterator(); i.hasNext(); index++ )
            {
            Action a = (Action)i.next();
            coordinator.addTool( index, a );
            }

        return( tools );
    }

     *  Creates a menu for the specified action.  This
     *  method will determine the appropriate menu to create
     *  based on the class of the action specified.  If it is
     *  an ActionList then that createActionMenu will be called.
     *  If it is a CheckBoxAction then a JCheckBoxMenuItem will
     *  be created.  If no special processing is done then a JMenuItem
     *  will be created.
    public static JMenuItem createActionMenuItem( Action a )
    {
        if( a instanceof ActionList )
            return( createActionMenu( (ActionList)a ) );

        if( a instanceof TogglableAction )
            {
            JCheckBoxMenuItem cb = new JCheckBoxMenuItem( a );
            ((TogglableAction)a).addButtonModel( cb.getModel() );

            // For some reason the check state doesn't get updated
            // even though check box action is doing it.
            cb.setState( ((TogglableAction)a).isChecked() );
            return( cb );
            }

        return( new JMenuItem( a ) );
    }

     *  Creates an appropriate toolbar button for the specified action.
     *  The internal toolbar creation routines delegate to this method
     *  for creating toolbar buttons.  For action lists, a pop-up button
     *  is created an a special JPanel subclass is returned instead of
     *  an actual button implementation.
    public static JComponent createToolBarButton( Action a )
    {
        if( a == null )
            throw new IllegalArgumentException( "Action cannot be null." );

        AbstractButton b;

        if( a instanceof TogglableAction )
            {
            //b = new JToggleButton( a );
            b = new EnhancedButton( a );
            b.setModel( new JToggleButton.ToggleButtonModel() );
            ((TogglableAction)a).addButtonModel( b.getModel() );
            }
        else
            {
            b = new EnhancedButton( a );
            }

        if( a instanceof ActionList )
            {
            b.setMargin( ZERO_INSETS );
            b.setRolloverEnabled( true );

            ActionListButton popupButton = new ActionListButton( b, (ActionList)a );
            popupButton.setMargin( ZERO_INSETS );
            popupButton.setRolloverEnabled( true );

            return( ActionListButton.createCombinedComponent( popupButton ) );
            }
        else
            {
            b.setMargin( ZERO_INSETS );
            b.setRolloverEnabled( true );

            return( b );
            }
    }


     *  Utility method to clone the specified action if it is cloneable.
     *  Otherwise, the original action is returned.
    public static Action cloneAction( Action o )
    {
        try
            {
            // Clone the object as appropriate
            if( o instanceof ActionList )
                {
                return( (ActionList)((ActionList)o).clone() );
                }
            else if( o instanceof Cloneable )
                {
                // Need to use reflection because normally clone() is
                // protected.
                Inspector ins = new Inspector( o );
                if( ins.hasMethod( "clone" ) )
                    {
                    return( (Action)ins.callMethod( "clone" ) );
                    }
                // It may be Cloneable, but it isn't cloneable.
                }
            else
                {
                // If the object is view context aware then it could be a mistake
                // that it isn't cloneable.  For now, we'll kick out a warning.
                if( o instanceof ViewContextAware )
                    {
                    log.warn( "Object is not cloneable but is context aware:" + o
                                + " This is usually a mistake." );
                    }
                }
            }
        catch( CloneNotSupportedException e )
            {
            log.warn( "Cloning object:" + o + " failed", e );
            }

        return( o );
    }

     *  Added to an ActionList to keep a JMenu in synch with the
     *  actions in the list.
    private static class ActionMenuCoordinator implements PropertyChangeListener
    {
        private JComponent menu;

        public ActionMenuCoordinator( JComponent menu )
        {
            this.menu = menu;
        }

        protected void addMenu( int index, Action a )
        {
            if( menu instanceof JMenu )
                {
                if( a == null )
                    {
                    ((JMenu)menu).add( new JSeparator(), index );
                    }
                else
                    {
                    ((JMenu)menu).add( createActionMenuItem( a ), index );
                    }
                }
            else if( menu instanceof JPopupMenu )
                {
                if( a == null )
                    {
                    ((JPopupMenu)menu).add( new JSeparator(), index );
                    }
                else
                    {
                    ((JPopupMenu)menu).add( createActionMenuItem( a ), index );
                    }
                }
            else if( menu instanceof JMenuBar )
                {
                if( a == null )
                    {
                    ((JMenuBar)menu).add( new JSeparator( SwingConstants.VERTICAL ), index );
                    }
                else
                    {
                    ((JMenuBar)menu).add( createActionMenuItem( a ), index );
                    }
                }
        }

        protected void replaceMenu( int index, Action a )
        {
            removeMenu( index );
            addMenu( index, a );
        }

        protected void removeMenu( int index )
        {
            menu.remove( index );
        }

        public void propertyChange( PropertyChangeEvent event )
        {
            if( !(event instanceof ListPropertyChangeEvent) )
                return;

            ActionList actions = (ActionList)event.getSource();

            ListPropertyChangeEvent lce = (ListPropertyChangeEvent)event;
            switch( lce.getType() )
                {
                case ListPropertyChangeEvent.INSERT:
                    for( int i = lce.getFirstIndex(); i <= lce.getLastIndex(); i++ )
                        {
                        addMenu( i, (Action)actions.get( i ) );
                        }
                    break;
                case ListPropertyChangeEvent.UPDATE:
                    for( int i = lce.getFirstIndex(); i <= lce.getLastIndex(); i++ )
                        {
                        replaceMenu( i, (Action)actions.get( i ) );
                        }
                    break;
                case ListPropertyChangeEvent.DELETE:
                    // Have to go backwards since the list indices
                    // won't work after we've removed earlier items.
                    for( int i = lce.getLastIndex(); i >= lce.getFirstIndex(); i-- )
                        {
                        removeMenu( i );
                        }
                    break;
                }
        }
    }

     *  Added to an ActionList to keep a JToolBar in synch with the
     *  actions in the list.
    private static class ActionToolBarCoordinator extends ListPropertyChangeListener
    {
        private JToolBar tools;

        public ActionToolBarCoordinator( JToolBar tools )
        {
            this.tools = tools;
        }

        public void addTool( int index, Action a )
        {
            if( a == null )
                {
                JToolBar.Separator s = new JToolBar.Separator(null);
                if( tools.getOrientation() == JToolBar.VERTICAL )
                    s.setOrientation( JSeparator.HORIZONTAL );
                else
                    s.setOrientation( JSeparator.VERTICAL );
                tools.add( s, index );
                return;
                }

            tools.add( createToolBarButton(a), index );
        }

        public void removeTool( int index )
        {
            tools.remove( index );
        }

        protected void itemInserted( Object source, int index, List oldList, List newList )
        {
            Action a = (Action)newList.get( index );
            addTool( index, a );
        }

        protected void itemUpdated( Object source, int index, List oldList, List newList )
        {
            itemDeleted( source, index, oldList, newList );
            itemInserted( source, index, oldList, newList );
        }

        protected void itemDeleted( Object source, int index, List oldList, List newList )
        {
            removeTool( index );
        }
    }

}*/

