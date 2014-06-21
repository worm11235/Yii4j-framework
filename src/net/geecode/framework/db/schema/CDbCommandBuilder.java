/**
 * CDbCommandBuilder class file.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @link http://www.yiiframework.com/
 * @copyright 2008-2013 Yii Software LLC
 * @license http://www.yiiframework.com/license/
 */
package net.geecode.framework.db.schema;

import java.util.Map;
import java.util.Map.Entry;

import net.geecode.framework.base.CComponent;
import net.geecode.framework.db.CDbCommand;
import net.geecode.framework.db.CDbException;
import net.geecode.framework.lite.CDbConnection;
import net.geecode.framework.lite.Yii;
import static net.geecode.php.base.Global.*;

/**
 * CDbCommandBuilder provides basic methods to create query commands for tables.
 *
 * @property CDbConnection $dbConnection Database connection.
 * @property CDbSchema $schema The schema for this command builder.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.db.schema
 * @since 1.0
 */
public class CDbCommandBuilder extends CComponent
{
    public static final String PARAM_PREFIX = ":yp";

    private CDbSchema _schema;
    private CDbConnection _connection;

    /**
     * @param CDbSchema $schema the schema for this command builder
     */
    public CDbCommandBuilder(CDbSchema schema)
    {
        this._schema = schema;
        this._connection = schema.getDbConnection();
    }

    /**
     * @return CDbConnection database connection.
     */
    public CDbConnection getDbConnection()
    {
        return this._connection;
    }

    /**
     * @return CDbSchema the schema for this command builder.
     */
    public CDbSchema getSchema()
    {
        return this._schema;
    }

    /**
     * Returns the last insertion ID for the specified table.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @return mixed last insertion id. Null is returned if no sequence name.
     */
    public Object getLastInsertID(Object table)
    {
        CDbTableSchema tab;
        if (table instanceof CDbTableSchema)
        {
            tab = (CDbTableSchema) table;
        }
        else
        {
            tab = this.ensureTable(table + "");
        }
        if (tab.sequenceName != null)
            return this._connection.getLastInsertID(tab.sequenceName);
        else
            return null;
    }

    /**
     * Creates a SELECT command for a single table.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @param CDbCriteria $criteria the query criteria
     * @param string $alias the alias name of the primary table. Defaults to 't'.
     * @return CDbCommand query command.
     */
    public CDbCommand createFindCommand(Object tab, CDbCriteria criteria,
            String alias/*='t'*/)
    {
        CDbTableSchema table;
        if (tab instanceof CDbTableSchema)
        {
            table = (CDbTableSchema) tab;
        }
        else
        {
            table = this.ensureTable(tab + "");
        }
        String select = implode(", ", criteria.select);
        if(criteria.alias != "")
            alias = criteria.alias;
        alias = this._schema.quoteTableName(alias);

        // issue 1432: need to expand * when SQL has JOIN
        if(select == "*" && !(criteria.join.isEmpty()))
        {
            String prefix = alias+".";
            select = array();
            for(table.getColumnNames() as name)
                $select[]=$prefix.this._schema.quoteColumnName($name);
            $select=implode(", ", select);
        }

        $sql=($criteria.distinct ? "SELECT DISTINCT":"SELECT")." {$select} FROM {$table->rawName} $alias";
        $sql=this.applyJoin($sql,$criteria.join);
        $sql=this.applyCondition($sql,$criteria.condition);
        $sql=this.applyGroup($sql,$criteria.group);
        $sql=this.applyHaving($sql,$criteria.having);
        $sql=this.applyOrder($sql,$criteria.order);
        $sql=this.applyLimit($sql,$criteria.limit,$criteria.offset);
        $command=this._connection.createCommand($sql);
        this.bindValues($command,$criteria.params);
        return $command;
    }

