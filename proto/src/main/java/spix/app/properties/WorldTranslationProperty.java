package spix.app.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import spix.props.AbstractProperty;
import spix.props.Property;
import spix.type.Type;

public class WorldTranslationProperty extends AbstractProperty {
    private final Spatial spatial;
    private final Property localTranslation;
    private boolean updating = false;
    private final Vector3f lastWorld = new Vector3f();

    public WorldTranslationProperty( Spatial spatial, Property localTranslation ) {
        super("worldTranslation");
        this.spatial = spatial;
        this.localTranslation = localTranslation;
        if( localTranslation != null ) {
            // Add a listener... we're pretty sure we won't have to release
            // this listener later because localTranslation is a sibling property.
            // When it gets GC'ed then so will we.  Otherwise we'd have some life
            // cycle issues to deal with and a central event dispatcher would be
            // a better idea for this.
            localTranslation.addPropertyChangeListener(new LocalTranslationObserver());
        }
        lastWorld.set(spatial.getWorldTranslation());
    }

    public Type getType() {
        return new Type(Vector3f.class);
    }

    public void setValue( Object value ) {
        if( value == null ) {
            return;
        }

        Vector3f v = (Vector3f)value;
        Vector3f last = lastWorld; //spatial.getWorldTranslation().clone();
        if( v.equals(last) ) {
            return;
        }

        // Else see how we'd have to move the spatial to get it
        // to the specified world translation... so find the point
        // in the parent's local space and calculate a delta from
        // the child's currentl location.
        Vector3f delta;
        if( spatial.getParent() != null ) {
            Vector3f local = spatial.getParent().worldToLocal(v, null);
            delta = local.subtract(spatial.getLocalTranslation());
        } else {
            // It is the root... so everything is already in world space
            delta = v.subtract(spatial.getLocalTranslation());
        }

        //...and that should be how far we need to move it
        spatial.move(delta);
        
        firePropertyChange(last, spatial.getWorldTranslation(), true);
        
        lastWorld.set(spatial.getWorldTranslation());
        
        // Make sure the local translation matches
        if( localTranslation != null ) {
            updating = true;
            try {
                localTranslation.setValue(spatial.getLocalTranslation());
                //lastWorld.set(spatial.getWorldTranslation());
            } finally {
                updating = false;
            }
        }
    }

    protected void updateFromLocal( Vector3f old, Vector3f local ) {
 
        // Avoid reacting to our own updates
        if( updating ) {
            return;
        }
    
        // Else fire the change
        firePropertyChange(lastWorld, spatial.getWorldTranslation(), true);
        lastWorld.set(spatial.getWorldTranslation());          
    }

    public Object getValue() {
        return spatial.getWorldTranslation();
    }
    
    private class LocalTranslationObserver implements PropertyChangeListener {
        public void propertyChange( PropertyChangeEvent event ) {
            updateFromLocal((Vector3f)event.getOldValue(), (Vector3f)event.getNewValue());  
        }
    }
}