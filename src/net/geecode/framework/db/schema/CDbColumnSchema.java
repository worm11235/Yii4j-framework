/**
 * CDbColumnSchema class file.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @link http://www.yiiframework.com/
 * @copyright 2008-2013 Yii Software LLC
 * @license http://www.yiiframework.com/license/
 */
package net.geecode.framework.db.schema;

import java.util.Arrays;

import net.geecode.framework.base.CComponent;
import static net.geecode.php.base.Global.*;


/**
 * CDbColumnSchema class describes the column meta data of a database table.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.db.schema
 * @since 1.0
 */
public class CDbColumnSchema extends CComponent
{
    /**
     * @var string name of this column (without quotes).
     */
    public String name;
    /**
     * @var string raw name of this column. This is the quoted name that can be used in SQL queries.
     */
    public String rawName;
    /**
     * @var boolean whether this column can be null.
     */
    public boolean allowNull;
    /**
     * @var string the DB type of this column.
     */
    public String dbType;
    /**
     * @var string the PHP type of this column.
     */
    public String type;
    /**
     * @var mixed default value of this column
     */
    public Object defaultValue;
    /**
     * @var integer size of the column.
     */
    public int size;
    /**
     * @var integer precision of the column data, if it is numeric.
     */
    public int precision;
    /**
     * @var integer scale of the column data, if it is numeric.
     */
    public int scale;
    /**
     * @var boolean whether this column is a primary key
     */
    public boolean isPrimaryKey;
    /**
     * @var boolean whether this column is a foreign key
     */
    public boolean isForeignKey;
    /**
     * @var boolean whether this column is auto-incremental
     * @since 1.1.7
     */
    public boolean autoIncrement=false;
    /**
     * @var string comment of this column. Default value is empty string which means that no comment
     * has been set for the column. Null value means that RDBMS does not support column comments
     * at all (SQLite) or comment retrieval for the active RDBMS is not yet supported by the framework.
     * @since 1.1.13
     */
    public String comment = "";

    /**
     * Initializes the column with its DB type and default value.
     * This sets up the column's PHP type, size, precision, scale as well as default value.
     * @param string $dbType the column's DB type
     * @param mixed $defaultValue the default value
     */
    public void init(String dbType, Object defaultValue)
    {
        this.dbType = dbType;
        this.extractType(dbType);
        this.extractLimit(dbType);
        if(defaultValue != null)
            this.extractDefault(defaultValue);
    }

    /**
     * Extracts the PHP type from DB type.
     * @param string $dbType DB type
     */
    protected void extractType(String dbType)
    {
        if(stripos(dbType,"int") != -1 && stripos(dbType,"unsigned int") == -1)
            this.type="integer";
        else if(stripos(dbType,"bool") != -1)
            this.type="boolean";
        else if(preg_match("/(real|floa|doub)/i", dbType))
            this.type = "double";
        else
            this.type = "string";
    }

    /**
     * Extracts size, precision and scale information from column's DB type.
     * @param string $dbType the column's DB type
     */
    protected void extractLimit(String dbType)
    {
        String[] matches;
        if(strpos(dbType, "(") && preg_match("/\\((.*)\\)/", dbType, matches))
        {
            String[] values = explode(",", matches[1]);
            this.size = this.precision = Integer.parseInt(values[0]);
            if(isset(Arrays.asList(values), 1))
                this.scale = Integer.parseInt(values[1]);
        }
    }

    /**
     * Extracts the default value for the column.
     * The value is typecasted to correct PHP type.
     * @param mixed $defaultValue the default value obtained from metadata
     */
    protected void extractDefault(Object defaultValue)
    {
        this.defaultValue = this.typecast(defaultValue);
    }

    /**
     * Converts the input value to the type that this column is of.
     * @param mixed $value input value
     * @return mixed converted value
     */
    public Object typecast(Object value)
    {
        if((value.getClass().getName().equalsIgnoreCase(this.type)) || value == null
                || value instanceof CDbExpression)
            return value;
        if(value.equals("") && this.allowNull)
            return this.type.equals("string") ? "" : null;
        switch(this.type)
        {
            case "string": return (String)value;
            case "integer": return (Integer)value;
            case "boolean": return (Boolean)value;
            case "double":
            default: return value;
        }
    }
}

