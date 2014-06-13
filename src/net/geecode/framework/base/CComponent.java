/**
 * 
 * This file contains the foundation classes for component-based and event-driven programming.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @link http://www.yiiframework.com/
 * @copyright 2008-2013 Yii Software LLC
 * @license http://www.yiiframework.com/license/
 */
package net.geecode.framework.base;

import static net.geecode.php.base.Global.YII_DEBUG;
import static net.geecode.php.base.Global.array;
import static net.geecode.php.base.Global.get_class;
import static net.geecode.php.base.Global.is_array;
import static net.geecode.php.base.Global.is_string;
import static net.geecode.php.base.Global.isset;
import static net.geecode.php.base.Global.method_exists;
import static net.geecode.php.base.Global.strncasecmp;
import static net.geecode.php.base.Global.strtolower;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.geecode.framework.lite.CEvent;
import net.geecode.framework.lite.IBehavior;
import net.geecode.framework.lite.Yii;

/**
 * CComponent is the base class for all components.
 *
 * CComponent implements the protocol of defining, using properties and events.
 *
 * A property is defined by a getter method, and/or a setter method.
 * Properties can be accessed in the way like accessing normal object members.
 * Reading or writing a property will cause the invocation of the corresponding
 * getter or setter method, e.g
 * <pre>
 * $a=$component->text;     // equivalent to $a=$component->getText();
 * $component->text='abc';  // equivalent to $component->setText('abc');
 * </pre>
 * The signatures of getter and setter methods are as follows,
 * <pre>
 * // getter, defines a readable property 'text'
 * public function getText() { ... }
 * // setter, defines a writable property 'text' with $value to be set to the property
 * public function setText($value) { ... }
 * </pre>
 *
 * An event is defined by the presence of a method whose name starts with 'on'.
 * The event name is the method name. When an event is raised, functions
 * (called event handlers) attached to the event will be invoked automatically.
 *
 * An event can be raised by calling {@link raiseEvent} method, upon which
 * the attached event handlers will be invoked automatically in the order they
 * are attached to the event. Event handlers must have the following signature,
 * <pre>
 * function eventHandler($event) { ... }
 * </pre>
 * where $event includes parameters associated with the event.
 *
 * To attach an event handler to an event, see {@link attachEventHandler}.
 * You can also use the following syntax:
 * <pre>
 * $component->onClick=$callback;    // or $component->onClick->add($callback);
 * </pre>
 * where $callback refers to a valid PHP callback. Below we show some callback examples:
 * <pre>
 * 'handleOnClick'                   // handleOnClick() is a global function
 * array($object,'handleOnClick')    // using $object->handleOnClick()
 * array('Page','handleOnClick')     // using Page::handleOnClick()
 * </pre>
 *
 * To raise an event, use {@link raiseEvent}. The on-method defining an event is
 * commonly written like the following:
 * <pre>
 * public function onClick($event)
 * {
 *     $this->raiseEvent('onClick',$event);
 * }
 * </pre>
 * where <code>$event</code> is an instance of {@link CEvent} or its child class.
 * One can then raise the event by calling the on-method instead of {@link raiseEvent} directly.
 *
 * Both property names and event names are case-insensitive.
 *
 * CComponent supports behaviors. A behavior is an
 * instance of {@link IBehavior} which is attached to a component. The methods of
 * the behavior can be invoked as if they belong to the component. Multiple behaviors
 * can be attached to the same component.
 *
 * To attach a behavior to a component, call {@link attachBehavior}; and to detach the behavior
 * from the component, call {@link detachBehavior}.
 *
 * A behavior can be temporarily enabled or disabled by calling {@link enableBehavior}
 * or {@link disableBehavior}, respectively. When disabled, the behavior methods cannot
 * be invoked via the component.
 *
 * Starting from version 1.1.0, a behavior's properties (either its public member variables or
 * its properties defined via getters and/or setters) can be accessed through the component it
 * is attached to.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.base
 * @since 1.0
 * @author David
 *
 */

/**
 */
public class CComponent
{
    private Map<String, List> _e;
    private Map<String, IBehavior> _m;

