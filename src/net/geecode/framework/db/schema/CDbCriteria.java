/**
 * CDbCriteria class file.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @link http://www.yiiframework.com/
 * @copyright 2008-2013 Yii Software LLC
 * @license http://www.yiiframework.com/license/
 */
package net.geecode.framework.db.schema;

import static net.geecode.php.base.Global.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.geecode.framework.base.CComponent;


/**
 * CDbCriteria represents a query criteria, such as conditions, ordering by, limit/offset.
 *
 * It can be used in AR query methods such as CActiveRecord::find and CActiveRecord::findAll.
 *
 * $criteria=new CDbCriteria();
 * $criteria->compare('status',Post::STATUS_ACTIVE);
 * $criteria->addInCondition('id',array(1,2,3,4,5,6));
 *
 * $posts = Post::model()->findAll($criteria);
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.db.schema
 * @since 1.0
 */
public class CDbCriteria extends CComponent
{
    public static final String PARAM_PREFIX = ":ycp";
    /**
     * @var integer the global counter for anonymous binding parameters.
     * This counter is used for generating the name for the anonymous parameters.
     */
    public static int paramCount = 0;
    /**
     * @var mixed the columns being selected. This refers to the SELECT clause in an SQL
     * statement. The property can be either a string (column names separated by commas)
     * or an array of column names. Defaults to '*', meaning all columns.
     */
    public String select = "*";
    /**
     * @var boolean whether to select distinct rows of data only. If this is set true,
     * the SELECT clause would be changed to SELECT DISTINCT.
     */
    public boolean distinct = false;
    /**
     * @var string query condition. This refers to the WHERE clause in an SQL statement.
     * For example, <code>age>31 AND team=1</code>.
     */
    public String condition = "";
    /**
     * @var array list of query parameter values indexed by parameter placeholders.
     * For example, <code>array(':name'=>'Dan', ':age'=>31)</code>.
     */
    public Map<String, Object> params = array();
    /**
     * @var integer maximum number of records to be returned. If less than 0, it means no limit.
     */
    public int limit = -1;
    /**
     * @var integer zero-based offset from where the records are to be returned. If less than 0, it means starting from the beginning.
     */
    public int offset = -1;
    /**
     * @var string how to sort the query results. This refers to the ORDER BY clause in an SQL statement.
     */
    public String order = "";
    /**
     * @var string how to group the query results. This refers to the GROUP BY clause in an SQL statement.
     * For example, <code>'projectID, teamID'</code>.
     */
    public String group = "";
    /**
     * @var string how to join with other tables. This refers to the JOIN clause in an SQL statement.
     * For example, <code>'LEFT JOIN users ON users.id=authorID'</code>.
     */
    public String join = "";
    /**
     * @var string the condition to be applied with GROUP-BY clause.
     * For example, <code>'SUM(revenue)<50000'</code>.
     */
    public String having = "";
    
