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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 *
 *
 *  @author    Paul Speed
 */
public class RollupPanel extends JPanel {
 
    private Action action = new OpenCloseAction();
    private boolean open = true;
    
    private JButton titleButton;
    private Component component;   
    
    public RollupPanel() {
        this(null, null, null);
    }

    public RollupPanel(String title, Component component, Icon icon) {
        setLayout(new BorderLayout());

        titleButton = new JButton(action);
        titleButton.setHorizontalAlignment(SwingConstants.LEFT);
        titleButton.setIcon(icon);
        titleButton.setBackground(new Color(25, 25, 50));

        add(titleButton, BorderLayout.NORTH);
        
        setTitle(title);
        setComponent(component);
    }    

    public void setComponent( Component component ) {
        if( this.component == component ) {
            return;
        }
        if( this.component != null ) {
            remove(this.component);
        }
        this.component = component;
        if( this.component != null && isOpen() ) {
            add(component, BorderLayout.CENTER);
        }
    }
    
    public Component getComponent() {
        return component;
    }

    public boolean isOpen() {
        return open;
    }
    
    public void setOpen( boolean open ) {
        if( this.open == open ) {
            return;
        }
        this.open = open;
        if( open ) {
            remove(component); // just make double sure we don't add it twice
            add(component, BorderLayout.CENTER);
        } else {
            remove(component);
        }
        revalidate();
        repaint();
    }

    public void toggle() {
        setOpen(!isOpen());
    }

    public void setTitle( String title ) {
        action.putValue(Action.NAME, title);
    }
    
    public String getTitle() {
        return String.valueOf(action.getValue(Action.NAME));
    }
    
    private class OpenCloseAction extends AbstractAction { 
        public OpenCloseAction() {            
        }
        
        public void actionPerformed( ActionEvent event ) {
            toggle();
        }       
    }
}


/*
public class RolloutPanel extends JPanel
{
    static final long serialVersionUID = 1;

    private static final Dimension ZERO_SIZE = new Dimension( 0, 0 );

    private static Icon openIcon;
    private static Icon closeIcon;
    static
    {
        try
            {
            // Lookup the open and close icons
            openIcon = new ImageIcon( RolloutPanel.class.getResource( "plus.png" ) );
            closeIcon = new ImageIcon( RolloutPanel.class.getResource( "minus.png" ) );
            }
        catch( Exception e )
            {
            throw new RuntimeException( "Error loading open/close icons", e );
            }
    }

    private String title;

    private EnhancedButton titleButton;

    private JComponent panel;

    private Font smaller;

    private boolean open = true;

    private java.util.List<ChangeListener> rolloutListeners = new ArrayList<ChangeListener>();
    private ChangeEvent changeEvent = new ChangeEvent( this );

    public RolloutPanel()
    {
        this( null, null );
    }

    public RolloutPanel( String title, JComponent panel )
    {
        this.title = title;
        this.panel = panel;

        buildContents();
    }

    public void addChangeListener( ChangeListener listener )
    {
        rolloutListeners.add( listener );
    }

    public void removeChangeListener( ChangeListener listener )
    {
        rolloutListeners.remove( listener );
    }

    protected void fireChangeEvent()
    {
        ChangeListener[] list = new ChangeListener[ rolloutListeners.size() ];
        list = (ChangeListener[])rolloutListeners.toArray( list );

        for( int i = 0; i < list.length; i++ )
            {
            list[i].stateChanged( changeEvent );
            }
    }

    public void setTitle( String title )
    {
        titleButton.setText( title );
    }

    public String getTitle()
    {
        return( title );
    }

    public void setRolloutPanel( JComponent panel )
    {
        if( this.panel != null )
            {
            // Remove the old panel
            remove( this.panel );
            }

        this.panel = panel;
        if( panel != null )
            {
            // Set the new panel
            add( panel, "Center" );
            panel.setFont( smaller );
            }
    }

    public JComponent getRolloutPanel()
    {
        return( panel );
    }

    public void openRollout()
    {
        if( open )
            return;

        titleButton.setIcon( closeIcon );
        add( panel, "Center" );
        open = true;
        revalidate();
        fireChangeEvent();
        repaint();
        //panel.setSize( panel.getPreferredSize() );
        //open = true;
        //revalidate();
    }

    public void closeRollout()
    {
        if( !open )
            return;

        titleButton.setIcon( openIcon );
        remove( panel );
        open = false;
        revalidate();
        fireChangeEvent();
        repaint();
        //panel.setSize( ZERO_SIZE );
        //open = false;
        //revalidate();
    }

    public Dimension getPreferredSize()
    {
        if( open )
            return( super.getPreferredSize() );

        Dimension d1 = titleButton.getPreferredSize();
        Dimension d2 = panel.getPreferredSize();

        return( new Dimension( d2.width, d1.height ) );
    }

    public Dimension getMinimumSize()
    {
        if( open )
            return( super.getMinimumSize() );

        Dimension d1 = titleButton.getMinimumSize();
        Dimension d2 = panel.getMinimumSize();

        return( new Dimension( d2.width, d1.height ) );
    }

    protected void buildContents()
    {
        setLayout( new BorderLayout() );

        titleButton = new EnhancedButton( title );
        titleButton.setAlwaysPaintBorder( true );
        titleButton.setMargin( new Insets( 0, 0, 0, 0 ) );
        titleButton.addActionListener( new ButtonListener() );
        titleButton.setIcon( closeIcon );
        titleButton.setHorizontalTextPosition( SwingConstants.LEADING );

        //Font f = titleButton.getFont();
        //smaller = new Font( f.getFontName(), f.getStyle(), f.getSize() - 2 );
        //titleButton.setFont( smaller );
       // setFont( smaller );

        add( titleButton, "North" );

        if( panel != null )
            {
            add( panel, "Center" );
            panel.setFont( smaller );
            }
    }

    private class ButtonListener implements ActionListener
    {
        public void actionPerformed( ActionEvent event )
        {
            if( event.getSource() == titleButton )
                {
                if( open )
                    closeRollout();
                else
                    openRollout();
                }
        }
    }
}*/
