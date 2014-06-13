/**
 * 
 */
package net.geecode.framework.base;

/**
 * @author David
 *
 */
public class CException extends RuntimeException
{

    /**
     * 
     */
    private static final long serialVersionUID = 8843725733778137711L;
    
    public CException(){}
    
    public CException(String str)
    {
        super(str);
    }

}