    /**
     * @var mixed the relational query criteria. This is used for fetching related objects in eager loading fashion.
     * This property is effective only when the criteria is passed as a parameter to the following methods of CActiveRecord:
     * <ul>
     * <li>{@link CActiveRecord::find()}</li>
     * <li>{@link CActiveRecord::findAll()}</li>
     * <li>{@link CActiveRecord::findByPk()}</li>
     * <li>{@link CActiveRecord::findAllByPk()}</li>
     * <li>{@link CActiveRecord::findByAttributes()}</li>
     * <li>{@link CActiveRecord::findAllByAttributes()}</li>
     * <li>{@link CActiveRecord::count()}</li>
     * </ul>
     * The property value will be used as the parameter to the {@link CActiveRecord::with()} method
     * to perform the eager loading. Please refer to {@link CActiveRecord::with()} on how to specify this parameter.
     * @since 1.1.0
     */
    public Object with;
    /**
     * @var string the alias name of the table. If not set, it means the alias is 't'.
     */
    public String alias;
    /**
     * @var boolean whether the foreign tables should be joined with the primary table in a single SQL.
     * This property is only used in relational AR queries for HAS_MANY and MANY_MANY relations.
     *
     * When this property is set true, only a single SQL will be executed for a relational AR query,
     * even if the primary table is limited and the relationship between a foreign table and the primary
     * table is many-to-one.
     *
     * When this property is set false, a SQL statement will be executed for each HAS_MANY relation.
     *
     * When this property is not set, if the primary table is limited or paginated,
     * a SQL statement will be executed for each HAS_MANY relation.
     * Otherwise, a single SQL statement will be executed for all.
     *
     * @since 1.1.4
     */
    public boolean together;
    /**
     * @var string the name of the AR attribute whose value should be used as index of the query result array.
     * Defaults to null, meaning the result array will be zero-based integers.
     * @since 1.1.5
     */
    public String index;
    /**
     * @var mixed scopes to apply
     *
     * This property is effective only when passing criteria to
     * the one of the following methods:
     * <ul>
     * <li>{@link CActiveRecord::find()}</li>
     * <li>{@link CActiveRecord::findAll()}</li>
     * <li>{@link CActiveRecord::findByPk()}</li>
     * <li>{@link CActiveRecord::findAllByPk()}</li>
     * <li>{@link CActiveRecord::findByAttributes()}</li>
     * <li>{@link CActiveRecord::findAllByAttributes()}</li>
     * <li>{@link CActiveRecord::count()}</li>
     * </ul>
     *
     * Can be set to one of the following:
     * <ul>
     * <li>One scope: $criteria->scopes='scopeName';</li>
     * <li>Multiple scopes: $criteria->scopes=array('scopeName1','scopeName2');</li>
     * <li>Scope with parameters: $criteria->scopes=array('scopeName'=>array($params));</li>
     * <li>Multiple scopes with parameters: $criteria->scopes=array('scopeName1'=>array($params1),'scopeName2'=>array($params2));</li>
     * <li>Multiple scopes with the same name: array(array('scopeName'=>array($params1)),array('scopeName'=>array($params2)));</li>
     * </ul>
     * @since 1.1.7
     */
    public Object scopes;