    /**
     * Creates a COUNT(*) command for a single table.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @param CDbCriteria $criteria the query criteria
     * @param string $alias the alias name of the primary table. Defaults to 't'.
     * @return CDbCommand query command.
     */
    public CDbCommand createCountCommand(Object table, CDbCriteria criteria,
            String alias/*='t'*/)
    {
        this.ensureTable(table);
        if(criteria.alias != "")
            alias = criteria.alias;
        alias = this._schema.quoteTableName(alias);

        if(!empty(criteria.group) || !empty(criteria.having))
        {
            select = is_array(criteria.select)
                    ? implode(", ", criteria.select) : criteria.select;
            if(criteria.alias != "")
                alias = criteria.alias;
            sql = (criteria.distinct ? "SELECT DISTINCT":"SELECT")+" {$select} FROM {$table->rawName} $alias";
            $sql=this.applyJoin($sql,$criteria.join);
            $sql=this.applyCondition($sql,$criteria.condition);
            $sql=this.applyGroup($sql,$criteria.group);
            $sql=this.applyHaving($sql,$criteria.having);
            $sql="SELECT COUNT(*) FROM ($sql) sq";
        }
        else
        {
            if(is_string($criteria.select) && stripos($criteria.select,"count")===0)
                $sql="SELECT ".$criteria.select;
            elseif($criteria.distinct)
            {
                if(is_array($table.primaryKey))
                {
                    $pk=array();
                    foreach($table.primaryKey as $key)
                        $pk[]=$alias.".".$key;
                    $pk=implode(", ",$pk);
                }
                else
                    $pk=$alias.".".$table.primaryKey;
                $sql="SELECT COUNT(DISTINCT $pk)";
            }
            else
                $sql="SELECT COUNT(*)";
            $sql.=" FROM {$table->rawName} $alias";
            $sql=this.applyJoin($sql,$criteria.join);
            $sql=this.applyCondition($sql,$criteria.condition);
        }

        // Suppress binding of parameters belonging to the ORDER clause. Issue #1407.
        if($criteria.order && $criteria.params)
        {
            $params1=array();
            preg_match_all("/(:\\w+)/",$sql,$params1);
            $params2=array();
            preg_match_all("/(:\\w+)/",this.applyOrder($sql,$criteria.order),$params2);
            foreach(array_diff($params2[0],$params1[0]) as $param)
                unset($criteria.params[$param]);
        }

        // Do the same for SELECT part.
        if($criteria.select && $criteria.params)
        {
            $params1=array();
            preg_match_all("/(:\\w+)/",$sql,$params1);
            $params2=array();
            preg_match_all("/(:\\w+)/",$sql." ".(is_array($criteria.select) ? implode(", ",$criteria.select) : $criteria.select),$params2);
            foreach(array_diff($params2[0],$params1[0]) as $param)
                unset($criteria.params[$param]);
        }

        $command=this._connection.createCommand($sql);
        this.bindValues($command,$criteria.params);
        return $command;
    }

    /**
     * Creates a DELETE command.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @param CDbCriteria $criteria the query criteria
     * @return CDbCommand delete command.
     */
    public CDbCommand createDeleteCommand(Object table, CDbCriteria criteria)
    {
        this.ensureTable(table);
        sql = "DELETE FROM {" + table.rawName + "}";
        sql = this.applyJoin(sql, criteria.join);
        sql=this.applyCondition(sql,criteria.condition);
        sql=this.applyGroup(sql,criteria.group);
        sql=this.applyHaving(sql,criteria.having);
        sql=this.applyOrder(sql,criteria.order);
        sql=this.applyLimit(sql,criteria.limit,criteria.offset);
        command=this._connection.createCommand(sql);
        this.bindValues($command,$criteria.params);
        return command;
    }

