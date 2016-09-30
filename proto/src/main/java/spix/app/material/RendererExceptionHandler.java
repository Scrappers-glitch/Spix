package spix.app.material;

import com.jme3.renderer.opengl.GLRenderer;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