    /**
     * Constructor.
     * @param array $data criteria initial property values (indexed by property name)
     */
    public CDbCriteria(Map<String, Object> data/*=array()*/)
    {
        for(Entry<String, Object> ent : data.entrySet()/* as $name=>$value*/)
        {
            String name = ent.getKey();
            Object value = ent.getValue();
            Field f;
            try
            {
                f = this.getClass().getField(name);
                if (null != f)
                    f.set(this, value);
            } catch (NoSuchFieldException | SecurityException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //            this.$name=$value;
            catch (IllegalArgumentException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Remaps criteria parameters on unserialize to prevent name collisions.
     * @since 1.1.9
     */
    public void __wakeup()
    {
        Map<String, Object> map = array();
        params=array();
        for(Entry<String, Object> ent : this.params.entrySet()/* as $name=>$value*/)
        {
            String name = ent.getKey();
            Object value = ent.getValue();
            String newName = null;
            if(strpos(name, PARAM_PREFIX) == 0)
            {
                newName = PARAM_PREFIX + paramCount++;
                map.put(name, newName);
            }
            else
            {
                newName = name;
            }
            params.put(newName, value);
        }
        if (!(map.isEmpty()))
        {
            List<String> sqlContentFieldNames = Arrays.asList(
                "select",
                "condition",
                "order",
                "group",
                "join",
                "having"
            );
            for (String field : sqlContentFieldNames/* as $field*/)
            {
                Field f;
                try
                {
                    f = this.getClass().getField(field);
                    if (null == f)
                        continue;
                    Object fv = f.get(this);
                    if(is_array(fv))
                    {
                        Map mp = (Map) fv;
                        for(Object ent : (mp.entrySet())/* as $k=>$v*/)
                        {
                            
                            Object k = ((Entry)ent).getKey();
                            Object v = ((Entry)ent).getValue();
                            mp.put(k, strtr((String)v, map));
                        }
                    }
                    else
                    {
                        f.set(this, strtr((String)fv, map));
//                    this.$field=strtr(this.$field,$map);
                    }
                } catch (NoSuchFieldException | SecurityException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalArgumentException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        this.params = params;
    }

    /**
     * Appends a condition to the existing {@link condition}.
     * The new condition and the existing condition will be concatenated via the specified operator
     * which defaults to 'AND'.
     * The new condition can also be an array. In this case, all elements in the array
     * will be concatenated together via the operator.
     * This method handles the case when the existing condition is empty.
     * After calling this method, the {@link condition} property will be modified.
     * @param mixed $condition the new condition. It can be either a string or an array of strings.
     * @param string $operator the operator to join different conditions. Defaults to 'AND'.
     * @return CDbCriteria the criteria object itself
     */
    public CDbCriteria addCondition(Object condition, String operator/*="AND"*/)
    {
        if(is_array(condition))
        {
            if(condition == array())
                return this;
            condition="(" + implode(") "+operator+" (",(Object[])condition)+")";
        }
        if(this.condition=="")
            this.condition=(String) condition;
        else
            this.condition="("+this.condition+") "+operator+" ("+condition+")";
        return this;
    }

    /**
     * Appends a search condition to the existing {@link condition}.
     * The search condition and the existing condition will be concatenated via the specified operator
     * which defaults to 'AND'.
     * The search condition is generated using the SQL LIKE operator with the given column name and
     * search keyword.
     * @param string $column the column name (or a valid SQL expression)
     * @param string $keyword the search keyword. This interpretation of the keyword is affected by the next parameter.
     * @param boolean $escape whether the keyword should be escaped if it contains characters % or _.
     * When this parameter is true (default), the special characters % (matches 0 or more characters)
     * and _ (matches a single character) will be escaped, and the keyword will be surrounded with a %
     * character on both ends. When this parameter is false, the keyword will be directly used for
     * matching without any change.
     * @param string $operator the operator used to concatenate the new condition with the existing one.
     * Defaults to 'AND'.
     * @param string $like the LIKE operator. Defaults to 'LIKE'. You may also set this to be 'NOT LIKE'.
     * @return CDbCriteria the criteria object itself
     */
    public CDbCriteria addSearchCondition(String column, String keyword,
            boolean escape/*=true*/, String operator/*="AND"*/, String like/*="LIKE"*/)
    {
        if(keyword=="")
            return this;
        if(escape)
            keyword = "%"
                    + strtr(keyword, array("%", "\\%", "_", "\\_", "\\", "\\\\")) + "%";
        condition = column + " $like " + PARAM_PREFIX + paramCount;
        this.params.put(PARAM_PREFIX + paramCount++, keyword);
        return this.addCondition(condition, operator);
    }

    /**
     * Appends an IN condition to the existing {@link condition}.
     * The IN condition and the existing condition will be concatenated via the specified operator
     * which defaults to 'AND'.
     * The IN condition is generated by using the SQL IN operator which requires the specified
     * column value to be among the given list of values.
     * @param string $column the column name (or a valid SQL expression)
     * @param array $values list of values that the column value should be in
     * @param string $operator the operator used to concatenate the new condition with the existing one.
     * Defaults to 'AND'.
     * @return CDbCriteria the criteria object itself
     */
    public CDbCriteria addInCondition(String column, List<String> values, String operator/*="AND"*/)
    {
        int n = values.size();
        if(n < 1)
            condition = "0=1"; // 0=1 is used because in MSSQL value alone can't be used in WHERE
        else if(n == 1)
        {
            Object value = values.get(0)/*reset(values)*/;
            if (value == null)
                condition = column + " IS NULL";
            else
            {
                condition = column + "=" + PARAM_PREFIX + paramCount;
                this.params.put(PARAM_PREFIX + paramCount++, value);
            }
        }
        else
        {
            List<String> lparams = new ArrayList<String>();
            for (String value : values)
            {
                lparams.add(PARAM_PREFIX + paramCount);
                this.params.put(PARAM_PREFIX + paramCount++, value);
            }
            condition = column + " IN (" + implode(", ", lparams)+")";
        }
        return this.addCondition(condition, operator);
    }

    /**
     * Appends an NOT IN condition to the existing {@link condition}.
     * The NOT IN condition and the existing condition will be concatenated via the specified operator
     * which defaults to 'AND'.
     * The NOT IN condition is generated by using the SQL NOT IN operator which requires the specified
     * column value to be among the given list of values.
     * @param string $column the column name (or a valid SQL expression)
     * @param array $values list of values that the column value should not be in
     * @param string $operator the operator used to concatenate the new condition with the existing one.
     * Defaults to 'AND'.
     * @return CDbCriteria the criteria object itself
     * @since 1.1.1
     */
    public CDbCriteria addNotInCondition(String column, List<String> values,
            String operator/*="AND"*/)
    {
        int n = values.size();
        if(n < 1)
            return this;
        if(n == 1)
        {
            Object value = values.get(0)/*reset(values)*/;
            if(value == null)
                condition= column+" IS NOT NULL";
            else
            {
                condition = column + "!=" + PARAM_PREFIX + paramCount;
                this.params.put(PARAM_PREFIX + paramCount++, value);
            }
        }
        else
        {
            List<String> lparams = new ArrayList<String>();
            for (String value : values/* as $value*/)
            {
                lparams.add(PARAM_PREFIX + paramCount);
                this.params.put(PARAM_PREFIX + paramCount++, value);
            }
            condition = column + " NOT IN (" + implode(", ", lparams) + ")";
        }
        return this.addCondition (condition, operator);
    }

    /**
     * Appends a condition for matching the given list of column values.
     * The generated condition will be concatenated to the existing {@link condition}
     * via the specified operator which defaults to 'AND'.
     * The condition is generated by matching each column and the corresponding value.
     * @param array $columns list of column names and values to be matched (name=>value)
     * @param string $columnOperator the operator to concatenate multiple column matching condition. Defaults to 'AND'.
     * @param string $operator the operator used to concatenate the new condition with the existing one.
     * Defaults to 'AND'.
     * @return CDbCriteria the criteria object itself
     */
    public CDbCriteria addColumnCondition(Map<String, Object> columns,
            String columnOperator/*="AND"*/, String operator/*="AND"*/)
    {
        List<String> lparams = new ArrayList<String>();
        for (Entry<String, Object> et : columns.entrySet()/* as name=>value*/)
        {
            String name = et.getKey();
            Object value = et.getValue();
            if (value == null)
                lparams.add(name+" IS NULL");
            else
            {
                lparams.add(name + "=" + PARAM_PREFIX + paramCount);
                this.params.put(PARAM_PREFIX + paramCount++, value);
            }
        }
        return this.addCondition(implode(" $columnOperator ", params), operator);
    }

    /**
     * Adds a comparison expression to the {@link condition} property.
     *
     * This method is a helper that appends to the {@link condition} property
     * with a new comparison expression. The comparison is done by comparing a column
     * with the given value using some comparison operator.
     *
     * The comparison operator is intelligently determined based on the first few
     * characters in the given value. In particular, it recognizes the following operators
     * if they appear as the leading characters in the given value:
     * <ul>
     * <li><code>&lt;</code>: the column must be less than the given value.</li>
     * <li><code>&gt;</code>: the column must be greater than the given value.</li>
     * <li><code>&lt;=</code>: the column must be less than or equal to the given value.</li>
     * <li><code>&gt;=</code>: the column must be greater than or equal to the given value.</li>
     * <li><code>&lt;&gt;</code>: the column must not be the same as the given value.
     * Note that when $partialMatch is true, this would mean the value must not be a substring
     * of the column.</li>
     * <li><code>=</code>: the column must be equal to the given value.</li>
     * <li>none of the above: the column must be equal to the given value. Note that when $partialMatch
     * is true, this would mean the value must be the same as the given value or be a substring of it.</li>
     * </ul>
     *
     * Note that any surrounding white spaces will be removed from the value before comparison.
     * When the value is empty, no comparison expression will be added to the search condition.
     *
     * @param string $column the name of the column to be searched
     * @param mixed $value the column value to be compared with. If the value is a string, the aforementioned
     * intelligent comparison will be conducted. If the value is an array, the comparison is done
     * by exact match of any of the value in the array. If the string or the array is empty,
     * the existing search condition will not be modified.
     * @param boolean $partialMatch whether the value should consider partial text match (using LIKE and NOT LIKE operators).
     * Defaults to false, meaning exact comparison.
     * @param string $operator the operator used to concatenate the new condition with the existing one.
     * Defaults to "AND'.
     * @param boolean $escape whether the value should be escaped if $partialMatch is true and
     * the value contains characters % or _. When this parameter is true (default),
     * the special characters % (matches 0 or more characters)
     * and _ (matches a single character) will be escaped, and the value will be surrounded with a %
     * character on both ends. When this parameter is false, the value will be directly used for
     * matching without any change.
     * @return CDbCriteria the criteria object itself
     * @since 1.1.1
     */
    public CDbCriteria compare(String column, Object value, boolean partialMatch/*=false*/,
            String operator/*="AND"*/, boolean escape/*=true*/)
    {
        if(is_array(value))
        {
            if(((List)value).isEmpty())
                return this;
            return this.addInCondition(column, (List<String>) value, operator);
        }
        else
            value="$value";

        List<String> matches = new ArrayList<String>();
        String op = null;
        if(preg_match("/^(?:\\s*(<>|<=|>=|<|>|=))?(.*)$/", value, matches))
        {
            value = matches.get(2);
            op = matches.get(1);
        }
        else
            op = "";

        if (value == "")
            return this;

        if (partialMatch)
        {
            if (op == "")
                return this.addSearchCondition (column, (String) value,
                        escape, operator, "LIKE");
            if(op=="<>")
                return this.addSearchCondition (column, (String) value, escape,
                        operator, "NOT LIKE");
        }
        else if (op == "")
            op = "=";

        this.addCondition(column + op + PARAM_PREFIX + paramCount, operator);
        this.params.put(PARAM_PREFIX + paramCount++, value);

        return this;
    }

    /**
     * Adds a between condition to the {@link condition} property.
     *
     * The new between condition and the existing condition will be concatenated via
     * the specified operator which defaults to 'AND'.
     * If one or both values are empty then the condition is not added to the existing condition.
     * This method handles the case when the existing condition is empty.
     * After calling this method, the {@link condition} property will be modified.
     * @param string $column the name of the column to search between.
     * @param string $valueStart the beginning value to start the between search.
     * @param string $valueEnd the ending value to end the between search.
     * @param string $operator the operator used to concatenate the new condition with the existing one.
     * Defaults to 'AND'.
     * @return CDbCriteria the criteria object itself
     * @since 1.1.2
     */
    public CDbCriteria addBetweenCondition(String column, String valueStart, String valueEnd,
            String operator/*="AND"*/)
    {
        if(valueStart == "" || valueEnd == "")
            return this;

        String paramStart = PARAM_PREFIX + paramCount++;
        String paramEnd = PARAM_PREFIX + paramCount++;
        this.params.put(paramStart, valueStart)/*[paramStart]=valueStart*/;
        this.params.put(paramEnd, valueEnd)/*[paramEnd]=valueEnd*/;
        condition="$column BETWEEN $paramStart AND $paramEnd";

        return this.addCondition(condition, operator);
    }

    /**
     * Merges with another criteria.
     * In general, the merging makes the resulting criteria more restrictive.
     * For example, if both criterias have conditions, they will be 'AND' together.
     * Also, the criteria passed as the parameter takes precedence in case
     * two options cannot be merged (e.g. LIMIT, OFFSET).
     * @param mixed $criteria the criteria to be merged with. Either an array or
     *  CDbCriteria.
     * @param string|boolean $operator the operator used to concatenate where and
     *  having conditions. Defaults to 'AND'.
     * For backwards compatibility a boolean value can be passed:
     * - 'false' for 'OR'
     * - 'true' for 'AND'
     */
    public void mergeWith(CDbCriteria criteria, String operator/*="AND"*/)
    {
        if(this.select != criteria.select)
        {
            if(this.select=="*")
                this.select=criteria.select;
            else if(criteria.select!="*")
            {
                String select1 = is_string(this.select) ?
                        preg_split("/\\s*,\\s*/", trim(this.select), -1, 
                                PREG_SPLIT_NO_EMPTY) : 
                                    this.select;
                String select2 = is_string(criteria.select) ?
                        preg_split("/\\s*,\\s*/", trim(criteria.select),
                                -1, PREG_SPLIT_NO_EMPTY) : criteria.select;
                this.select = array_merge(select1, array_diff(select2, select1));
            }
        }

        if (this.condition != criteria.condition)
        {
            if (this.condition == "")
                this.condition = criteria.condition;
            else if (criteria.condition != "")
                this.condition
                        = "({$this->condition}) $operator ({$criteria->condition})";
        }

        if (!this.params.equals(criteria.params))
            this.params = array_merge(this.params, criteria.params);

        if (criteria.limit > 0)
            this.limit = criteria.limit;

        if (criteria.offset >= 0)
            this.offset = criteria.offset;

        if (criteria.alias != null)
            this.alias = criteria.alias;

        if(this.order != criteria.order)
        {
            if (this.order == "")
                this.order = criteria.order;
            else if (criteria.order != "")
                this.order = criteria.order + ", " + this.order;
        }

        if (this.group != criteria.group)
        {
            if (this.group == "")
                this.group = criteria.group;
            else if (criteria.group != "")
                this.group+=", "+criteria.group;
        }

        if (this.join != criteria.join)
        {
            if (this.join == "")
                this.join = criteria.join;
            else if (criteria.join != "")
                this.join += " " + criteria.join;
        }

        if (this.having != criteria.having)
        {
            if (this.having == "")
                this.having=criteria.having;
            else if(criteria.having!="")
                this.having="({$this->having}) $operator ({$criteria->having})";
        }

        if(criteria.distinct/*>0*/)
            this.distinct = criteria.distinct;

        if(criteria.together/*!=null*/)
            this.together = criteria.together;

        if(criteria.index!=null)
            this.index=criteria.index;

        if(null == (this.scopes))
            this.scopes=criteria.scopes;
        else if(null != /*empty*/(criteria.scopes))
        {
            Map<String, Object> scopes1 = (Map<String, Object>)this.scopes;
            Map<String, Object> scopes2=(Map<String, Object>)criteria.scopes;
            for(Entry<String, Object> ent : scopes1.entrySet()/* as $k=>$v*/)
            {
                String k = ent.getKey();
                Object v = ent.getValue();
                /*if(is_integer(k))
                    scopes[]=$v;
                else */if(isset(scopes2, k))
                    scopes[] = array(k, v);
                else
                    scopes[$k]=$v;
            }
            for(Entry<String, Object> ent : scopes2.entrySet()/* as $k=>$v*/)
            {
                /*if(is_integer($k))
                    $scopes[]=$v;
                else*/if(isset($scopes1[$k]))
                    $scopes[]=array($k=>$v);
                else
                    $scopes[$k]=$v;
            }
            $this.scopes=$scopes;
        }

        if(null == (this.with))
            this.with = criteria.with;
        else if(null !=/*empty*/(criteria.with))
        {
            this.with = /*(array)*/this.with;
            for (Entry<String, Object> ent :
                ((Map<String, Object>)criteria.with).entrySet()/* as $k=>$v*/)
            {
                String k = ent.getKey();
                Object v = ent.getValue();
                /*if(is_integer($k))
                    $this.with[]=$v;
                else*/if(isset((Map)this.with, k))
                {
                    Map<String, Object> excludes = array();
                    for(String opt : Arrays.asList("joinType","on")/* as $opt*/)
                    {
                        if(isset((Map)this.with, k) && isset((Map)((Map)this.with).get(k),opt))
                            excludes[$opt]=$this.with[$k][$opt];
                        if(isset($v[$opt]))
                            $excludes[$opt]= ($opt==="on" && isset($excludes[$opt]) && $v[$opt]!==$excludes[$opt]) ?
                                "($excludes[$opt]) AND $v[$opt]" : $v[$opt];
                        unset($this.with[$k][$opt]);
                        unset($v[$opt]);
                    }
                    $this.with[$k]=new self($this.with[$k]);
                    $this.with[$k].mergeWith($v,$operator);
                    $this.with[$k]=$this.with[$k].toArray();
                    if (count($excludes)!==0)
                        $this.with[$k]=CMap::mergeArray($this.with[$k],$excludes);
                }
                else
                    this.with[k]=v;
            }
        }
    }
    public void mergeWith(CDbCriteria criteria)
    {
        mergeWith(criteria, "AND");
    }
    
    public void mergeWith(CDbCriteria criteria, boolean operator/*="AND"*/)
    {
        mergeWith(criteria, operator ? "AND" : "OR");
    }
    
    public void mergeWith(Map criteria, boolean operator/*="AND"*/)
    {
        mergeWith(new CDbCriteria(criteria), operator ? "AND" : "OR");
    }

    /**
     * @return array the array representation of the criteria
     */
    public Map<String, Object> toArray()
    {
        Map<String, Object> result=array();
        for (String name : Arrays.asList("select", "condition", "params", "limit",
                "offset", "order", "group", "join", "having", "distinct", "scopes",
                "with", "alias", "index", "together")/* as $name*/)
        {
            Field f;
            try
            {
                f = this.getClass().getField(name);
                if (null != f)
                {
                    result.put(name, f.get(this));
                }
            } catch (NoSuchFieldException | SecurityException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return result;
    }
}
