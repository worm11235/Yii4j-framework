/**
 * 
 */
package net.geecode.framework.db.ar;

import static net.geecode.php.base.Global.array;
import static net.geecode.php.base.Global.explode;
import static net.geecode.php.base.Global.get_class;
import static net.geecode.php.base.Global.implode;
import static net.geecode.php.base.Global.is_array;
import static net.geecode.php.base.Global.is_string;
import static net.geecode.php.base.Global.isset;
import static net.geecode.php.base.Global.strncasecmp;
import static net.geecode.php.base.Global.strrpos;
import static net.geecode.php.base.Global.trim;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.geecode.framework.db.CDbException;
import net.geecode.framework.db.schema.CDbColumnSchema;
import net.geecode.framework.db.schema.CDbCommandBuilder;
import net.geecode.framework.db.schema.CDbCriteria;
import net.geecode.framework.db.schema.CDbSchema;
import net.geecode.framework.db.schema.CDbTableSchema;
import net.geecode.framework.lite.Yii;



/**
 * CJoinElement represents a tree node in the join tree created by {@link CActiveFinder}.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.db.ar
 * @since 1.0
 */
public class CJoinElement
{
    /**
     * @var integer the unique ID of this tree node
     */
    public int id;
    /**
     * @var CActiveRelation the relation represented by this tree node
     */
    public CActiveRelation relation;
    /**
     * @var CActiveRelation the master relation
     */
    public CActiveRelation master;
    /**
     * @var CActiveRelation the slave relation
     */
    public CActiveRelation slave;
    /**
     * @var CActiveRecord the model associated with this tree node
     */
    public CActiveRecord model;
    /**
     * @var array list of active records found by the queries. They are indexed by primary key values.
     */
    public Map records=array();
    /**
     * @var array list of child join elements
     */
    public Map<String, CJoinElement> children = new HashMap<String, CJoinElement>();
    /**
     * @var array list of stat elements
     */
    public List<CStatElement> stats = new ArrayList<CStatElement>();
    /**
     * @var string table alias for this join element
     */
    public String tableAlias;
    /**
     * @var string the quoted table alias for this element
     */
    public String rawTableAlias;

    private CActiveFinder _finder;
    private CDbCommandBuilder _builder;
    private CJoinElement _parent;
    private Object _pkAlias;                  // string or name=>alias
    private Map<String, Object> _columnAliases = array();    // name=>alias
    private boolean _joined=false;
    private CDbTableSchema _table;
    private Map _related=array();          // PK, relation name, related PK => true

    /**
     * Constructor.
     * @param CActiveFinder $finder the finder
     * @param mixed $relation the relation (if the third parameter is not null)
     * or the model (if the third parameter is null) associated with this tree node.
     * @param CJoinElement $parent the parent tree node
     * @param integer $id the ID of this tree node that is unique among all the tree nodes
     */
    public CJoinElement(CActiveFinder finder, Object relation,
            CJoinElement parent/*=null*/, int id/*=0*/)
    {
        this._finder = finder;
        this.id = id;
        if(parent!=null)
        {
            this.relation = (CActiveRelation) relation;
            this._parent = parent;
            this.model = this._finder.getModel(relation.getClass().getName());
            this._builder = this.model.getCommandBuilder();
            this.tableAlias = ((CActiveRelation)relation).alias == null
                    ? ((CActiveRelation)relation).name : ((CActiveRelation)relation).alias;
            this.rawTableAlias = this._builder.getSchema().quoteTableName(this.tableAlias);
            this._table = this.model.getTableSchema();
        }
        else  // root element, the first parameter is the model.
        {
            this.model = (CActiveRecord) relation;
            this._builder = ((CActiveRecord)relation).getCommandBuilder();
            this._table = ((CActiveRecord)relation).getTableSchema();
            this.tableAlias = this.model.getTableAlias(false, true);
            this.rawTableAlias = this._builder.getSchema().quoteTableName(this.tableAlias);
        }

        // set up column aliases, such as t1_c2
        CDbTableSchema table = this._table;
        String prefix;
        if(this.model.getDbConnection().getDriverName().equals("oci"))  // Issue 482
            prefix = "T" + id + "_C";
        else
            prefix = "t" + id + "_c";
        for(Entry<String, CDbColumnSchema> e : table.getColumnNames().entrySet()/* as $key=>$name*/)
        {
            String key = e.getKey();
            String name = e.getValue().name;
            String alias = prefix + key;
            this._columnAliases.put(name, alias);
            if (table.primaryKey.equals(name))
                this._pkAlias = alias;
            else if(is_array(table.primaryKey) && /*in_array*/(/*name, */table.primaryKey.contains(name)))
                ((Map)this._pkAlias).put(name, alias);
        }
    }

