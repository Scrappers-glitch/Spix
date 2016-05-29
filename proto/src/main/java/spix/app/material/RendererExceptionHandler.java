package spix.app.material;

import com.jme3.renderer.opengl.GLRenderer;
import com.sun.org.apache.bcel.internal.generic.IFLE;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

public class RendererExceptionHandler {

  private static Logger logger = Logger.getLogger(GLRenderer.class.getName());

  private static StringBuffer strHolder = new StringBuffer();

  public RendererExceptionHandler() {
    logger.addHandler(new Handler() {
      public void publish(LogRecord logRecord) {
        if (logRecord.getLevel() == Level.SEVERE || logRecord.getLevel() == Level.WARNING){
          String message = logRecord.getMessage();
          for (int i = 0; i < logRecord.getParameters().length; i++) {
            message =  message.replace("{"+i+"}", (String)logRecord.getParameters()[i]);
          }
          clear();
          strHolder.append(message);
        }
      }

      public void flush() {
      }

      public void close() {
      }
    });
  }

  public String getBuffer(){
    return strHolder.toString();
  }
  public void clear(){
    strHolder.delete(0, strHolder.capacity());
  }
}
