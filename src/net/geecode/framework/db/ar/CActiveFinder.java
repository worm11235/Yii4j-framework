/**
 * CActiveFinder class file.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @link http://www.yiiframework.com/
 * @copyright 2008-2013 Yii Software LLC
 * @license http://www.yiiframework.com/license/
 */
package net.geecode.framework.db.ar;

import static net.geecode.php.base.Global.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.geecode.framework.base.CComponent;
import net.geecode.framework.db.CDbException;
import net.geecode.framework.db.schema.CDbCommandBuilder;
import net.geecode.framework.db.schema.CDbCriteria;
import net.geecode.framework.db.schema.CDbSchema;
import net.geecode.framework.db.schema.CDbTableSchema;
import net.geecode.framework.lite.Yii;

/**
 * CActiveFinder implements eager loading and lazy loading of related active records.
 *
 * When used in eager loading, this class provides the same set of find methods as
 * {@link CActiveRecord}.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.db.ar
 * @since 1.0
 */
public class CActiveFinder extends CComponent
{
    /**
     * @var boolean join all tables all at once. Defaults to false.
     * This property is internally used.
     */
    public boolean joinAll=false;
    /**
     * @var boolean whether the base model has limit or offset.
     * This property is internally used.
     */
    public boolean baseLimited=false;

    private int _joinCount=0;
    private CJoinElement _joinTree;
    private CDbCommandBuilder _builder;

    /**
     * Constructor.
     * A join tree is built up based on the declared relationships between active record classes.
     * @param CActiveRecord $model the model that initiates the active finding process
     * @param mixed $with the relation names to be actively looked for
     */
    public CActiveFinder(CActiveRecord model, Object with)
    {
        this._builder = model.getCommandBuilder();
        this._joinTree = new CJoinElement(this, model);
        this.buildJoinTree(this._joinTree, $with);
    }

    /**
     * Do not call this method. This method is used internally to perform the relational query
     * based on the given DB criteria.
     * @param CDbCriteria $criteria the DB criteria
     * @param boolean $all whether to bring back all records
     * @return mixed the query result
     */
    public Collection<Object> query(CDbCriteria criteria, boolean all/*=false*/)
    {
        this.joinAll = criteria.together == true;

        if(criteria.alias!="")
        {
            this._joinTree.tableAlias = criteria.alias;
            this._joinTree.rawTableAlias=this._builder.getSchema().quoteTableName(criteria.alias);
        }

        this._joinTree.find(criteria);
        this._joinTree.afterFind();

        Collection result;
        if(all)
        {
            result = this._joinTree.records.values();
            if (criteria.index!=null)
            {
                String index = criteria.index;
                Map<String, Object> array = array();
                for(Object object : result/* as $object*/)
                    $array[$object.$index]=$object;
                result = array.values();
            }
        }
        else if(!this._joinTree.records.isEmpty())
            result = /*reset*/(this._joinTree.records.values());
        else
            result = null;

        this.destroyJoinTree();
        return result;
    }

    /**
     * This method is internally called.
     * @param string $sql the SQL statement
     * @param array $params parameters to be bound to the SQL statement
     * @return CActiveRecord
     */
    public CActiveRecord findBySql(String sql,
            Map<String, Object> params/*=array()*/)
    {
        Yii.trace(get_class(this._joinTree.model)+".findBySql() eagerly",
                "system.db.ar.CActiveRecord");
        CDbCommand row;
        if((row=this._builder.createSqlCommand(sql, params).queryRow())!=false)
        {
            CActiveRecord baseRecord = this._joinTree.model.populateRecord(row, false);
            this._joinTree.findWithBase(baseRecord);
            this._joinTree.afterFind();
            this.destroyJoinTree();
            return baseRecord;
        }
        else
            this.destroyJoinTree();
    }

    /**
     * This method is internally called.
     * @param string $sql the SQL statement
     * @param array $params parameters to be bound to the SQL statement
     * @return CActiveRecord[]
     */
    public CActiveRecord[] findAllBySql(String sql, Map params/*=array()*/)
    {
        Yii.trace(get_class(this._joinTree.model) + ".findAllBySql() eagerly",
                "system.db.ar.CActiveRecord");
        if((rows=this._builder.createSqlCommand(sql, params).queryAll())
                != array())
        {
            baseRecords = this._joinTree.model.populateRecords(rows, false);
            this._joinTree.findWithBase(baseRecords);
            this._joinTree.afterFind();
            this.destroyJoinTree();
            return baseRecords;
        }
        else
        {
            this.destroyJoinTree();
            return array();
        }
    }

