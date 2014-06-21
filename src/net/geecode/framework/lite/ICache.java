/**
 * 
 */
package net.geecode.framework.lite;

/**
 * @author worm
 *
 */

public interface ICache
{
    Object get(String id);
    Object mget(String[] ids);
    void set(String id, Object value, long expire/*=0*/, ICacheDependency dependency/*=null*/);
    void set(String id, Object value, long expire/*=0*/);
    void set(String id, Object value);
    void add(String id, Object value, long expire/*=0*/, ICacheDependency dependency/*=null*/);
    void add(String id, Object value, long expire/*=0*/);
    void add(String id, Object value);
    void delete(String id);
    void flush();
}