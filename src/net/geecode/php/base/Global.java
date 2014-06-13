/**
 * 
 */
package net.geecode.php.base;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David
 *
 */
public final class Global
{

    public static final long YII_BEGIN_TIME = System.currentTimeMillis()/*microtime(true)*/;
    public static final boolean YII_DEBUG = false;
    public static final int YII_TRACE_LEVEL = 0;
    public static final boolean YII_ENABLE_EXCEPTION_HANDLER = true;
    public static final boolean YII_ENABLE_ERROR_HANDLER = true;
    public static final String YII_PATH = /*dirname(__FILE__)*/"./";
    public static final String DIRECTORY_SEPARATOR = File.separator;
    public static final String YII_ZII_PATH = YII_PATH + DIRECTORY_SEPARATOR + "zii";
    public static Map<String, Object> $_SERVER;
    
    public static boolean isset(Collection c, Object obj)
    {
        return c.contains(obj);
    }
    
    public static boolean isset(Map c, Object key)
    {
        return c.containsKey(key);
    }
    
    public static boolean array_key_exists(Object key, Map map)
    {
        return map.containsKey(key);
    }
    
    public static String strtolower(String str)
    {
        return str.toLowerCase();
    }
    
    public static String str_replace(String find, String rp, String str)
    {
        return str.replaceAll(find, rp);
    }
    
    public static String rtrim(String str, String regex)
    {
        if (str.endsWith(regex))
        {
            return rtrim(str.substring(0, str.lastIndexOf(regex)), regex);
        }
        return str;
    }
    
    public static String[] explode(String sp, String str)
    {
        return str.split(sp);
    }
    
    public static void echo(String str)
    {
        ;
    }
    
    public static Class get_class(Object obj)
    {
        return obj.getClass();
    }
    
    public static boolean is_dir(String path)
    {
        return new File(path).isDirectory();
    }
    
    public static String dirname(String path)
    {
        return new File(path).getParentFile().getPath();
    }
    
    public static boolean is_writable(String path)
    {
        return new File(path).canWrite();
    }
    
    public static boolean is_file(String path)
    {
        return new File(path).isFile();
    }
    
    public static String realpath(String path)
    {
        return new File(path).getAbsolutePath();
    }
    
    public static boolean method_exists(Object obj, String name)
    {
        for (Method m : obj.getClass().getDeclaredMethods())
        {
            if (m.getName().equals(name))
            {
                return true;
            }
        }
        return false;
    }
    
    public static int strncasecmp(String name, String str, int idx)
    {
        if (name.length() > idx)
        {
            name = name.substring(0, idx).toLowerCase();
        }
        if (str.length() > idx)
        {
            str = str.substring(0, idx).toLowerCase();
        }
        return name.compareTo(str);
    }
    
    public static boolean is_array(Object obj)
    {
        return obj.getClass().isArray();
    }
    
    public static Map<String, Object> array(Object ... params)
    {
        Map<String, Object> ret = new HashMap<String, Object>();
        
        String key = null;
        for (int idx = 0; idx < params.length; idx++)
        {
            if (null == key && params[idx] instanceof String)
            {
                key = (String) params[idx];
            }
            else
            {
                ret.put(key, params[idx]);
                key = null;
            }
        }
        
        return ret;
    }
    
    public static boolean is_string(Object obj)
    {
        return obj instanceof String;
    }
    
    public static int strpos(String org, String find)
    {
        return org.indexOf(find);
    }
    
    public static int strlen(String str)
    {
        return str.length();
    }
    
    //system functions
    public static void register_shutdown_function(Map map, int status, boolean bool)
    {
        //TODO
        ;
    }
    
    public static void exit(int retCode)
    {
        ;//TODO
    }
    
    public static String date_default_timezone_get()
    {
        return "";//TODO
    }
    
    public static void date_default_timezone_set(String value)
    {
        ;//TODO
    }
}
