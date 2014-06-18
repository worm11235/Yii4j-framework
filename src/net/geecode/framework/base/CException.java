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
    
    private int code;
    
    public CException(){}
    
    public CException(String str)
    {
        super(str);
    }
    
    public CException(String str, int errCode)
    {
        super(str);
        code = errCode;
    }

}
