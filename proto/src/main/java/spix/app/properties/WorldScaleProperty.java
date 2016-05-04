package spix.app.properties;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import spix.props.*;
import spix.type.Type;

import java.beans.*;

public class WorldScaleProperty extends AbstractProperty {
    private final Spatial spatial;
    private final Property localScale;
    private boolean updating = false;
    private final Vector3f lastWorld = new Vector3f();
    private final boolean allowNonUniformScale;

    public WorldScaleProperty(Spatial spatial, Property localScale, boolean allowNonUniformScale) {
        super("worldScale");
        this.spatial = spatial;
        this.localScale = localScale;
        this.allowNonUniformScale = allowNonUniformScale;
        if( localScale != null ) {
            // Add a listener... we're pretty sure we won't have to release
            // this listener later because localScale is a sibling property.
            // When it gets GC'ed then so will we.  Otherwise we'd have some life
            // cycle issues to deal with and a central event dispatcher would be
            // a better idea for this.
            localScale.addPropertyChangeListener(new LocalScaleObserver());
        }
    }

    public WorldScaleProperty(Spatial spatial, Property localScale ) {
        this(spatial,localScale,true);
    }

    public Type getType() {
        return new Type(Vector3f.class);
    }

    public void setValue( Object value ) {
        if( value == null ) {
            return;
        }

        Vector3f v = (Vector3f)value;
        Vector3f last = spatial.getWorldScale().clone();
        if( v.equals(last) ) {
            return;
        }

        //computing the delta from parent scale.
        Vector3f delta;
        if( spatial.getParent() != null ) {
            Vector3f local = v.divide(spatial.getParent().getWorldScale());
            delta = local.divide(spatial.getLocalScale());
        } else {
            // It is the root... so everything is already in world space
            delta = v.divide(spatial.getLocalScale());
        }

        //...and that should be how far we need to scale it
        spatial.scale(delta.getX(),delta.getY(),delta.getZ());
        
        firePropertyChange(last, spatial.getWorldScale(), true);
        
        // Make sure the local scales matches
        if( localScale != null ) {
            updating = true;
            try {            
                localScale.setValue(spatial.getLocalScale());
                lastWorld.set(spatial.getWorldScale());
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
        firePropertyChange(lastWorld, spatial.getWorldScale(), true);
        lastWorld.set(spatial.getWorldScale());
    }

    public Object getValue() {
        return spatial.getWorldScale();
    }
    
    private class LocalScaleObserver implements PropertyChangeListener {
        public void propertyChange( PropertyChangeEvent event ) {
            updateFromLocal((Vector3f)event.getOldValue(), (Vector3f)event.getNewValue());  
        }
    }

    public boolean isAllowNonUniformScale() {
        return allowNonUniformScale;
    }
}