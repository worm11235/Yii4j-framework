/**
 * 
 * CDbException class file.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @link http://www.yiiframework.com/
 * @copyright 2008-2013 Yii Software LLC
 * @license http://www.yiiframework.com/license/
 */
package net.geecode.framework.db;

import net.geecode.framework.base.CException;

/**
 * CDbException represents an exception that is caused by some DB-related operations.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.db
 * @since 1.0
 */
public class CDbException extends CException
{
    /**
     * 
     */
    private static final long serialVersionUID = 112480210661535623L;
    
    /**
     * @var mixed the error info provided by a PDO exception. This is the same as returned
     * by {@link http://www.php.net/manual/en/pdo.errorinfo.php PDO::errorInfo}.
     * @since 1.1.4
     */
    public Object errorInfo;

    /**
     * Constructor.
     * @param string $message PDO error message
     * @param integer $code PDO error code
     * @param mixed $errorInfo PDO error info
     */
    public CDbException (String message, int code/*=0*/, Object errorInfo/*=null*/)
    {
        super(message, code);
        this.errorInfo = errorInfo;
    }
    public CDbException (String message, int code/*=0*/)
    {
        this(message, code, null);
    }
    public CDbException (String message)
    {
        this(message, 0);
    }
}