    /**
     * Creates an INSERT command.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @param array $data data to be inserted (column name=>column value). If a key is not a valid column name, the corresponding value will be ignored.
     * @return CDbCommand insert command
     */
    public CDbCommand createInsertCommand(Object table, Map<String, Object> data)
    {
        this.ensureTable(table);
        fields = array();
        values = array();
        placeholders = array();
        i = 0;
        for(data as $name=>$value)
        {
            if((column=table.getColumn(name)) != null
                    && (value != null || column.allowNull))
            {
                fields[] = column.rawName;
                if(value instanceof CDbExpression)
                {
                    placeholders[] = value.expression;
                    for(value.params as $n=>$v)
                        values[$n]=$v;
                }
                else
                {
                    placeholders[] = this.PARAM_PREFIX+$i;
                    values[this.PARAM_PREFIX+$i] = column.typecast(value);
                    i++;
                }
            }
        }
        if(fields == array())
        {
            pks=is_array($table.primaryKey) ? $table.primaryKey : array($table.primaryKey);
            for($pks as $pk)
            {
                $fields[]=$table.getColumn($pk).rawName;
                $placeholders[]=this.getIntegerPrimaryKeyDefaultValue();
            }
        }
        sql = "INSERT INTO {" + table.rawName + "} (" + implode(", ",fields)
                +") VALUES (" + implode(", ", placeholders)+")";
        command = this._connection.createCommand(sql);

        foreach($values as $name=>$value)
            command.bindValue($name,$value);

        return command;
    }

    /**
     * Creates a multiple INSERT command.
     * This method could be used to achieve better performance during insertion of the large
     * amount of data into the database tables.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @param array[] $data list data to be inserted, each value should be an array in format (column name=>column value).
     * If a key is not a valid column name, the corresponding value will be ignored.
     * @return CDbCommand multiple insert command
     * @since 1.1.14
     */
    public CDbCommand createMultipleInsertCommand(CDbTableSchema table,
            List<Map<String, Object>> data)
    {
        return this.composeMultipleInsertCommand(table, data);
    }
    
    public CDbCommand createMultipleInsertCommand(String table,
            List<Map<String, Object>> data)
    {
        return this.composeMultipleInsertCommand($table, data);
    }

    /**
     * Creates a multiple INSERT command.
     * This method compose the SQL expression via given part templates, providing ability to adjust
     * command for different SQL syntax.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @param array[] $data list data to be inserted, each value should be an array in format (column name=>column value).
     * If a key is not a valid column name, the corresponding value will be ignored.
     * @param array $templates templates for the SQL parts.
     * @return CDbCommand multiple insert command
     */
    protected CDbCommand composeMultipleInsertCommand(CDbTableSchema table,
            List<Map<String, Object>> data, Map<String, Object> templates)
    {
        templates=array_merge(
            array(
                "main", "INSERT INTO {{tableName}} ({{columnInsertNames}}) VALUES {{rowInsertValues}}",
                "columnInsertValue", "{{value}}",
                "columnInsertValueGlue", ", ",
                "rowInsertValue", "({{columnInsertValues}})",
                "rowInsertValueGlue", ", ",
                "columnInsertNameGlue", ", "
            ),
            templates
        );
        this.ensureTable(table);
        tableName = this.getDbConnection().quoteTableName(table.name);
        params = array();
        columnInsertNames = array();
        rowInsertValues = array();

        columns=array();
        for(Map<String, Object> rowData : data/* as $rowData*/)
        {
            for(Entry<String, Object> e : rowData.entrySet()
                    /* as $columnName=>$columnValue*/)
            {
                String columnName = e.getKey();
                Object columnValue = e.getValue();
                        
                if(!in_array(columnName, columns, true))
                    if(table.getColumn(columnName) != null)
                        columns[] = columnName;
            }
        }
        for($columns as $name)
            $columnInsertNames[$name]=this.getDbConnection().quoteColumnName($name);
        $columnInsertNamesSqlPart=implode($templates["columnInsertNameGlue"],$columnInsertNames);

        foreach($data as $rowKey=>$rowData)
        {
            $columnInsertValues=array();
            foreach($columns as $columnName)
            {
                $column=$table.getColumn($columnName);
                $columnValue=array_key_exists($columnName,$rowData) ? $rowData[$columnName] : new CDbExpression("NULL");
                if($columnValue instanceof CDbExpression)
                {
                    $columnInsertValue=$columnValue.expression;
                    foreach($columnValue.params as $columnValueParamName=>$columnValueParam)
                        $params[$columnValueParamName]=$columnValueParam;
                }
                else
                {
                    $columnInsertValue=":".$columnName."_".$rowKey;
                    $params[":".$columnName."_".$rowKey]=$column.typecast($columnValue);
                }
                $columnInsertValues[]=strtr($templates["columnInsertValue"],array(
                    "{{column}}", $columnInsertNames[$columnName],
                    "{{value}}", $columnInsertValue,
                ));
            }
            $rowInsertValues[]=strtr($templates["rowInsertValue"],array(
                "{{tableName}}", $tableName,
                "{{columnInsertNames}}", $columnInsertNamesSqlPart,
                "{{columnInsertValues}}", implode($templates["columnInsertValueGlue"],$columnInsertValues)
            ));
        }

        $sql=strtr($templates["main"],array(
            "{{tableName}}", $tableName,
            "{{columnInsertNames}}", $columnInsertNamesSqlPart,
            "{{rowInsertValues}}", implode($templates["rowInsertValueGlue"], $rowInsertValues),
        ));
        $command=this.getDbConnection().createCommand($sql);

        foreach($params as $name=>$value)
            $command.bindValue($name,$value);

        return $command;
    }

