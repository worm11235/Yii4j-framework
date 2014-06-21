/**
 * 
 */
package net.geecode.framework.db.ar;

import static net.geecode.php.base.Global.array;
import static net.geecode.php.base.Global.get_class;
import static net.geecode.php.base.Global.is_array;
import static net.geecode.php.base.Global.is_string;
import static net.geecode.php.base.Global.isset;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.geecode.framework.db.CDbException;
import net.geecode.framework.db.schema.CDbColumnSchema;
import net.geecode.framework.db.schema.CDbTableSchema;
import net.geecode.framework.lite.Yii;

/**
 * CActiveRecordMetaData represents the meta-data for an Active Record class.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.db.ar
 * @since 1.0
 */
public class CActiveRecordMetaData
{
    /**
     * @var CDbTableSchema the table schema information
     */
    public CDbTableSchema tableSchema;
    /**
     * @var array table columns
     */
    public Map<String, CDbColumnSchema> columns;
    /**
     * @var array list of relations
     */
    public Map<String, CActiveRelation> relations = new HashMap<String, CActiveRelation>();
    /**
     * @var array attribute default values
     */
    public Map<String, Object> attributeDefaults=array();

    private String _modelClassName;

    /**
     * Constructor.
     * @param CActiveRecord $model the model instance
     * @throws CDbException if specified table for active record class cannot be found in the database
     */
    public CActiveRecordMetaData (CActiveRecord model)
    {
        this._modelClassName = get_class(model);

        String tableName = model.tableName();
        CDbTableSchema table = model.getDbConnection().getSchema().getTable(tableName, false);
        if(table == null)
            throw new CDbException(Yii.t("yii","The table \"{table}\" for active record class \"{class}\" cannot be found in the database.",
                array("{class}", this._modelClassName, "{table}", tableName)));
        if(table.primaryKey== null)
        {
            table.primaryKey = model.primaryKey();
//            if(is_string(table.primaryKey) && isset(table.columns, table.primaryKey))
//                table.columns.get(table.primaryKey).isPrimaryKey=true;
//            else if(is_array(table.primaryKey))
            {
                for(String name : table.primaryKey/* as $name*/)
                {
                    if(isset(table.columns, name))
                        table.columns.get(name).isPrimaryKey=true;
                }
            }
        }
        this.tableSchema = table;
        this.columns = table.columns;

        for (Entry<String, CDbColumnSchema> e : table.columns.entrySet()/* as $name=>$column*/)
        {
            String name = e.getKey();
            CDbColumnSchema column = e.getValue();
            if(!column.isPrimaryKey && column.defaultValue != null)
                this.attributeDefaults.put(name, column.defaultValue);
        }

        for(Entry<String, Object> e : model.relations().entrySet()/* as $name=>$config*/)
        {
            String name = e.getKey();
            Object config = e.getValue();
            this.addRelation(name, (Map<String, Object>) config);
        }
    }

    /**
     * Adds a relation.
     *
     * $config is an array with three elements:
     * relation type, the related active record class and the foreign key.
     *
     * @throws CDbException
     * @param string $name $name Name of the relation.
     * @param array $config $config Relation parameters.
     * @return void
     * @since 1.1.2
     */
    public void addRelation(String name, Map<String, Object> config)
    {
        if(isset(config, 0) && isset(config, 1) && isset(config, 2))  // relation class, AR class, FK
            this.relations.put(name, new config[0](name, config[1], config[2], array_slice(config,3));
        else
            throw new CDbException(Yii.t("yii","Active record \"{class}\" has an invalid configuration for relation"
                    + " \"{relation}\". It must specify the relation type, the related active record class and the "
                    + "foreign key.",
                    array("{class}", this._modelClassName, "{relation}", name)));
    }

    /**
     * Checks if there is a relation with specified name defined.
     *
     * @param string $name $name Name of the relation.
     * @return boolean
     * @since 1.1.2
     */
    public boolean hasRelation(String name)
    {
        return isset(this.relations, name);
    }

    /**
     * Deletes a relation with specified name.
     *
     * @param string $name $name
     * @return void
     * @since 1.1.2
     */
    public void removeRelation(String name)
    {
        this.relations.remove(name);
    }
}
