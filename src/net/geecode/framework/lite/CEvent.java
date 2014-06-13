/**
 * 
 */
package net.geecode.framework.lite;

import net.geecode.framework.base.CComponent;

/**
 * CEvent is the base class for all event classes.
 *
 * It encapsulates the parameters associated with an event.
 * The {@link sender} property describes who raises the event.
 * And the {@link handled} property indicates if the event is handled.
 * If an event handler sets {@link handled} to true, those handlers
 * that are not invoked yet will not be invoked anymore.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.base
 * @since 1.0
 * @author David
 *
 */
public class CEvent extends CComponent
{
    /**
     * @var object the sender of this event
     */
    public Object sender;
    /**
     * @var boolean whether the event is handled. Defaults to false.
     * When a handler sets this true, the rest of the uninvoked event handlers will not be invoked anymore.
     */
    public boolean handled=false;
    /**
     * @var mixed additional event parameters.
     * @since 1.1.7
     */
    public Object params;

    /**
     * Constructor.
     * @param mixed $sender sender of the event
     * @param mixed $params additional parameters for the event
     */
    public CEvent(Object $sender/*=null*/, Object $params/*=null*/)
    {
        this.sender=$sender;
        this.params=$params;
    }

    public CEvent(Object $sender/*=null*/)
    {
        this.sender = $sender;
    }
}
