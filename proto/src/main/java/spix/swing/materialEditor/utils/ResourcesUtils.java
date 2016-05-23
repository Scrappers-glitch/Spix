package spix.swing.materialEditor.utils;

import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.util.*;

/**
 * Created by Nehon on 23/05/2016.
 */
public class ResourcesUtils {

    public static List<String> getShaderNodeDefinitionsFromClassPath() throws IOException {
        ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
        List<String> res = new ArrayList<>();

        for (ClassPath.ResourceInfo resourceInfo : classPath.getResources()) {
            if(resourceInfo.getResourceName().endsWith(".j3sn") && resourceInfo.getResourceName().startsWith("Common")){
                res.add(resourceInfo.getResourceName());
            }
        }
        return res;
    }

}
