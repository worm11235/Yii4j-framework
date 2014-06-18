/**
 * 
 */
package net.geecode.framework.lite;

import static net.geecode.php.base.Global.array;

import java.util.Map;

import net.geecode.framework.base.CComponent;

/**
 * @author worm
 *
 */
abstract class CApplicationComponent extends CComponent implements IApplicationComponent
{
    public Map behaviors=array();
    private boolean _initialized=false;
    public void init()
    {
        this.attachBehaviors(this.behaviors);
        this._initialized=true;
    }
    public boolean getIsInitialized()
    {
        return this._initialized;
    }
}