    /**
     * Creates an UPDATE command.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @param array $data list of columns to be updated (name=>value)
     * @param CDbCriteria $criteria the query criteria
     * @throws CDbException if no columns are being updated for the given table
     * @return CDbCommand update command.
     */
    public CDbCommand createUpdateCommand(CDbTableSchema table,
            Map<String, Object> data, CDbCriteria criteria)
    {
        fields = array();
        values = array();
        bindByPosition = isset(criteria.params, 0);
        i=0;
        for(Entry<String, Object> e : data.entrySet()
                /* as $name=>$value*/)
        {
            String name = e.getKey();
            Object value = e.getValue();
            
            if((column = table.getColumn(name)) != null)
            {
                if(value instanceof CDbExpression)
                {
                    fields[] = column.rawName+"="+value.expression;
                    for($value.params as $n=>$v)
                        $values[$n]=$v;
                }
                else if($bindByPosition)
                {
                    $fields[]=$column.rawName."=?";
                    $values[]=$column.typecast($value);
                }
                else
                {
                    $fields[]=$column.rawName."=".self::PARAM_PREFIX.$i;
                    $values[self::PARAM_PREFIX.$i]=$column.typecast($value);
                    $i++;
                }
            }
        }
        
        if($fields==array())
            throw new CDbException(Yii.t("yii", "No columns are being updated for table \"{table}\".",
                array("{table}", $table.name)));
        $sql="UPDATE {$table->rawName} SET ".implode(", ",$fields);
        $sql=this.applyJoin($sql,$criteria.join);
        $sql=this.applyCondition($sql,$criteria.condition);
        $sql=this.applyOrder($sql,$criteria.order);
        $sql=this.applyLimit($sql,$criteria.limit,$criteria.offset);

        $command=this._connection.createCommand($sql);
        this.bindValues($command,array_merge($values,$criteria.params));

        return command;
    }
    
    public CDbCommand createUpdateCommand(String table, Map<String, Object> data,
            CDbCriteria criteria)
    {
        return createUpdateCommand(this.ensureTable(table), data, criteria);
    }

    /**
     * Creates an UPDATE command that increments/decrements certain columns.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @param array $counters counters to be updated (counter increments/decrements indexed by column names.)
     * @param CDbCriteria $criteria the query criteria
     * @throws CDbException if no columns are being updated for the given table
     * @return CDbCommand the created command
     */
    public CDbCommand createUpdateCounterCommand(CDbTableSchema table,
            Map<String, Object> counters, CDbCriteria criteria)
    {
        this.ensureTable($table);
        $fields=array();
        foreach($counters as $name=>$value)
        {
            if(($column=$table.getColumn($name))!==null)
            {
                $value=(float)$value;
                if($value<0)
                    $fields[]="{$column->rawName}={$column->rawName}-".(-$value);
                else
                    $fields[]="{$column->rawName}={$column->rawName}+".$value;
            }
        }
        if($fields!==array())
        {
            $sql="UPDATE {$table->rawName} SET ".implode(", ",$fields);
            $sql=this.applyJoin($sql,$criteria.join);
            $sql=this.applyCondition($sql,$criteria.condition);
            $sql=this.applyOrder($sql,$criteria.order);
            $sql=this.applyLimit($sql,$criteria.limit,$criteria.offset);
            $command=this._connection.createCommand($sql);
            this.bindValues($command,$criteria.params);
            return $command;
        }
        else
            throw new CDbException(Yii::t("yii","No counter columns are being updated for table "{table}".",
                array("{table}", $table.name)));
    }
    public CDbCommand createUpdateCounterCommand(String table,
            Map<String, Object> counters, CDbCriteria criteria)
    {
        return createUpdateCounterCommand(this.ensureTable(table),
                counters, criteria);
    }

