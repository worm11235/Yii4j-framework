/**
 * 
 */
package net.geecode.framework.db.ar;

import static net.geecode.php.base.Global.PREG_SPLIT_NO_EMPTY;
import static net.geecode.php.base.Global.array;
import static net.geecode.php.base.Global.preg_match;
import static net.geecode.php.base.Global.preg_split;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.geecode.framework.db.CDbException;
import net.geecode.framework.lite.Yii;

/**
 * CManyManyRelation represents the parameters specifying a MANY_MANY relation.
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.db.ar
 * @since 1.0
 */
public class CManyManyRelation extends CHasManyRelation
{
    /**
     * @var string name of the junction table for the many-to-many relation.
     */
    private String _junctionTableName=null;
    /**
     * @var array list of foreign keys of the junction table for the many-to-many relation.
     */
    private List _junctionForeignKeys=null;

    /**
     * @return string junction table name.
     * @since 1.1.12
     */
    public String getJunctionTableName()
    {
        if (this._junctionTableName == null)
            this.initJunctionData();
        return this._junctionTableName;
    }

    /**
     * @return array list of junction table foreign keys.
     * @since 1.1.12
     */
    public List getJunctionForeignKeys()
    {
        if (this._junctionForeignKeys == null)
            this.initJunctionData();
        return this._junctionForeignKeys;
    }

    /**
     * Initializes values of {@link junctionTableName} and {@link junctionForeignKeys} parsing
     * {@link foreignKey} value.
     * @throws CDbException if {@link foreignKey} has been specified in wrong format.
     */
    private void initJunctionData()
    {
        List<String> matches = new ArrayList<String>();
        if(!preg_match("/^\\s*(.*?)\\((.*)\\)\\s*$/", this.foreignKey, matches))
            throw new CDbException(Yii.t("yii",
                    "The relation \"{relation}\" in active record class \"{class}\" is specified"
                    + " with an invalid foreign key. The format of the foreign key must be "
                    + "\"joinTable(fk1,fk2,...)\".",
                    array("{class}", this.className, "{relation}", this.name)));
        this._junctionTableName = matches.get(1);
        this._junctionForeignKeys = Arrays.asList(preg_split("/\\s*,\\s*/", matches.get(2), -1, PREG_SPLIT_NO_EMPTY));
    }
}