    /**
     * This method is internally called.
     * @param CDbCriteria $criteria the query criteria
     * @return string
     */
    public String count(CDbCriteria criteria)
    {
        Yii.trace(get_class(this._joinTree.model) + ".count() eagerly",
                "system.db.ar.CActiveRecord");
        this.joinAll = criteria.together != true;

        alias = criteria.alias == null ? "t" : criteria.alias;
        this._joinTree.tableAlias = alias;
        this._joinTree.rawTableAlias =
                this._builder.getSchema().quoteTableName(alias);

        n = this._joinTree.count(criteria);
        this.destroyJoinTree();
        return $n;
    }

    /**
     * Finds the related objects for the specified active record.
     * This method is internally invoked by {@link CActiveRecord} to support lazy loading.
     * @param CActiveRecord $baseRecord the base record whose related objects are to be loaded
     */
    public void lazyFind(CActiveRecord baseRecord)
    {
        this._joinTree.lazyFind(baseRecord);
        if(!empty(this._joinTree.children))
        {
            for (this._joinTree.children as child)
                child.afterFind();
        }
        this.destroyJoinTree();
    }

    /**
     * Given active record class name returns new model instance.
     *
     * @param string $className active record class name
     * @return CActiveRecord active record model instance
     *
     * @since 1.1.14
     */
    public CActiveRecord getModel(String className)
    {
        return CActiveRecord.model(className);
    }

    private void destroyJoinTree()
    {
        if(this._joinTree!=null)
            this._joinTree.destroy();
        this._joinTree=null;
    }

    /**
     * Builds up the join tree representing the relationships involved in this query.
     * @param CJoinElement $parent the parent tree node
     * @param mixed $with the names of the related objects relative to the parent tree node
     * @param array $options additional query options to be merged with the relation
     * @return 
     * @throws CDbException if given parent tree node is an instance of {@link CStatElement}
     * or relation is not defined in the given parent's tree node model class
     */
    private CJoinElement buildJoinTree(CJoinElement parent, Object with, Map options/*=null*/)
    {
        if(parent instanceof CStatElement)
            throw new CDbException(Yii.t("yii",
                    "The STAT relation \"{name}\" cannot have child relations.",
                array("{name}", parent.relation.name)));

        if(is_string(with))
        {
            int pos = strrpos(with + "", ".");
            if(pos != -1)
            {
                parent = this.buildJoinTree(parent, (with + "").substring( 0, pos), null);
                with = (with + "").substring(pos+1);
            }

            // named scope
            List<String> scopes;
            pos=strpos(with + "", ":");
            if(pos != -1)
            {
                scopes = Arrays.asList(explode(":", substr(with + "", pos+1)));
                with = substr(with+"", 0, pos);
            }

            if(isset(parent.children, with) && parent.children.get(with).master == null)
                return parent.children.get(with);
            
            CActiveRelation relation = parent.model.getActiveRelation((String) with);
            if(relation == null)
                throw new CDbException(Yii.t("yii","Relation \"{name}\" is not defined in active record class \"{class}\".",
                        array("{class}", get_class(parent.model), "{name}", with)));

//            relation = clone relation;
            CActiveRecord model = this.getModel(relation.className);
            String oldAlias;
            if(relation instanceof CActiveRelation)
            {
                oldAlias = model.getTableAlias(false,false);
                if(isset(options, "alias"))
                    model.setTableAlias(options.get("alias") + "");
                else if(relation.alias == null)
                    model.setTableAlias(relation.name);
                else
                    model.setTableAlias(relation.alias);
            }

            if(!((List)relation.scopes).isEmpty())
                scopes.addAll((List)relation.scopes); // no need for complex merging

            if(options.containsKey("scopes"))
                scopes.addAll((List)options.get("scopes")); // no need for complex merging

            model.resetScope(false);
            CDbCriteria criteria = model.getDbCriteria();
            criteria.scopes = scopes;
            model.beforeFindInternal();
            model.applyScopes(criteria);

            // select has a special meaning in stat relation, so we need to ignore select from scope or model criteria
//            if(relation instanceof CStatRelation)
//                criteria.select = Arrays.asList("*");

            relation.mergeWith(criteria,true);

            // dynamic options
            if(options!=null)
                relation.mergeWith(options);

            if(relation instanceof CActiveRelation)
                model.setTableAlias(oldAlias);

            CJoinElement element;
//            if(relation instanceof CStatRelation)
//                return new CStatElement(this, relation, parent);
//            else
            {
                if(isset(parent.children, with))
                {
                    element = parent.children.get(with);
                    element.relation = relation;
                }
                else
                    element = new CJoinElement(this, relation, parent, ++this._joinCount);
                if(!/*empty*/(relation.through.isEmpty()))
                {
                    CJoinElement slave = this.buildJoinTree(parent, relation.through, array("select", ""));
                    slave.master = element.master;
                    element.slave = slave.slave;
                }
                
                parent.children.put(with + "", element);
                if(!((Map)relation.with).isEmpty())
                    this.buildJoinTree(element, relation.with, null);
                return element;
            }
        }

        // $with is an array, keys are relation name, values are relation spec
        for(Entry<String, Object> e : ((Map<String, Object>)with).entrySet()/* as $key=>$value*/)
        {
            String key = e.getKey();
            Object value = e.getValue();
            if(is_string(value))  // the value is a relation name
                this.buildJoinTree(parent, value, null);
            else if(is_string(key) && is_array(value))
                this.buildJoinTree(parent, key, (Map) value);
        }
    }
}