    /**
     * Creates a command based on a given SQL statement.
     * @param string $sql the explicitly specified SQL statement
     * @param array $params parameters that will be bound to the SQL statement
     * @return CDbCommand the created command
     */
    public CDbCommand createSqlCommand(String sql,
            Map<String, Object> params/*=array()*/)
    {
        command = this._connection.createCommand(sql);
        this.bindValues(command, params);
        return command;
    }

    /**
     * Alters the SQL to apply JOIN clause.
     * @param string $sql the SQL statement to be altered
     * @param string $join the JOIN clause (starting with join type, such as INNER JOIN)
     * @return string the altered SQL statement
     */
    public String applyJoin(String sql, String join)
    {
        if(join != "")
            return sql+" "+join;
        else
            return sql;
    }

    /**
     * Alters the SQL to apply WHERE clause.
     * @param string $sql the SQL statement without WHERE clause
     * @param string $condition the WHERE clause (without WHERE keyword)
     * @return string the altered SQL statement
     */
    public String applyCondition(String sql, String condition)
    {
        if(condition != "")
            return sql+" WHERE "+condition;
        else
            return sql;
    }

    /**
     * Alters the SQL to apply ORDER BY.
     * @param string $sql SQL statement without ORDER BY.
     * @param string $orderBy column ordering
     * @return string modified SQL applied with ORDER BY.
     */
    public String applyOrder(String sql, String orderBy)
    {
        if(orderBy!="")
            return sql+" ORDER BY "+orderBy;
        else
            return sql;
    }

    /**
     * Alters the SQL to apply LIMIT and OFFSET.
     * Default implementation is applicable for PostgreSQL, MySQL and SQLite.
     * @param string $sql SQL query string without LIMIT and OFFSET.
     * @param integer $limit maximum number of rows, -1 to ignore limit.
     * @param integer $offset row offset, -1 to ignore offset.
     * @return string SQL with LIMIT and OFFSET
     */
    public String applyLimit(String sql, int limit, int offset)
    {
        if(limit >= 0)
            sql += " LIMIT "+limit;
        if(offset > 0)
            sql+=" OFFSET "+offset;
        return sql;
    }

    /**
     * Alters the SQL to apply GROUP BY.
     * @param string $sql SQL query string without GROUP BY.
     * @param string $group GROUP BY
     * @return string SQL with GROUP BY.
     */
    public String applyGroup(String sql, String group)
    {
        if(group!="")
            return sql+" GROUP BY "+group;
        else
            return sql;
    }

    /**
     * Alters the SQL to apply HAVING.
     * @param string $sql SQL query string without HAVING
     * @param string $having HAVING
     * @return string SQL with HAVING
     */
    public String applyHaving(String sql, String having)
    {
        if (having != "")
            return sql+" HAVING "+having;
        else
            return sql;
    }

    /**
     * Binds parameter values for an SQL command.
     * @param CDbCommand $command database command
     * @param array $values values for binding (integer-indexed array for question mark placeholders, string-indexed array for named placeholders)
     */
    public void bindValues(CDbCommand command, Map values)
    {
        if((n = values.size()) == 0)
            return;
        if(isset(values, 0)) // question mark placeholders
        {
            for($i=0;$i<$n;++$i)
                command.bindValue($i+1,$values[$i]);
        }
        else // named placeholders
        {
            for($values as $name=>$value)
            {
                if($name[0]!==":")
                    $name=":".$name;
                $command.bindValue($name,$value);
            }
        }
    }

