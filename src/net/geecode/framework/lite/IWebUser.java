/**
 * 
 */
package net.geecode.framework.lite;

/**
 * @author David
 *
 */
interface IWebUser
{
    int getId();
    String getName();
    boolean getIsGuest();
    boolean checkAccess(String operation, Object ... params/*=array()*/);
    boolean loginRequired();
}