    /**
     * Returns a property value, an event handler list or a behavior based on its name.
     * Do not call this method. This is a PHP magic method that we override
     * to allow using the following syntax to read a property or obtain event handlers:
     * <pre>
     * $value=$component->propertyName;
     * $handlers=$component->eventName;
     * </pre>
     * @param string $name the property name or event name
     * @return mixed the property value, event handlers attached to the event, or the named behavior
     * @throws CException if the property or event is not defined
     * @see __set
     */
    public Object __get(String name) throws CException
    {
        Method[] ms = this.getClass().getMethods();
        String getter="get"+name;
//        if(method_exists($this,$getter))
//            return $this->$getter();
        for (Method m : ms)
        {
            if (m.getName().equals(getter))
            {
                try
                {
                    return m.invoke(this);
                } catch (IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
        if(strncasecmp(name,"on",2)==0 && method_exists(this, name))
        {
            // duplicating getEventHandlers() here for performance
            name=strtolower(name);
            if(!isset(this._e, name))
                this._e.put(name, new ArrayList());
            return this._e.get(name);
        }
        else if(isset(this._m, name))
            return this._m.get(name);
        else if(is_array(this._m))
        {
//            foreach($this->_m as $object)
//            {
//                if($object->getEnabled() && (property_exists($object,$name) || $object->canGetProperty($name)))
//                    return $object->$name;
//            }
        }
        throw new CException(Yii.t("yii","Property \"{class}.{property}\" is not defined.",
            array("{class}", get_class(this), "{property}", name), null, null));
    }

    /**
     * Sets value of a component property.
     * Do not call this method. This is a PHP magic method that we override
     * to allow using the following syntax to set a property or attach an event handler
     * <pre>
     * $this->propertyName=$value;
     * $this->eventName=$callback;
     * </pre>
     * @param string $name the property name or the event name
     * @param mixed $value the property value or callback
     * @return mixed
     * @throws CException if the property/event is not defined or the property is read only.
     * @see __get
     */
    public void __set(String name, Object value)
    {
        String setter="set" + name;
//        if(method_exists($this,$setter))
//            return $this->$setter($value);
        for (Method m : this.getClass().getDeclaredMethods())
        {
            if (m.getName().equals(setter))
            {
                try
                {
                    m.invoke(this, value);
                } catch (IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
        if(strncasecmp(name,"on",2)==0 && method_exists(this, name))
        {
            // duplicating getEventHandlers() here for performance
            name= name.toLowerCase();
//            if(!isset(this._e, name))
//                this._e.put(name, new CList());
//            return $this->_e[name]->add(value);
        }
        else if(is_array(this._m))
        {
            for(Object obj : this._m.values()/* as $object*/)
            {
//                if(obj.getEnabled() && (property_exists(obj, name) || obj->canSetProperty(name)))
//                    return obj.name=value;
            }
        }
//        if(method_exists(this,"get"+name))
//            throw new CException(Yii::t("yii","Property \"{class}.{property}\" is read only.",
//                array("{class}", get_class(this), "{property}", name)));
//        else
//            throw new CException(Yii::t("yii","Property \"{class}.{property}\" is not defined.",
//                array("{class}", get_class(this), "{property}", name)));
    }

    /**
     * Checks if a property value is null.
     * Do not call this method. This is a PHP magic method that we override
     * to allow using isset() to detect if a component property is set or not.
     * @param string $name the property name or the event name
     * @return boolean
     */
    public boolean __isset(String name)
    {
        String getter="get" + name;
//        if(method_exists(this, getter))
//            return this.getter()!=null;
        for (Method m : this.getClass().getMethods())
        {
            if (m.getName().equals(getter) && m.isAccessible())
            {
                try
                {
                    return m.invoke(this) != null;
                } catch (IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        if(strncasecmp(name,"on",2)==0 && method_exists(this, name))
        {
            name=strtolower(name);
            return isset(this._e, name) && this._e.get(name).size() > 0;
        }
        else if(is_array(this._m))
        {
            if(isset(this._m, name))
                return true;
            for(IBehavior $object : this._m.values())
            {
                if($object.getEnabled())
                {
                    Field f;
                    try
                    {
                        f = $object.getClass().getField(name);
                        if (null != f && f.isAccessible())
                        {
                            return f.get($object) != null;
                        }
                    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    /**
     * Sets a component property to be null.
     * Do not call this method. This is a PHP magic method that we override
     * to allow using unset() to set a component property to be null.
     * @param string $name the property name or the event name
     * @throws CException if the property is read only.
     * @return mixed
     */
    public void __unset(String name) throws CException
    {
        String setter="set"+name;
        if(method_exists(this, setter))
        {
            for (Method m : this.getClass().getDeclaredMethods())
            {
                if (m.getName().equals(setter) && m.isAccessible())
                {
                    try
                    {
                        m.invoke(this, null);
                    } catch (IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return;
                }
            }
        }
        if(strncasecmp(name,"on",2) == 0 && method_exists(this, name))
            this._e.remove(strtolower(name));
        else if(is_array(this._m))
        {
            if(isset(this._m, name))
                this.detachBehavior(name);
            else
            {
                for(IBehavior $object : this._m.values())
                {
                    if($object.getEnabled())
                    {
//                        if(property_exists($object, name))
//                            return $object->$name=null;
//                        else if($object->canSetProperty($name))
//                            return $object->$setter(null);
                        Field f;
                        try
                        {
                            f = $object.getClass().getField(name);
                            if (null != f && f.isAccessible())
                            {
                                f.set($object, null);
                                return;
                            }
                            for (Method m : $object.getClass().getMethods())
                            {
                                if (m.getName().equals(setter) && m.isAccessible())
                                {
                                    m.invoke($object, null);
                                    return;
                                }
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
                        } catch (InvocationTargetException e)
                        {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        
        if(method_exists(this, "get"+name))
            throw new CException(Yii.t("yii","Property \"{class}.{property}\" is read only.",
                array("{class}", get_class(this), "{property}", name), null, null));
    }

    /**
     * Calls the named method which is not a class method.
     * Do not call this method. This is a PHP magic method that we override
     * to implement the behavior feature.
     * @param string $name the method name
     * @param array $parameters method parameters
     * @throws CException if current class and its behaviors do not have a method or closure with the given name
     * @return mixed the method return value
     */
    public Object __call(String name, Object ... parameters) throws CException
    {
        if(this._m != null)
        {
            for(Object object : this._m.values()/* as $object*/)
            {
//                if(((IBehavior)object).getEnabled() && method_exists(object, name))
//                    return call_user_func_array(Arrays.asList(object, name), parameters);
            }
        }
//        if(class_exists("Closure", false) && $this->canGetProperty($name) && $this->$name instanceof Closure)
//            return call_user_func_array($this->$name, $parameters);
        throw new CException(Yii.t("yii","{class} and its behaviors do not have a method or closure named \"{name}\".",
            array("{class}", get_class(this), "{name}", name)));
    }

    /**
     * Returns the named behavior object.
     * The name 'asa' stands for 'as a'.
     * @param string $behavior the behavior name
     * @return IBehavior the behavior object, or null if the behavior does not exist
     */
    public IBehavior asa(String behavior)
    {
        return isset(this._m, behavior) ? this._m.get(behavior) : null;
    }

    /**
     * Attaches a list of behaviors to the component.
     * Each behavior is indexed by its name and should be an instance of
     * {@link IBehavior}, a string specifying the behavior class, or an
     * array of the following structure:
     * <pre>
     * array(
     *     'class'=>'path.to.BehaviorClass',
     *     'property1'=>'value1',
     *     'property2'=>'value2',
     * )
     * </pre>
     * @param array $behaviors list of behaviors to be attached to the component
     */
    public void attachBehaviors(Map<String, IBehavior> behaviors)
    {
        for(Entry<String, IBehavior> ent : behaviors.entrySet()/* as $name=>$behavior*/)
        {
            String name = ent.getKey();
            IBehavior behavior = ent.getValue();
            this.attachBehavior(name, behavior);
        }
    }

    /**
     * Detaches all behaviors from the component.
     */
    public void detachBehaviors()
    {
        if(this._m!=null)
        {
            for(Entry<String, IBehavior> ent : this._m.entrySet()/* as $name=>$behavior*/)
            {
                String name = ent.getKey();
                this.detachBehavior(name);
            }
            this._m=null;
        }
    }

    /**
     * Attaches a behavior to this component.
     * This method will create the behavior object based on the given
     * configuration. After that, the behavior object will be initialized
     * by calling its {@link IBehavior::attach} method.
     * @param string $name the behavior's name. It should uniquely identify this behavior.
     * @param mixed $behavior the behavior configuration. This is passed as the first
     * parameter to {@link YiiBase::createComponent} to create the behavior object.
     * You can also pass an already created behavior instance (the new behavior will replace an already created
     * behavior with the same name, if it exists).
     * @return IBehavior the behavior object
     */
    public IBehavior attachBehavior(String name, IBehavior behavior)
    {
        if(!(behavior instanceof IBehavior))
            behavior = (IBehavior) Yii.createComponent(behavior);
        behavior.setEnabled(true);
        behavior.attach(this);
        return this._m.put(name, behavior);
    }

    /**
     * Detaches a behavior from the component.
     * The behavior's {@link IBehavior::detach} method will be invoked.
     * @param string $name the behavior's name. It uniquely identifies the behavior.
     * @return IBehavior the detached behavior. Null if the behavior does not exist.
     */
    public IBehavior detachBehavior(String name)
    {
        if(isset(this._m, name))
        {
            this._m.get(name).detach(this);
            IBehavior behavior = this._m.get(name);
            this._m.remove(name);
            return behavior;
        }
        return null;
    }

    /**
     * Enables all behaviors attached to this component.
     */
    public void enableBehaviors()
    {
        if(this._m != null)
        {
            for(IBehavior beh : this._m.values()/* as $behavior*/)
                beh.setEnabled(true);
        }
    }

    /**
     * Disables all behaviors attached to this component.
     */
    public void disableBehaviors()
    {
        if(this._m != null)
        {
            for(IBehavior beh : this._m.values()/* as $behavior*/)
                beh.setEnabled(false);
        }
    }

    /**
     * Enables an attached behavior.
     * A behavior is only effective when it is enabled.
     * A behavior is enabled when first attached.
     * @param string $name the behavior's name. It uniquely identifies the behavior.
     */
    public void enableBehavior(String $name)
    {
        if(isset(this._m, $name))
            this._m.get($name).setEnabled(true);
    }

    /**
     * Disables an attached behavior.
     * A behavior is only effective when it is enabled.
     * @param string $name the behavior's name. It uniquely identifies the behavior.
     */
    public void disableBehavior(String $name)
    {
        if(isset(this._m, $name))
            this._m.get($name).setEnabled(false);
    }

    /**
     * Determines whether a property is defined.
     * A property is defined if there is a getter or setter method
     * defined in the class. Note, property names are case-insensitive.
     * @param string $name the property name
     * @return boolean whether the property is defined
     * @see canGetProperty
     * @see canSetProperty
     */
    public boolean hasProperty(String $name)
    {
        return method_exists(this, "get" + $name) || method_exists(this, "set" + $name);
    }

    /**
     * Determines whether a property can be read.
     * A property can be read if the class has a getter method
     * for the property name. Note, property name is case-insensitive.
     * @param string $name the property name
     * @return boolean whether the property can be read
     * @see canSetProperty
     */
    public boolean canGetProperty(String $name)
    {
        return method_exists(this,"get"+$name);
    }

    /**
     * Determines whether a property can be set.
     * A property can be written if the class has a setter method
     * for the property name. Note, property name is case-insensitive.
     * @param string $name the property name
     * @return boolean whether the property can be written
     * @see canGetProperty
     */
    public boolean canSetProperty(String $name)
    {
        return method_exists (this,"set"+$name);
    }

    /**
     * Determines whether an event is defined.
     * An event is defined if the class has a method named like 'onXXX'.
     * Note, event name is case-insensitive.
     * @param string $name the event name
     * @return boolean whether an event is defined
     */
    public boolean hasEvent(String $name)
    {
        return strncasecmp($name,"on",2) < 0 && method_exists(this,$name);
    }

    /**
     * Checks whether the named event has attached handlers.
     * @param string $name the event name
     * @return boolean whether an event has been attached one or several handlers
     */
    public boolean hasEventHandler(String $name)
    {
        $name=strtolower($name);
        return isset(this._e, $name) && this._e.get($name).size()>0;
    }

    /**
     * Returns the list of attached event handlers for an event.
     * @param string $name the event name
     * @return CList list of attached event handlers for the event
     * @throws CException if the event is not defined
     */
    public List getEventHandlers(String $name) throws CException
    {
        if(this.hasEvent($name))
        {
            $name=strtolower($name);
            if(!isset(this._e, $name))
                this._e.put($name, new ArrayList());
            return this._e.get($name);
        }
        else
            throw new CException(Yii.t("yii","Event \"{class}.{event}\" is not defined.",
                array("{class}", get_class(this), "{event}", $name)));
    }

    /**
     * Attaches an event handler to an event.
     *
     * An event handler must be a valid PHP callback, i.e., a string referring to
     * a global function name, or an array containing two elements with
     * the first element being an object and the second element a method name
     * of the object.
     *
     * An event handler must be defined with the following signature,
     * <pre>
     * function handlerName($event) {}
     * </pre>
     * where $event includes parameters associated with the event.
     *
     * This is a convenient method of attaching a handler to an event.
     * It is equivalent to the following code:
     * <pre>
     * $component->getEventHandlers($eventName)->add($eventHandler);
     * </pre>
     *
     * Using {@link getEventHandlers}, one can also specify the excution order
     * of multiple handlers attaching to the same event. For example:
     * <pre>
     * $component->getEventHandlers($eventName)->insertAt(0,$eventHandler);
     * </pre>
     * makes the handler to be invoked first.
     *
     * @param string $name the event name
     * @param callback $handler the event handler
     * @throws CException if the event is not defined
     * @see detachEventHandler
     */
    public void attachEventHandler(String $name, Object $handler) throws CException
    {
        this.getEventHandlers($name).add($handler);
    }

    /**
     * Detaches an existing event handler.
     * This method is the opposite of {@link attachEventHandler}.
     * @param string $name event name
     * @param callback $handler the event handler to be removed
     * @return boolean if the detachment process is successful
     * @throws CException 
     * @see attachEventHandler
     */
    public boolean detachEventHandler(String $name, Object $handler) throws CException
    {
        if(this.hasEventHandler($name))
            return this.getEventHandlers($name).remove($handler);
        else
            return false;
    }

    /**
     * Raises an event.
     * This method represents the happening of an event. It invokes
     * all attached handlers for the event.
     * @param string $name the event name
     * @param CEvent $event the event parameter
     * @throws CException if the event is undefined or an event handler is invalid.
     */
    public void raiseEvent(String $name, CEvent $event)
    {
        $name=strtolower($name);
        if(isset(this._e, $name))
        {
            for(Object $handler : this._e.get($name))
            {
                if(is_string($handler))
                    call_user_func($handler,$event);
                else if(is_callable($handler,true))
                {
                    if(is_array($handler))
                    {
                        // an array: 0 - object, 1 - method name
                        Object $object = ((Object[])$handler)[0];
                        Object $method = ((Object[])$handler)[1];
//                        list($object,$method)=$handler;
                        if(is_string($object))  // static method call
                            call_user_func($handler, $event);
                        else if(method_exists($object, $method))
                            $object.$method($event);
                        else
                            throw new CException(Yii.t("yii","Event \"{class}.{event}\" is attached with an invalid handler \"{handler}\".",
                                array("{class}", get_class(this), "{event}", $name, "{handler}", $handler[1])));
                    }
                    else // PHP 5.3: anonymous function
                        call_user_func($handler,$event);
                }
                else
                    throw new CException(Yii.t("yii","Event \"{class}.{event}\" is attached with an invalid handler \"{handler}\".",
                        array("{class}", get_class(this), "{event}", $name, "{handler}", get_class($handler))));
                // stop further handling if param.handled is set true
                if(($event instanceof CEvent) && $event.handled)
                    return;
            }
        }
        else if(YII_DEBUG && !this.hasEvent($name))
            throw new CException(Yii.t("yii","Event \"{class}.{event}\" is not defined.",
                array("{class}", get_class(this), "{event}", $name)));
    }

    /**
     * Evaluates a PHP expression or callback under the context of this component.
     *
     * Valid PHP callback can be class method name in the form of
     * array(ClassName/Object, MethodName), or anonymous function (only available in PHP 5.3.0 or above).
     *
     * If a PHP callback is used, the corresponding function/method signature should be
     * <pre>
     * function foo($param1, $param2, ..., $component) { ... }
     * </pre>
     * where the array elements in the second parameter to this method will be passed
     * to the callback as $param1, $param2, ...; and the last parameter will be the component itself.
     *
     * If a PHP expression is used, the second parameter will be "extracted" into PHP variables
     * that can be directly accessed in the expression. See {@link http://us.php.net/manual/en/function.extract.php PHP extract}
     * for more details. In the expression, the component object can be accessed using $this.
     *
     * A PHP expression can be any PHP code that has a value. To learn more about what an expression is,
     * please refer to the {@link http://www.php.net/manual/en/language.expressions.php php manual}.
     *
     * @param mixed $_expression_ a PHP expression or PHP callback to be evaluated.
     * @param array $_data_ additional parameters to be passed to the above expression/callback.
     * @return mixed the expression result
     * @since 1.1.0
     */
//    public Object evaluateExpression($_expression_,$_data_=array())
//    {
//        if(is_string($_expression_))
//        {
//            extract($_data_);
//            return eval("return ".$_expression_.";");
//        }
//        else
//        {
//            $_data_[]=$this;
//            return call_user_func_array($_expression_, $_data_);
//        }
//    }
}


/**
 * CEnumerable is the base class for all enumerable types.
 *
 * To define an enumerable type, extend CEnumberable and define string constants.
 * Each constant represents an enumerable value.
 * The constant name must be the same as the constant value.
 * For example,
 * <pre>
 * class TextAlign extends CEnumerable
 * {
 *     const Left='Left';
 *     const Right='Right';
 * }
 * </pre>
 * Then, one can use the enumerable values such as TextAlign::Left and
 * TextAlign::Right.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.base
 * @since 1.0
 */
class CEnumerable
{
}

