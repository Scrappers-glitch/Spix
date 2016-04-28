package spix.app.properties;

import com.jme3.math.*;
import com.jme3.scene.Spatial;
import spix.props.*;
import spix.type.Type;

import java.beans.*;

public class WorldRotationProperty extends AbstractProperty {
    private final Spatial spatial;
    private final Property localRotation;
    private boolean updating = false;
    private final Quaternion lastWorld = new Quaternion();

    public WorldRotationProperty(Spatial spatial, Property localRotation ) {
        super("worldRotation");
        this.spatial = spatial;
        this.localRotation = localRotation;
        if( localRotation != null ) {
            // Add a listener... we're pretty sure we won't have to release
            // this listener later because localTranslation is a sibling property.
            // When it gets GC'ed then so will we.  Otherwise we'd have some life
            // cycle issues to deal with and a central event dispatcher would be
            // a better idea for this.
            localRotation.addPropertyChangeListener(new LocalRotationObserver());
        }
    }

    public Type getType() {
        return new Type(Quaternion.class);
    }

    public void setValue( Object value ) {
        if( value == null ) {
            return;
        }

        Quaternion q = (Quaternion)value;
        Quaternion last = spatial.getWorldRotation().clone();
        if( q.equals(last) ) {
            return;
        }

        Quaternion delta;
        if( spatial.getParent() != null ) {
            Quaternion local = spatial.getParent().getWorldRotation().inverse().multLocal(q);
            delta = spatial.getLocalRotation().inverse().multLocal(local);
        } else {
            // It is the root... so everything is already in world space
            delta = spatial.getLocalRotation().inverse().multLocal(q);
        }

        //...and that should be how far we need to rotate it
        spatial.rotate(delta);
        
        firePropertyChange(last, spatial.getWorldRotation(), true);
        
        // Make sure the local rotation matches
        if( localRotation != null ) {
            updating = true;
            try {
                localRotation.setValue(spatial.getLocalRotation());
                lastWorld.set(spatial.getWorldRotation());
            } finally {
                updating = false;
            }
        }
    }

    protected void updateFromLocal( Quaternion old, Quaternion local ) {
 
        // Avoid reacting to our own updates
        if( updating ) {
            return;
        }
    
        // Else fire the change
        firePropertyChange(lastWorld, spatial.getWorldRotation(), true);
        lastWorld.set(spatial.getWorldRotation());
    }

    public Object getValue() {
        return spatial.getWorldRotation();
    }
    
    private class LocalRotationObserver implements PropertyChangeListener {
        public void propertyChange( PropertyChangeEvent event ) {
            updateFromLocal((Quaternion)event.getOldValue(), (Quaternion)event.getNewValue());
        }
    }
}