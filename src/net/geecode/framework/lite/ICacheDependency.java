/**
 * 
 */
package net.geecode.framework.lite;

/**
 * @author worm
 *
 */

public interface ICacheDependency
{
    Object evaluateDependency();
    boolean getHasChanged();
}
