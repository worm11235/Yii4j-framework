/**
 * 
 */
package net.geecode.framework.db.ar;

import static net.geecode.php.base.Global.array;
import static net.geecode.php.base.Global.implode;
import static net.geecode.php.base.Global.is_array;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.geecode.framework.db.CDbCommand;
import net.geecode.framework.db.schema.CDbCommandBuilder;
import net.geecode.framework.db.schema.CDbCriteria;


/**
 * CJoinQuery represents a JOIN SQL statement.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.db.ar
 * @since 1.0
 */
public class CJoinQuery
{
    /**
     * @var array list of column selections
     */
    public List<String> selects = new ArrayList<String>();
    /**
     * @var boolean whether to select distinct result set
     */
    public boolean distinct=false;
    /**
     * @var array list of join statement
     */
    public List<Object> joins = new ArrayList<Object>();
    /**
     * @var array list of WHERE clauses
     */
    public List<String> conditions = new ArrayList<String>();
    /**
     * @var array list of ORDER BY clauses
     */
    public List<Object> orders = new ArrayList<Object>();
    /**
     * @var array list of GROUP BY clauses
     */
    public List<Object> groups = new ArrayList<Object>();
    /**
     * @var array list of HAVING clauses
     */
    public List<Object> havings = new ArrayList<Object>();
    /**
     * @var integer row limit
     */
    public int limit=-1;
    /**
     * @var integer row offset
     */
    public int offset=-1;
    /**
     * @var array list of query parameters
     */
    public Map params=array();
    /**
     * @var array list of join element IDs (id=>true)
     */
    public Map<String, Boolean> elements = new HashMap<String, Boolean>();

    /**
     * Constructor.
     * @param CJoinElement $joinElement The root join tree.
     * @param CDbCriteria $criteria the query criteria
     */
    public CJoinQuery (CJoinElement joinElement, CDbCriteria criteria/*=null*/)
    {
        if (criteria != null)
        {
            this.selects.add(joinElement.getColumnSelect(criteria.select));
            this.joins.add(joinElement.getTableNameWithAlias());
            this.joins.add(criteria.join);
            this.conditions.add(criteria.condition);
            this.orders.add(criteria.order);
            this.groups.add(criteria.group);
            this.havings.add(criteria.having);
            this.limit = criteria.limit;
            this.offset = criteria.offset;
            this.params = criteria.params;
            if(!this.distinct && criteria.distinct)
                this.distinct=true;
        }
        else
        {
            this.selects.add(joinElement.getPrimaryKeySelect());
            this.joins.add(joinElement.getTableNameWithAlias());
            this.conditions.add(joinElement.getPrimaryKeyRange());
        }
        this.elements.put(joinElement.id + "",true);
    }

    /**
     * Joins with another join element
     * @param CJoinElement $element the element to be joined
     */
    public void join(CJoinElement element)
    {
        if(element.slave!=null)
            this.join(element.slave);
        if(!empty(element.relation.select))
            this.selects.add(element.getColumnSelect(element.relation.select));
        this.conditions.add(element.relation.condition);
        this.orders.add(element.relation.order);
        this.joins.add(element.getJoinCondition());
        this.joins.add(element.relation.join);
        this.groups.add(element.relation.group);
        this.havings.add(element.relation.having);

        if(is_array(element.relation.params))
        {
            if(is_array(this.params))
                this.params.putAll(element.relation.params);
            else
                this.params.putAll(element.relation.params);
        }
        this.elements.put(element.id + "", true);
    }

    /**
     * Creates the SQL statement.
     * @param CDbCommandBuilder $builder the command builder
     * @return CDbCommand DB command instance representing the SQL statement
     */
    public CDbCommand createCommand(CDbCommandBuilder builder)
    {
        String sql = (this.distinct ? "SELECT DISTINCT "
                : "SELECT ") + implode(", ", this.selects);
        sql += " FROM " + implode(" ",this.joins);

        List<Object> conditions = new ArrayList<Object>();
        for (Object condition : this.conditions/* as $condition*/)
            if (condition.equals(""))
                conditions.add(condition);
        if (!conditions.isEmpty())
            sql += " WHERE (" + implode(") AND (", conditions) + ")";

        List<Object> groups = new ArrayList<Object>();
        for(Object group : this.groups/* as $group*/)
            if(!group.equals(""))
                groups.add(group);
        if(!groups.isEmpty())
            sql += " GROUP BY " + implode(", ", groups);

        List<Object> havings = new ArrayList<Object>();
        for(Object having : this.havings)
            if(!having.equals(""))
                havings.add(having);
        if(!havings.isEmpty())
            sql += " HAVING (" + implode(") AND (", havings) + ")";

        List<Object> orders = new ArrayList<Object>();
        for(Object order : this.orders)
            if(!order.equals(""))
                orders.add(order);
        if(!orders.isEmpty())
            sql += " ORDER BY " + implode(", ", orders);

        sql = builder.applyLimit(sql, this.limit, this.offset);
        CDbCommand command = builder.getDbConnection().createCommand(sql);
        builder.bindValues(command, this.params);
        return command;
    }
}