    /**
     * Creates a query criteria.
     * @param mixed $condition query condition or criteria.
     * If a string, it is treated as query condition (the WHERE clause);
     * If an array, it is treated as the initial values for constructing a {@link CDbCriteria} object;
     * Otherwise, it should be an instance of {@link CDbCriteria}.
     * @param array $params parameters to be bound to an SQL statement.
     * This is only used when the first parameter is a string (query condition).
     * In other cases, please use {@link CDbCriteria::params} to set parameters.
     * @return CDbCriteria the created query criteria
     * @throws CException if the condition is not string, array and CDbCriteria
     */
    public CDbCriteria createCriteria(CDbCriteria condition/*=""*/,
            Map params/*=array()*/)
    {
        $criteria=clone $condition;
        return $criteria;
    }
    
    public CDbCriteria createCriteria(Map condition/*=""*/,
            Map params/*=array()*/)
    {
        return new CDbCriteria(condition);
    }
    
    public CDbCriteria createCriteria(String condition/*=""*/,
            Map params/*=array()*/)
    {
        criteria = new CDbCriteria();
        criteria.condition = condition;
        criteria.params = params;
        return criteria;
    }

    /**
     * Creates a query criteria with the specified primary key.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @param mixed $pk primary key value(s). Use array for multiple primary keys. For composite key, each key value must be an array (column name=>column value).
     * @param mixed $condition query condition or criteria.
     * If a string, it is treated as query condition;
     * If an array, it is treated as the initial values for constructing a {@link CDbCriteria};
     * Otherwise, it should be an instance of {@link CDbCriteria}.
     * @param array $params parameters to be bound to an SQL statement.
     * This is only used when the second parameter is a string (query condition).
     * In other cases, please use {@link CDbCriteria::params} to set parameters.
     * @param string $prefix column prefix (ended with dot). If null, it will be the table name
     * @return CDbCriteria the created query criteria
     */
    public CDbCriteria createPkCriteria(Object table, Object pk,
            Object condition/*=""*/, Map<String, Object> params/*=array()*/,
            String prefix/*=null*/)
    {
        table = this.ensureTable(table);
        criteria = this.createCriteria(condition, params);
        if(criteria.alias!="")
            prefix = this._schema.quoteTableName(criteria.alias)+".";
        if(!is_array(pk)) // single key
            pk = array(pk);
        if(is_array($table.primaryKey) && !isset($pk[0]) && $pk!==array()) // single composite key
            $pk=array($pk);
        $condition=this.createInCondition($table,$table.primaryKey,$pk,$prefix);
        if($criteria.condition!="")
            $criteria.condition=$condition." AND (".$criteria.condition.")";
        else
            $criteria.condition=$condition;

        return criteria;
    }

    /**
     * Generates the expression for selecting rows of specified primary key values.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @param array $values list of primary key values to be selected within
     * @param string $prefix column prefix (ended with dot). If null, it will be the table name
     * @return string the expression for selection
     */
    public string createPkCondition(Object table, List<Object> values,
            String prefix/*=null*/)
    {
        this.ensureTable($table);
        return this.createInCondition(table, table.primaryKey, values, prefix);
    }