    /**
     * Removes references to child elements and finder to avoid circular references.
     * This is internally used.
     */
    public void destroy()
    {
        if(!(this.children.isEmpty()))
        {
            for(CJoinElement child : this.children.values()/* as $child*/)
                child.destroy();
        }
//        unset(this._finder, this._parent, this.model,
//                this.relation, this.master, this.slave,
//                this.records, this.children, this.stats);
        this._finder = null;
        this._parent = null;
        this.model = null;
        this.relation = null;
        this.master = null;
        this.slave = null;
        this.records = null;
        this.children = null;
        this.stats = null;
    }

    /**
     * Performs the recursive finding with the criteria.
     * @param CDbCriteria $criteria the query criteria
     */
    public void find(CDbCriteria criteria/*=null*/)
    {
        CJoinQuery query;
        if(this._parent == null) // root element
        {
            query = new CJoinQuery(this, criteria);
            this._finder.baseLimited = (criteria.offset >= 0 || criteria.limit >= 0);
            this.buildQuery(query);
            this._finder.baseLimited=false;
            this.runQuery(query);
        }
        else if(!this._joined && !(this._parent.records.isEmpty())) // not joined before
        {
            query = new CJoinQuery(this._parent, null);
            this._joined=true;
            query.join(this);
            this.buildQuery(query);
            this._parent.runQuery(query);
        }

        for(CJoinElement child : this.children.values()/* as $child*/) // find recursively
            child.find(null);

        for(CStatElement stat : this.stats/* as $stat*/)
            stat.query();
    }

    /**
     * Performs lazy find with the specified base record.
     * @param CActiveRecord $baseRecord the active record whose related object is to be fetched.
     */
    public void lazyFind(CActiveRecord baseRecord)
    {
//        if(is_string(this._table.primaryKey))
//        {
//            Field f = baseRecord.getClass().getField(this._table.primaryKey)
//            this.records[$baseRecord.{this._table.primaryKey}]=$baseRecord;
//        }
//        else
        {
            Map<String, Object> pk = array();
            for(String name : this._table.primaryKey/* as $name*/)
            {
                Field f = baseRecord.getClass().getField(name);
                
                pk.put(name, f.get(baseRecord));
            }
            this.records.put(serialize(pk), baseRecord);
        }

        for(CStatElement stat : this.stats/* as $stat*/)
            stat.query();

        if(null == this.children)
            return;

        Map<String, Object> params = array();
        for(CJoinElement child : this.children.values()/* as $child*/) {
            if(is_array(child.relation.params))
                params.putAll(child.relation.params);

            CJoinQuery query = new CJoinQuery(child);
            query.selects=Arrays.asList(child.getColumnSelect(child.relation.select));
            query.conditions=Arrays.asList(
                child.relation.condition,
                child.relation.on
            );
            query.groups.add(child.relation.group);
            query.joins.add(child.relation.join);
            query.havings.add(child.relation.having);
            query.orders.add(child.relation.order);
            query.params = params;
            query.elements.put(child.id + "", true);
            if(child.relation instanceof CHasManyRelation)
            {
                query.limit = child.relation.limit;
                query.offset = child.relation.offset;
            }
    
            child.applyLazyCondition(query, baseRecord);
    
            this._joined=true;
            child._joined=true;
    
            this._finder.baseLimited=false;
            child.buildQuery(query);
            child.runQuery(query);
            for(CJoinElement c : child.children.values()/* as $c*/)
                c.find(null);
    
            if((child.records.isEmpty()))
                return;
            if(child.relation instanceof CHasOneRelation || child.relation instanceof CBelongsToRelation)
                baseRecord.addRelatedRecord(child.relation.name, /*reset*/(child.records),false);
            else // has_many and many_many
            {
                for(Object record : child.records.values())
                {
                    if(child.relation.index!=null)
                        index = record.{child.relation.index};
                        else
                            index=true;
                    baseRecord.addRelatedRecord(child.relation.name, record, index);
                }
            }
        }
    }