/**
 * CStatElement represents STAT join element for {@link CActiveFinder}.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.db.ar
 */
class CStatElement extends CJoinElement
{
    /**
     * @var CActiveRelation the relation represented by this tree node
     */
    public CActiveRelation relation;

    private CActiveFinder _finder;
    private CJoinElement _parent;

    /**
     * Constructor.
     * @param CActiveFinder $finder the finder
     * @param CStatRelation $relation the STAT relation
     * @param CJoinElement $parent the join element owning this STAT element
     */
    public CStatElement(CActiveFinder finder, CStatRelation relation,
            CJoinElement parent)
    {
        this._finder = finder;
        this._parent = parent;
        this.relation = relation;
        super.stats.add(this);
    }

    /**
     * Performs the STAT query.
     */
    public void query()
    {
        if(preg_match("/^\\s*(.*?)\\((.*)\\)\\s*$/", this.relation.foreignKey,
                matches))
            this.queryManyMany($matches[1],$matches[2]);
        else
            this.queryOneMany();
    }

    private void queryOneMany()
    {
        $relation=this.relation;
        $model=this._finder.getModel($relation.className);
        $builder=$model.getCommandBuilder();
        $schema=$builder.getSchema();
        $table=$model.getTableSchema();
        $parent=this._parent;
        $pkTable=$parent.model.getTableSchema();

        $fks=preg_split("/\\s*,\\s*/",$relation.foreignKey,-1,PREG_SPLIT_NO_EMPTY);
        if(count($fks)!=count($pkTable.primaryKey))
            throw new CDbException(Yii.t("yii",
                    "The relation \"{relation}\" in active record class \"{class}\""
                            + " is specified with an invalid foreign key. The"
                            + " columns in the key must match the primary keys"
                            + " of the table \"{table}\".",
                        array("{class}", get_class(parent.model),
                                "{relation}", relation.name,
                                "{table}", pkTable.name)));

        // set up mapping between fk and pk columns
        $map=array();  // pk=>fk
        for($fks as $i=>$fk)
        {
            if(!isset($table.columns[$fk]))
                throw new CDbException(Yii.t("yii", "The relation \"{relation}\""
                        + " in active record class \"{class}\" is specified with"
                        + " an invalid foreign key \"{key}\". There is no such"
                        + " column in the table \"{table}\".",
                    array("{class}", get_class($parent.model),
                            "{relation}", $relation.name, "{key}", fk,
                            "{table}", table.name)));

            if(isset($table.foreignKeys[$fk]))
            {
                list($tableName,$pk)=$table.foreignKeys[$fk];
                if($schema.compareTableNames($pkTable.rawName,$tableName))
                    $map[$pk]=$fk;
                else
                    throw new CDbException(Yii.t("yii","The relation \"{relation}\""
                            + " in active record class \"{class}\" is specified"
                            + " with a foreign key \"{key}\" that does not point"
                            + " to the parent table \"{table}\".",
                        array("{class}", get_class($parent.model),
                                "{relation}", relation.name, "{key}", fk,
                                "{table}", pkTable.name)));
            }
            else  // FK constraints undefined
            {
                if(is_array($pkTable.primaryKey)) // composite PK
                    $map[$pkTable.primaryKey[$i]]=$fk;
                else
                    $map[$pkTable.primaryKey]=$fk;
            }
        }

        $records=this._parent.records;

        $join=empty($relation.join)?"" : " ".$relation.join;
        $where=empty($relation.condition) ? " WHERE "
                : " WHERE (" + relation.condition + ") AND ";
        $group=empty($relation.group) ? "" : ", " + relation.group;
        $having=empty($relation.having) ? "" : " HAVING (" + relation.having + ")";
        $order=empty($relation.order) ? "" : " ORDER BY " + relation.order;

        $c=$schema.quoteColumnName("c");
        $s=$schema.quoteColumnName("s");

        $tableAlias=$model.getTableAlias(true);

        // generate and perform query
        if(count($fks)===1)  // single column FK
        {
            $col=$tableAlias.".".$table.columns[$fks[0]].rawName;
            String sql = "SELECT $col AS $c, {$relation.select} AS $s FROM {$table.rawName} "
                    + tableAlias + join + where + "("
                    + builder.createInCondition(table, fks[0], array_keys(records),
                            tableAlias + ".") + ")"
                            + " GROUP BY $col"  + group
                            + having + order;
            $command=$builder.getDbConnection().createCommand($sql);
            if(is_array($relation.params))
                $builder.bindValues($command,$relation.params);
            $stats=array();
            for($command.queryAll() as $row)
                $stats[$row["c"]]=$row["s"];
        }
        else  // composite FK
        {
            $keys=array_keys($records);
            for($keys as &$key)
            {
                $key2=unserialize($key);
                $key=array();
                for($pkTable.primaryKey as $pk)
                    $key[$map[$pk]]=$key2[$pk];
            }
            $cols=array();
            for($pkTable.primaryKey as $n=>$pk)
            {
                $name=$tableAlias.".".$table.columns[$map[$pk]].rawName;
                $cols[$name]=$name." AS ".$schema.quoteColumnName("c".$n);
            }
            $sql="SELECT ".implode(", ",$cols).", {$relation.select} AS $s FROM {$table.rawName} ".$tableAlias.$join
                .$where."(".$builder.createInCondition($table,$fks,$keys,$tableAlias.".").")"
                ." GROUP BY ".implode(", ",array_keys($cols)).$group
                .$having.$order;
            $command=$builder.getDbConnection().createCommand($sql);
            if(is_array($relation.params))
                $builder.bindValues($command,$relation.params);
            $stats=array();
            for($command.queryAll() as $row)
            {
                $key=array();
                for($pkTable.primaryKey as $n=>$pk)
                    $key[$pk]=$row["c".$n];
                $stats[serialize($key)]=$row["s"];
            }
        }

        // populate the results into existing records
        for($records as $pk=>$record)
            $record.addRelatedRecord($relation.name,isset($stats[$pk])?$stats[$pk]:$relation.defaultValue,false);
    }