    /**
     * Creates a query criteria with the specified column values.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @param array $columns column values that should be matched in the query (name=>value)
     * @param mixed $condition query condition or criteria.
     * If a string, it is treated as query condition;
     * If an array, it is treated as the initial values for constructing a {@link CDbCriteria};
     * Otherwise, it should be an instance of {@link CDbCriteria}.
     * @param array $params parameters to be bound to an SQL statement.
     * This is only used when the third parameter is a string (query condition).
     * In other cases, please use {@link CDbCriteria::params} to set parameters.
     * @param string $prefix column prefix (ended with dot). If null, it will be the table name
     * @throws CDbException if specified column is not found in given table
     * @return CDbCriteria the created query criteria
     */
    public CDbCriteria createColumnCriteria(Object table, Map<String, Object> columns,
            Object condition/*=""*/,
            Map<String, Object> params/*=array()*/, String prefix/*=null*/)
    {
        table = this.ensureTable(table);
        criteria = this.createCriteria(condition, params);
        if($criteria.alias!="")
            $prefix=this._schema.quoteTableName($criteria.alias)+".";
        $bindByPosition=isset($criteria.params[0]);
        $conditions=array();
        $values=array();
        $i=0;
        if($prefix == null)
            $prefix = table.rawName+".";
        for($columns as $name, $value)
        {
            if(($column=$table.getColumn($name))!==null)
            {
                if(is_array($value))
                    $conditions[]=this.createInCondition($table,$name,$value,$prefix);
                elseif($value!==null)
                {
                    if($bindByPosition)
                    {
                        $conditions[]=$prefix.$column.rawName."=?";
                        $values[]=$value;
                    }
                    else
                    {
                        $conditions[]=$prefix.$column.rawName."=".self::PARAM_PREFIX.$i;
                        $values[self::PARAM_PREFIX.$i]=$value;
                        $i++;
                    }
                }
                else
                    $conditions[]=$prefix.$column.rawName." IS NULL";
            }
            else
                throw new CDbException(Yii::t("yii","Table "{table}" does not have a column named "{column}".",
                    array("{table}", $table.name,"{column}", $name)));
        }
        $criteria.params=array_merge($values,$criteria.params);
        if(isset($conditions[0]))
        {
            if($criteria.condition!="")
                $criteria.condition=implode(" AND ",$conditions)." AND (".$criteria.condition.")";
            else
                $criteria.condition=implode(" AND ",$conditions);
        }
        return $criteria;
    }

    /**
     * Generates the expression for searching the specified keywords within a list of columns.
     * The search expression is generated using the 'LIKE' SQL syntax.
     * Every word in the keywords must be present and appear in at least one of the columns.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @param array $columns list of column names for potential search condition.
     * @param mixed $keywords search keywords. This can be either a string with space-separated keywords or an array of keywords.
     * @param string $prefix optional column prefix (with dot at the end). If null, the table name will be used as the prefix.
     * @param boolean $caseSensitive whether the search is case-sensitive. Defaults to true.
     * @throws CDbException if specified column is not found in given table
     * @return string SQL search condition matching on a set of columns. An empty string is returned
     * if either the column array or the keywords are empty.
     */
    public String createSearchCondition(Object table, Map<String, Object> columns,
            Object keywords, String prefix, boolean caseSensitive/*=true*/)
    {
        table = this.ensureTable(table);
        if(!is_array($keywords))
            $keywords=preg_split("/\\s+/u", keywords, -1, PREG_SPLIT_NO_EMPTY);
        if(empty($keywords))
            return "";
        if($prefix == null)
            prefix = table.rawName+".";
        conditions = array();
        for ($columns as $name)
        {
            if(($column=$table.getColumn($name))===null)
                throw new CDbException(Yii::t("yii","Table "{table}" does not have a column named "{column}".",
                    array("{table}", $table.name,"{column}", $name)));
            $condition=array();
            foreach($keywords as $keyword)
            {
                $keyword="%".strtr($keyword,array("%", "\%", "_", "\_"))."%";
                if($caseSensitive)
                    $condition[]=$prefix.$column.rawName." LIKE ".this._connection.quoteValue("%".$keyword."%");
                else
                    $condition[]="LOWER(".$prefix.$column.rawName.") LIKE LOWER(".this._connection.quoteValue("%".$keyword."%").")";
            }
            $conditions[]=implode(" AND ",$condition);
        }
        return "("+implode(" OR ",$conditions)+")";
    }

