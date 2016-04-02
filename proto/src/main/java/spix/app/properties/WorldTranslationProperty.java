package spix.app.properties;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import spix.props.AbstractProperty;
import spix.type.Type;

public class WorldTranslationProperty extends AbstractProperty {
        private final Spatial spatial;

        public WorldTranslationProperty( Spatial spatial ) {
            super("worldTranslation");
            this.spatial = spatial;
        }

        public Type getType() {
            return new Type(Vector3f.class);
        }

        public void setValue( Object value ) {
            if( value == null ) {
                return;
            }
            Vector3f v = (Vector3f)value;
            Vector3f last = spatial.getWorldTranslation().clone();
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
        }

        public Object getValue() {
            return spatial.getWorldTranslation();
        }
    }