    /*
     * @param string $joinTableName jointablename
     * @param string $keys keys
     */
    private void queryManyMany(String joinTableName, String keys)
    {
        $relation=this.relation;
        $model=this._finder.getModel($relation.className);
        $table=$model.getTableSchema();
        $builder=$model.getCommandBuilder();
        $schema=$builder.getSchema();
        $pkTable=this._parent.model.getTableSchema();

        $tableAlias=$model.getTableAlias(true);

        if(($joinTable=$builder.getSchema().getTable($joinTableName))==null)
            throw new CDbException(Yii.t("yii", "The relation \"{relation}\" in active record class \"{class}\" is not specified correctly: the join table \"{joinTable}\" given in the foreign key cannot be found in the database.",
                array("{class}", get_class(this._parent.model), "{relation}", $relation.name, "{joinTable}", $joinTableName)));

        $fks=preg_split("/\\s*,\\s*/",$keys,-1,PREG_SPLIT_NO_EMPTY);
        if(count($fks)!=count($table.primaryKey)+count($pkTable.primaryKey))
            throw new CDbException(Yii::t("yii","The relation \"{relation}\" in active record class \"{class}\" is specified with an incomplete foreign key. The foreign key must consist of columns referencing both joining tables.",
                array("{class}", get_class(this._parent.model), "{relation}", $relation.name)));

        $joinCondition=array();
        $map=array();

        $fkDefined=true;
        for($fks as $i=>$fk)
        {
            if(!isset($joinTable.columns[$fk]))
                throw new CDbException(Yii::t("yii","The relation "{relation}" in active record class "{class}" is specified with an invalid foreign key "{key}". There is no such column in the table "{table}".",
                    array("{class}", get_class(this._parent.model), "{relation}", $relation.name, "{key}", $fk, "{table}", $joinTable.name)));

            if(isset($joinTable.foreignKeys[$fk]))
            {
                list($tableName,$pk)=$joinTable.foreignKeys[$fk];
                if(!isset($joinCondition[$pk]) && $schema.compareTableNames($table.rawName,$tableName))
                    $joinCondition[$pk]=$tableAlias.".".$schema.quoteColumnName($pk)."=".$joinTable.rawName.".".$schema.quoteColumnName($fk);
                else if(!isset($map[$pk]) && $schema.compareTableNames($pkTable.rawName,$tableName))
                    $map[$pk]=$fk;
                else
                {
                    $fkDefined=false;
                    break;
                }
            }
            else
            {
                $fkDefined=false;
                break;
            }
        }

        if(!$fkDefined)
        {
            $joinCondition=array();
            $map=array();
            for($fks as $i=>$fk)
            {
                if($i<count($pkTable.primaryKey))
                {
                    $pk=is_array($pkTable.primaryKey) ? $pkTable.primaryKey[$i] : $pkTable.primaryKey;
                    $map[$pk]=$fk;
                }
                else
                {
                    $j=$i-count($pkTable.primaryKey);
                    $pk=is_array($table.primaryKey) ? $table.primaryKey[$j] : $table.primaryKey;
                    $joinCondition[$pk]=$tableAlias.".".$schema.quoteColumnName($pk)."=".$joinTable.rawName.".".$schema.quoteColumnName($fk);
                }
            }
        }

        if($joinCondition===array() || $map===array())
            throw new CDbException(Yii::t("yii","The relation "{relation}" in active record class "{class}" is specified with an incomplete foreign key. The foreign key must consist of columns referencing both joining tables.",
                array("{class}", get_class(this._parent.model), "{relation}", $relation.name)));

        $records=this._parent.records;

        $cols=array();
        for(is_string($pkTable.primaryKey)?array($pkTable.primaryKey):$pkTable.primaryKey as $n=>$pk)
        {
            $name=$joinTable.rawName.".".$schema.quoteColumnName($map[$pk]);
            $cols[$name]=$name." AS ".$schema.quoteColumnName("c".$n);
        }

        $keys=array_keys($records);
        if(is_array($pkTable.primaryKey))
        {
            for($keys as &$key)
            {
                $key2=unserialize($key);
                $key=array();
                for($pkTable.primaryKey as $pk)
                    $key[$map[$pk]]=$key2[$pk];
            }
        }

        $join=empty($relation.join)?"" : " ".$relation.join;
        $where=empty($relation.condition)?"" : " WHERE (".$relation.condition.")";
        $group=empty($relation.group)?"" : ", ".$relation.group;
        $having=empty($relation.having)?"" : " AND (".$relation.having.")";
        $order=empty($relation.order)?"" : " ORDER BY ".$relation.order;

        $sql="SELECT ".this.relation.select." AS ".$schema.quoteColumnName("s").", ".implode(", ",$cols)
            ." FROM ".$table.rawName." ".$tableAlias." INNER JOIN ".$joinTable.rawName
            ." ON (".implode(") AND (",$joinCondition).")".$join
            .$where
            ." GROUP BY ".implode(", ",array_keys($cols)).$group
            ." HAVING (".$builder.createInCondition($joinTable,$map,$keys).")"
            .$having.$order;

        $command=$builder.getDbConnection().createCommand($sql);
        if(is_array($relation.params))
            $builder.bindValues($command,$relation.params);

        $stats=array();
        for($command.queryAll() as $row)
        {
            if(is_array($pkTable.primaryKey))
            {
                $key=array();
                for($pkTable.primaryKey as $n=>$k)
                    $key[$k]=$row["c".$n];
                $stats[serialize($key)]=$row["s"];
            }
            else
                $stats[$row["c0"]]=$row["s"];
        }

        for($records as $pk=>$record)
            $record.addRelatedRecord($relation.name,isset($stats[$pk])?$stats[$pk]:this.relation.defaultValue,false);
    }
}

