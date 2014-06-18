/**
 * 
 */
package net.geecode.php.base;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David
 *
 */
public class CArray
{
    private Map<String, Object> map = new HashMap<String, Object>();
    
    private int cursor = 0;
    
    public Object push(Object value)
    {
        return map.put(++cursor + "", value);
    }
    
    public Object put(Object k, Object v)
    {
        if (k instanceof Number)
        {
            
        }
        return map.put(k + "", v);
    }
    
    public Object get(Object k)
    {
        return map.get(k);
    }
}