    /**
     * Apply Lazy Condition
     * @param CJoinQuery $query represents a JOIN SQL statements
     * @param CActiveRecord $record the active record whose related object is to be fetched.
     * @throws CDbException if relation in active record class is not specified correctly
     */
    private void applyLazyCondition(CJoinQuery query, CActiveRecord record)
    {
        CDbSchema schema = this._builder.getSchema();
        CJoinElement parent = this._parent;
        if(this.relation instanceof CManyManyRelation)
        {
            joinTableName=this.relation.getJunctionTableName();
            if(($joinTable=$schema.getTable($joinTableName))==null)
                throw new CDbException(Yii.t("yii","The relation \"{relation}\" in active record class \"{class}\" is not specified correctly: the join table \"{joinTable}\" given in the foreign key cannot be found in the database.",
                    array("{class}", get_class(parent.model), "{relation}", this.relation.name, "{joinTable}", joinTableName)));
            $fks=this.relation.getJunctionForeignKeys();

            joinAlias=$schema.quoteTableName(this.relation.name+"_"+this.tableAlias);
            parentCondition=array();
            childCondition=array();
            count=0;
            params=array();

            fkDefined=true;
            for($fks as $i=>$fk)
            {
                if(isset($joinTable.foreignKeys[$fk]))  // FK defined
                {
                    list($tableName,$pk)=$joinTable.foreignKeys[$fk];
                    if(!isset($parentCondition[$pk]) && $schema.compareTableNames($parent._table.rawName,$tableName))
                    {
                        $parentCondition[$pk]=$joinAlias.".".$schema.quoteColumnName($fk)."=:ypl".$count;
                        $params[":ypl".$count]=$record.$pk;
                        $count++;
                    }
                    else if(!isset($childCondition[$pk]) && $schema.compareTableNames(this._table.rawName,$tableName))
                        $childCondition[$pk]=this.getColumnPrefix().$schema.quoteColumnName($pk)."=".$joinAlias.".".$schema.quoteColumnName($fk);
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
                $parentCondition=array();
                $childCondition=array();
                $count=0;
                $params=array();
                for($fks as $i=>$fk)
                {
                    if($i<count($parent._table.primaryKey))
                    {
                        $pk=is_array($parent._table.primaryKey) ? $parent._table.primaryKey[$i] : $parent._table.primaryKey;
                        $parentCondition[$pk]=$joinAlias.".".$schema.quoteColumnName($fk)."=:ypl".$count;
                        $params[":ypl".$count]=$record.$pk;
                        $count++;
                    }
                    else
                    {
                        $j=$i-count($parent._table.primaryKey);
                        $pk=is_array(this._table.primaryKey) ? this._table.primaryKey[$j] : this._table.primaryKey;
                        $childCondition[$pk]=this.getColumnPrefix().$schema.quoteColumnName($pk)."=".$joinAlias.".".$schema.quoteColumnName($fk);
                    }
                }
            }

            if($parentCondition!=array() && $childCondition!=array())
            {
                $join="INNER JOIN ".$joinTable.rawName." ".$joinAlias." ON ";
                $join.="(".implode(") AND (",$parentCondition).") AND (".implode(") AND (",$childCondition).")";
                if(!empty(this.relation.on))
                    $join.=" AND (".this.relation.on.")";
                $query.joins[]=$join;
                for($params as $name=>$value)
                    $query.params[$name]=$value;
            }
            else
                throw new CDbException(Yii::t("yii","The relation "{relation}" in active record class "{class}" is specified with an incomplete foreign key. The foreign key must consist of columns referencing both joining tables.",
                    array("{class}", get_class($parent.model), "{relation}", this.relation.name)));
        }
        else
        {
            $element=this;
            while(true)
            {
                $condition=$element.relation.condition;
                if(!empty($condition))
                    $query.conditions[]=$condition;
                $query.params=array_merge($query.params,$element.relation.params);
                if($element.slave!=null)
                {
                    $query.joins[]=$element.slave.joinOneMany($element.slave,$element.relation.foreignKey,$element,$parent);
                    $element=$element.slave;
                }
                else
                    break;
            }
            fks = is_array(element.relation.foreignKey)
                    ? element.relation.foreignKey
                            : preg_split("/\\s*,\\s*/",
                                    element.relation.foreignKey, -1,
                                    PREG_SPLIT_NO_EMPTY);
            $prefix=$element.getColumnPrefix();
            $params=array();
            for($fks as $i=>$fk)
            {
                if(!is_int($i))
                {
                    $pk=$fk;
                    $fk=$i;
                }

                if($element.relation instanceof CBelongsToRelation)
                {
                    if(is_int($i))
                    {
                        if(isset($parent._table.foreignKeys[$fk]))  // FK defined
                            $pk=$parent._table.foreignKeys[$fk][1];
                        else if(is_array($element._table.primaryKey)) // composite PK
                            $pk=$element._table.primaryKey[$i];
                        else
                            $pk=$element._table.primaryKey;
                    }
                    $params[$pk]=$record.$fk;
                }
                else
                {
                    if(is_int($i))
                    {
                        if(isset($element._table.foreignKeys[$fk]))  // FK defined
                            $pk=$element._table.foreignKeys[$fk][1];
                        else if(is_array($parent._table.primaryKey)) // composite PK
                            $pk=$parent._table.primaryKey[$i];
                        else
                            $pk=$parent._table.primaryKey;
                    }
                    $params[$fk]=$record.$pk;
                }
            }
            $count=0;
            for($params as $name=>$value)
            {
                $query.conditions[] = prefix+schema.quoteColumnName($name)
                        + "=:ypl" + count;
                $query.params[":ypl".$count]=$value;
                $count++;
            }
        }
    }

    /**
     * Performs the eager loading with the base records ready.
     * @param mixed $baseRecords the available base record(s).
     */
    public void findWithBase(Object baseRecords)
    {
        if(!is_array($baseRecords))
            $baseRecords=array($baseRecords);
        if(is_string(this._table.primaryKey))
        {
            for($baseRecords as $baseRecord)
                this.records[$baseRecord.{this._table.primaryKey}]=$baseRecord;
        }
        else
        {
            for($baseRecords as $baseRecord)
            {
                $pk=array();
                for(this._table.primaryKey as $name)
                    $pk[$name]=$baseRecord.$name;
                this.records[serialize($pk)]=$baseRecord;
            }
        }

        $query=new CJoinQuery(this);
        this.buildQuery($query);
        if(count($query.joins)>1)
            this.runQuery($query);
        for(this.children as $child)
            $child.find();

        for(this.stats as $stat)
            $stat.query();
    }

    /**
     * Count the number of primary records returned by the join statement.
     * @param CDbCriteria $criteria the query criteria
     * @return string number of primary records. Note: type is string to keep max. precision.
     */
    public String count(CDbCriteria criteria/*=null*/)
    {
        $query=new CJoinQuery(this,$criteria);
        // ensure only one big join statement is used
        this._finder.baseLimited=false;
        this._finder.joinAll=true;
        this.buildQuery($query);

        $query.limit=$query.offset=-1;

        if(!empty($criteria.group) || !empty($criteria.having))
        {
            $query.orders = array();
            $command=$query.createCommand(this._builder);
            $sql=$command.getText();
            $sql="SELECT COUNT(*) FROM ({$sql}) sq";
            $command.setText($sql);
            $command.params=$query.params;
            return $command.queryScalar();
        }
        else
        {
            $select=is_array($criteria.select) ? implode(",",$criteria.select) : $criteria.select;
            if($select!="*" && !strncasecmp($select,"count",5))
                $query.selects=array($select);
            else if(is_string(this._table.primaryKey))
            {
                $prefix=this.getColumnPrefix();
                $schema=this._builder.getSchema();
                $column=$prefix.$schema.quoteColumnName(this._table.primaryKey);
                $query.selects=array("COUNT(DISTINCT $column)");
            }
            else
                $query.selects=array("COUNT(*)");

            $query.orders=$query.groups=$query.havings=array();
            $command=$query.createCommand(this._builder);
            return $command.queryScalar();
        }
    }

    /**
     * Calls {@link CActiveRecord::afterFind} of all the records.
     */
    public void afterFind()
    {
        for(this.records as $record)
            $record.afterFindInternal();
        for(this.children as $child)
            $child.afterFind();

        this.children = null;
    }

    /**
     * Builds the join query with all descendant HAS_ONE and BELONGS_TO nodes.
     * @param CJoinQuery $query the query being built up
     */
    public void buildQuery(CJoinQuery query)
    {
        for(this.children as $child)
        {
            if($child.master!=null)
                $child._joined=true;
            else if($child.relation instanceof CHasOneRelation || $child.relation instanceof CBelongsToRelation
                || this._finder.joinAll || $child.relation.together || (!this._finder.baseLimited && $child.relation.together===null))
            {
                $child._joined=true;
                $query.join($child);
                $child.buildQuery($query);
            }
        }
    }

    /**
     * Executes the join query and populates the query results.
     * @param CJoinQuery $query the query to be executed.
     */
    public void runQuery(CJoinQuery query)
    {
        $command=$query.createCommand(this._builder);
        for($command.queryAll() as $row)
            this.populateRecord($query,$row);
    }

    /**
     * Populates the active records with the query data.
     * @param CJoinQuery $query the query executed
     * @param array $row a row of data
     * @return CActiveRecord the populated record
     */
    private CActiveRecord populateRecord(CJoinQuery query, Map row)
    {
        // determine the primary key value
        if(is_string(this._pkAlias))  // single key
        {
            if(isset($row[this._pkAlias]))
                $pk=$row[this._pkAlias];
            else    // no matching related objects
                return null;
        }
        else // is_array, composite key
        {
            $pk=array();
            for(this._pkAlias as $name=>$alias)
            {
                if(isset($row[$alias]))
                    $pk[$name]=$row[$alias];
                else    // no matching related objects
                    return null;
            }
            $pk=serialize($pk);
        }

        // retrieve or populate the record according to the primary key value
        if(isset(this.records[$pk]))
            $record=this.records[$pk];
        else
        {
            $attributes=array();
            $aliases=array_flip(this._columnAliases);
            for($row as $alias=>$value)
            {
                if(isset($aliases[$alias]))
                    $attributes[$aliases[$alias]]=$value;
            }
            $record=this.model.populateRecord($attributes,false);
            for(this.children as $child)
            {
                if(!empty($child.relation.select))
                    $record.addRelatedRecord($child.relation.name,null,$child.relation instanceof CHasManyRelation);
            }
            this.records[$pk]=$record;
        }

        // populate child records recursively
        for(this.children as $child)
        {
            if(!isset($query.elements[$child.id]) || empty($child.relation.select))
                continue;
            $childRecord=$child.populateRecord($query,$row);
            if($child.relation instanceof CHasOneRelation || $child.relation instanceof CBelongsToRelation)
                $record.addRelatedRecord($child.relation.name,$childRecord,false);
            else // has_many and many_many
            {
                // need to double check to avoid adding duplicated related objects
                if($childRecord instanceof CActiveRecord)
                    $fpk=serialize($childRecord.getPrimaryKey());
                else
                    $fpk=0;
                if(!isset(this._related[$pk][$child.relation.name][$fpk]))
                {
                    if($childRecord instanceof CActiveRecord && $child.relation.index!=null)
                        $index=$childRecord.{$child.relation.index};
                    else
                        $index=true;
                    $record.addRelatedRecord($child.relation.name,$childRecord,$index);
                    this._related[$pk][$child.relation.name][$fpk]=true;
                }
            }
        }

        return $record;
    }

    /**
     * @return string the table name and the table alias (if any). This can be used directly in SQL query without escaping.
     */
    public String getTableNameWithAlias()
    {
        if(this.tableAlias != null)
            return this._table.rawName + " " + this.rawTableAlias;
        else
            return this._table.rawName;
    }

    /**
     * Generates the list of columns to be selected.
     * Columns will be properly aliased and primary keys will be added to selection if they are not specified.
     * @param mixed $select columns to be selected. Defaults to '*', indicating all columns.
     * @throws CDbException if active record class is trying to select an invalid column
     * @return string the column selection
     */
    public String getColumnSelect(Object select/*='*'*/)
    {
        schema = this._builder.getSchema();
        prefix = this.getColumnPrefix();
        columns = array();
        if (select == "*")
        {
            for (this._table.getColumnNames() as name)
                columns[] = prefix.$schema.quoteColumnName($name)+" AS "
                        +$schema.quoteColumnName(this._columnAliases[$name]);
        }
        else
        {
            if(is_string($select))
                $select=explode(",",$select);
            $selected=array();
            for($select as $name)
            {
                $name=trim($name);
                $matches=array();
                if(($pos=strrpos($name,"."))!=false)
                    $key=substr($name,$pos+1);
                else
                    $key=$name;
                $key=trim($key,"\'"`");

                if($key==="*")
                {
                    for(this._table.columns as $name=>$column)
                    {
                        $alias=this._columnAliases[$name];
                        if(!isset($selected[$alias]))
                        {
                            $columns[]=$prefix.$column.rawName." AS ".$schema.quoteColumnName($alias);
                            $selected[$alias]=1;
                        }
                    }
                    continue;
                }

                if(isset(this._columnAliases[$key]))  // simple column names
                {
                    $columns[]=$prefix.$schema.quoteColumnName($key)." AS ".$schema.quoteColumnName(this._columnAliases[$key]);
                    $selected[this._columnAliases[$key]]=1;
                }
                else if(preg_match("/^(.*?)\\s+AS\\s+(\\w+)$/im",$name,$matches)) // if the column is already aliased
                {
                    $alias=$matches[2];
                    if(!isset(this._columnAliases[$alias]) || this._columnAliases[$alias]!=$alias)
                    {
                        this._columnAliases[$alias]=$alias;
                        $columns[]=$name;
                        $selected[$alias]=1;
                    }
                }
                else
                    throw new CDbException(Yii::t("yii","Active record "{class}" is trying to select an invalid column "{column}". Note, the column must exist in the table or be an expression with alias.",
                        array("{class}", get_class(this.model), "{column}", $name)));
            }
            // add primary key selection if they are not selected
            if(is_string(this._pkAlias) && !isset($selected[this._pkAlias]))
                $columns[]=$prefix.$schema.quoteColumnName(this._table.primaryKey)." AS ".$schema.quoteColumnName(this._pkAlias);
            else if(is_array(this._pkAlias))
            {
                for(this._table.primaryKey as $name)
                    if(!isset($selected[$name]))
                        $columns[]=$prefix.$schema.quoteColumnName($name)." AS ".$schema.quoteColumnName(this._pkAlias[$name]);
            }
        }

        return implode(", ",$columns);
    }

    /**
     * @return string the primary key selection
     */
    public String getPrimaryKeySelect()
    {
        $schema=this._builder.getSchema();
        $prefix=this.getColumnPrefix();
        $columns=array();
        if(is_string(this._pkAlias))
            $columns[]=$prefix.$schema.quoteColumnName(this._table.primaryKey)." AS ".$schema.quoteColumnName(this._pkAlias);
        else if(is_array(this._pkAlias))
        {
            for(this._pkAlias as $name=>$alias)
                $columns[]=$prefix.$schema.quoteColumnName($name)." AS ".$schema.quoteColumnName($alias);
        }
        return implode(", ",$columns);
    }

    /**
     * @return string the condition that specifies only the rows with the selected primary key values.
     */
    public String getPrimaryKeyRange()
    {
        if(empty(this.records))
            return "";
        $values=array_keys(this.records);
        if(is_array(this._table.primaryKey))
        {
            for($values as &$value)
                $value=unserialize($value);
        }
        return this._builder.createInCondition(this._table,this._table.primaryKey,$values,this.getColumnPrefix());
    }

    /**
     * @return string the column prefix for column reference disambiguation
     */
    public String getColumnPrefix()
    {
        if(this.tableAlias!=null)
            return this.rawTableAlias.".";
        else
            return this._table.rawName.".";
    }

    /**
     * @throws CDbException if relation in active record class is not specified correctly
     * @return string the join statement (this node joins with its parent)
     */
    public String getJoinCondition()
    {
        $parent=this._parent;
        if(this.relation instanceof CManyManyRelation)
        {
            $schema=this._builder.getSchema();
            $joinTableName=this.relation.getJunctionTableName();
            if(($joinTable=$schema.getTable($joinTableName))===null)
                throw new CDbException(Yii::t("yii","The relation "{relation}" in active record class "{class}" is not specified correctly: the join table "{joinTable}" given in the foreign key cannot be found in the database.",
                    array("{class}", get_class($parent.model), "{relation}", this.relation.name, "{joinTable}", $joinTableName)));
            $fks=this.relation.getJunctionForeignKeys();

            return this.joinManyMany($joinTable,$fks,$parent);
        }
        else
        {
            $fks=is_array(this.relation.foreignKey)
                    ? this.relation.foreignKey
                            : preg_split("/\\s*,\\s*/", this.relation.foreignKey,
                                    -1, PREG_SPLIT_NO_EMPTY);
            if(this.slave!=null)
            {
                if(this.relation instanceof CBelongsToRelation)
                {
                    $fks=array_flip($fks);
                    $pke=this.slave;
                    $fke=this;
                }
                else
                {
                    $pke=this;
                    $fke=this.slave;
                }
            }
            else if(this.relation instanceof CBelongsToRelation)
            {
                $pke=this;
                $fke=$parent;
            }
            else
            {
                $pke=$parent;
                $fke=this;
            }
            return this.joinOneMany($fke,$fks,$pke,$parent);
        }
    }

    /**
     * Generates the join statement for one-many relationship.
     * This works for HAS_ONE, HAS_MANY and BELONGS_TO.
     * @param CJoinElement $fke the join element containing foreign keys
     * @param array $fks the foreign keys
     * @param CJoinElement $pke the join element contains primary keys
     * @param CJoinElement $parent the parent join element
     * @return string the join statement
     * @throws CDbException if a foreign key is invalid
     */
    private String joinOneMany(CJoinElement fke, Map<String, Object> fks,
            CJoinElement pke, CJoinElement parent)
    {
        schema = this._builder.getSchema();
        joins=array();
        if(is_string(fks))
            fks = preg_split("/\\s*,\\s*/", fks, -1, PREG_SPLIT_NO_EMPTY);
        for (fks as $i=>$fk)
        {
            if(!is_int($i))
            {
                $pk=$fk;
                $fk=$i;
            }

            if(!isset($fke._table.columns[$fk]))
                throw new CDbException(Yii.t("yii",
                        "The relation \"{relation}\" in active record class"
                                + " \"{class}\" is specified with an invalid"
                                + " foreign key \"{key}\". There is no such "
                                + "column in the table \"{table}\".",
                    array("{class}", get_class(parent.model),
                            "{relation}", this.relation.name, "{key}", fk,
                            "{table}", fke._table.name)));

            if(is_int($i))
            {
                if(isset($fke._table.foreignKeys[$fk])
                        && $schema.compareTableNames($pke._table.rawName,
                                $fke._table.foreignKeys[$fk][0]))
                    $pk=$fke._table.foreignKeys[$fk][1];
                else // FK constraints undefined
                {
                    if(is_array($pke._table.primaryKey)) // composite PK
                        $pk=$pke._table.primaryKey[$i];
                    else
                        $pk=$pke._table.primaryKey;
                }
            }

            $joins[]=$fke.getColumnPrefix() + $schema.quoteColumnName($fk)
                    + "=" + $pke.getColumnPrefix().$schema.quoteColumnName($pk);
        }
        if(!empty(this.relation.on))
            $joins[]=this.relation.on;
        return this.relation.joinType + " " + this.getTableNameWithAlias()
                + " ON (" + implode(") AND (",$joins) + ")";
    }

    /**
     * Generates the join statement for many-many relationship.
     * @param CDbTableSchema $joinTable the join table
     * @param array $fks the foreign keys
     * @param CJoinElement $parent the parent join element
     * @return string the join statement
     * @throws CDbException if a foreign key is invalid
     */
    private String joinManyMany(CDbTableSchema joinTable, Map fks,
            CJoinElement parent)
    {
        CDbSchema schema=this._builder.getSchema();
        joinAlias = schema.quoteTableName(this.relation.name+"_"+this.tableAlias);
        parentCondition=array();
        childCondition=array();

        fkDefined=true;
        for($fks as $i=>$fk)
        {
            if(!isset($joinTable.columns[$fk]))
                throw new CDbException(Yii.t("yii","The relation \"{relation}\""
                        + " in active record class \"{class}\" is specified"
                        + " with an invalid foreign key \"{key}\". There is no"
                        + " such column in the table \"{table}\".",
                    array("{class}", get_class($parent.model),
                            "{relation}", this.relation.name,
                            "{key}", fk, "{table}", joinTable.name)));

            if(isset($joinTable.foreignKeys[$fk]))
            {
                list($tableName,$pk)=$joinTable.foreignKeys[$fk];
                if(!isset(parentCondition[$pk])
                        && $schema.compareTableNames($parent._table.rawName,
                                tableName))
                    $parentCondition[$pk] = $parent.getColumnPrefix()
                            + schema.quoteColumnName($pk)
                            + "=" + joinAlias + "." + schema.quoteColumnName($fk);
                else if(!isset($childCondition[$pk])
                        && $schema.compareTableNames(this._table.rawName,$tableName))
                    $childCondition[$pk] = this.getColumnPrefix()
                            + schema.quoteColumnName($pk) + "=" + joinAlias
                            + "." + schema.quoteColumnName($fk);
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
            $parentCondition=array();
            $childCondition=array();
            for($fks as $i=>$fk)
            {
                if($i<count($parent._table.primaryKey))
                {
                    $pk=is_array($parent._table.primaryKey)
                            ? $parent._table.primaryKey[$i]
                                    : $parent._table.primaryKey;
                    $parentCondition[$pk] = parent.getColumnPrefix()
                            + schema.quoteColumnName($pk) + "=" + joinAlias
                            + "." + schema.quoteColumnName($fk);
                }
                else
                {
                    $j=$i-count($parent._table.primaryKey);
                    $pk=is_array(this._table.primaryKey)
                            ? this._table.primaryKey[$j]
                                    : this._table.primaryKey;
                    $childCondition[$pk] = this.getColumnPrefix()
                            + schema.quoteColumnName($pk) + "="
                            + joinAlias + "." + schema.quoteColumnName($fk);
                }
            }
        }

        if (parentCondition != array() && childCondition != array())
        {
            join = this.relation.joinType + " " + joinTable.rawName + " "
                    + joinAlias;
            join += " ON (" + implode(") AND (",$parentCondition) + ")";
            $join.=" ".this.relation.joinType." ".this.getTableNameWithAlias();
            $join.=" ON (".implode(") AND (",$childCondition).")";
            if(!empty(this.relation.on))
                $join.=" AND (".this.relation.on.")";
            return $join;
        }
        else
            throw new CDbException(Yii.t("yii","The relation \"{relation}\" in "
                    + "active record class \"{class}\" is specified with an"
                    + " incomplete foreign key. The foreign key must consist"
                    + " of columns referencing both joining tables.",
                array("{class}", get_class($parent.model),
                        "{relation}", this.relation.name)));
    }
}