    /**
     * Generates the expression for selecting rows of specified primary key values.
     * @param mixed $table the table schema ({@link CDbTableSchema}) or the table name (string).
     * @param mixed $columnName the column name(s). It can be either a string indicating a single column
     * or an array of column names. If the latter, it stands for a composite key.
     * @param array $values list of key values to be selected within
     * @param string $prefix column prefix (ended with dot). If null, it will be the table name
     * @throws CDbException if specified column is not found in given table
     * @return string the expression for selection
     */
    public String createInCondition(Object table, Object columnName,
            List<Object> values, String prefix/*=null*/)
    {
        if(($n=count($values))<1)
            return "0=1";

        table = this.ensureTable($table);

        if ($prefix==null)
            $prefix=$table.rawName+".";

        $db=this._connection;

        if(is_array($columnName) && count($columnName)==1)
            $columnName=reset($columnName);

        if(is_string($columnName)) // simple key
        {
            if(!isset($table.columns[$columnName]))
                throw new CDbException(Yii.t("yii","Table \"{table}\" does not have a column named \"{column}\".",
                array("{table}", $table.name, "{column}", $columnName)));
            $column=$table.columns[$columnName];

            $values=array_values($values);
            for ($values as &$value)
            {
                $value=$column.typecast($value);
                if(is_string($value))
                    $value=$db.quoteValue($value);
            }
            if($n===1)
                return $prefix.$column.rawName.($values[0]===null?" IS NULL":"=".$values[0]);
            else
                return $prefix.$column.rawName." IN (".implode(", ",$values).")";
        }
        else if(is_array($columnName)) // composite key: $values=array(array('pk1'=>'v1','pk2'=>'v2'),array(...))
        {
            for($columnName as $name)
            {
                if(!isset($table.columns[$name]))
                    throw new CDbException(Yii::t("yii","Table "{table}" does not have a column named "{column}".",
                    array("{table}", $table.name, "{column}", $name)));

                for($i=0;$i<$n;++$i)
                {
                    if(isset($values[$i][$name]))
                    {
                        $value=$table.columns[$name].typecast($values[$i][$name]);
                        if(is_string($value))
                            $values[$i][$name]=$db.quoteValue($value);
                        else
                            $values[$i][$name]=$value;
                    }
                    else
                        throw new CDbException(Yii::t("yii","The value for the column "{column}" is not supplied when querying the table "{table}".",
                            array("{table}", $table.name,"{column}", $name)));
                }
            }
            if(count($values)===1)
            {
                $entries=array();
                foreach($values[0] as $name=>$value)
                    $entries[]=$prefix.$table.columns[$name].rawName.($value===null?" IS NULL":"=".$value);
                return implode(" AND ",$entries);
            }

            return this.createCompositeInCondition($table,$values,$prefix);
        }
        else
            throw new CDbException(Yii::t("yii","Column name must be either a string or an array."));
    }

    /**
     * Generates the expression for selecting rows with specified composite key values.
     * @param CDbTableSchema $table the table schema
     * @param array $values list of primary key values to be selected within
     * @param string $prefix column prefix (ended with dot)
     * @return string the expression for selection
     */
    protected String createCompositeInCondition(CDbTableSchema table, List values,
            String prefix)
    {
        keyNames=array();
        for(array_keys($values[0]) as $name)
            $keyNames[]=$prefix.$table.columns[$name].rawName;
        $vs=array();
        foreach($values as $value)
            $vs[]="(".implode(", ",$value).")";
        return "(".implode(", ",$keyNames).") IN (".implode(", ",$vs).")";
    }

    /**
     * Checks if the parameter is a valid table schema.
     * If it is a string, the corresponding table schema will be retrieved.
     * @param mixed $table table schema ({@link CDbTableSchema}) or table name (string).
     * If this refers to a valid table name, this parameter will be returned with the corresponding table schema.
     * @throws CDbException if the table name is not valid
     */
    protected CDbTableSchema ensureTable(String tableName)
    {
        CDbTableSchema table = this._schema.getTable(tableName, false);
        if(table == null)
            throw new CDbException(Yii.t("yii","Table \"{table}\" does not exist.",
                array("{table}", tableName)));
        return table;
    }
    
    protected CDbTableSchema ensureTable(CDbTableSchema table)
    {
        return table;
    }

    /**
     * Returns default value of the integer/serial primary key. Default value means that the next
     * autoincrement/sequence value would be used.
     * @return string default value of the integer/serial primary key.
     * @since 1.1.14
     */
    protected function getIntegerPrimaryKeyDefaultValue()
    {
        return "NULL";
    }
}

