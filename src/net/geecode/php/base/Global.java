/**
 * 
 */
package net.geecode.php.base;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
    
    public static final int PREG_SPLIT_NO_EMPTY = 1;
    
    public static Map<String, Object> $_SERVER;
    
    public static boolean isset(Collection c, Object obj)
    {
        return c.contains(obj);
    }
    
    public static boolean isset(List c, int obj)
    {
        return c.size() > obj && null != c.get(obj);
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
    
    public static String str_replace(List<String> find, String rp, String str)
    {
        for (String sf : find)
        {
            str = str.replaceAll(sf, rp);
        }
        return str;
    }
    
    public static String preg_replace (String find, String rp, String str)
    {
        return str.replaceAll(find, rp);
    }
    
    public static boolean preg_match(String regex, String str)
    {
        return str.matches(regex);
    }
    
    public static String trim(String str)
    {
        return str.trim();
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
    
    public static String[] preg_split(String regex, String str, int limit, int flag)
    {
        return str.split(regex, limit);
    }
    
    public static String implode(String insert, Collection col)
    {
        String str = "";
        for (Object ojb : col)
        {
            str += ojb + insert;
        }
        if (str.length() > insert.length())
        {
            str = str.substring(0, str.length() - insert.length() - 1);
        }
        
        return str;
    }
    
    public static String implode(String insert, Object[] col)
    {
        return implode(insert, Arrays.asList(col));
    }
    
    public static String implode(String insert, Map<String, Object> col)
    {
        return implode(insert, col.values());
    }
    
    public static String strtr(String str, String sch, String rpl)
    {
        return str.replaceAll(sch, rpl);
    }
    
    public static String strtr(String str, Map<String, Object> rpl)
    {
        for (Entry<String, Object> et : rpl.entrySet())
        {
            str.replaceAll(et.getKey(), et.getValue() + "");
        }
        return str;
    }
    
    public static String substr(String str, int start)
    {
        return str.substring(start);
    }
    
    public static String substr(String str, int start, int end)
    {
        return str.substring(start, end);
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
        return obj instanceof List;
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
    
    public static int stripos(String org, String find)
    {
        return org.toLowerCase().indexOf(find.toLowerCase());
    }
    
    public static int stripos(String org, String find, int start)
    {
        return org.toLowerCase().indexOf(find.toLowerCase(), start);
    }
    
    public static int strrpos(String org, String find)
    {
        return org.lastIndexOf(find);
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
