/**
 * 
 */
package net.geecode.framework.lite;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.geecode.framework.base.CComponent;
import net.geecode.framework.base.CException;
import static net.geecode.php.base.Global.*;

/**
 * @author David
 *
 */
public abstract class CModule extends CComponent
{
    public Map preload=array();
    public Map behaviors=array();
    private String _id;
    private CModule _parentModule;
    private String _basePath;
    private String _modulePath;
    private Map<String, Object> _params;
    private Map<String, CModule> _modules = new HashMap<String, CModule>();
    private Map<String, Object> _moduleConfig = new HashMap<String, Object>();
    private Map<String, IApplicationComponent> _components =new HashMap<String, IApplicationComponent>();
    private Map<String, Map<String, Object>> _componentConfig=new HashMap<String, Map<String, Object>>();
    
    public CModule(String $id, CModule $parent, Map $config/*=null*/) throws CException
    {
        this._id=$id;
        this._parentModule=$parent;
        // set basePath at early as possible to avoid trouble
//        if(is_string($config))
//            $config=require($config);
        if(isset($config, "basePath"))
        {
            this.setBasePath((String) $config.get("basePath"));
            $config.remove("basePath");
        }
        Yii.setPathOfAlias($id + "",this.getBasePath());
        this.preinit();
        this.configure($config);
        this.attachBehaviors(this.behaviors);
        this.preloadComponents();
        this.init();
    }
    public Object __get(String $name) throws CException
    {
        try
        {
            if(this.hasComponent($name))
                return this.getComponent($name, true);
        } catch (NumberFormatException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return super.__get($name);
    }
    public boolean __isset(String $name)
    {
        try
        {
            if(this.hasComponent($name))
                return this.getComponent($name, true)!=null;
        } catch (NumberFormatException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return super.__isset($name);
    }
    public String getId()
    {
        return this._id;
    }
    public void setId(String $id)
    {
        this._id=$id;
    }
    public String getBasePath()
    {
        if(this._basePath==null)
        {
            Class $class= this.getClass();
            this._basePath=dirname($class.getFileName());
        }
        return this._basePath;
    }
    public void setBasePath(String $path) throws CException
    {
        if((this._basePath=realpath($path))==null || !is_dir(this._basePath))
            throw new CException(Yii.t("yii","Base path \"{path}\" is not a valid directory.",
                array("{path}",$path), null, null));
    }
    public Map<String, Object> getParams()
    {
        if(this._params!=null)
            return this._params;
        else
        {
            this._params = new HashMap<String, Object>();
//            this._params.caseSensitive=true;
            return this._params;
        }
    }
    public void setParams(Map<String, Object> $value)
    {
        this.getParams().putAll($value);
    }
    public String getModulePath()
    {
        if(this._modulePath!=null)
            return this._modulePath;
        else
            return this._modulePath=this.getBasePath()+DIRECTORY_SEPARATOR+"modules";
    }
    public void setModulePath(String $value) throws CException
    {
        if((this._modulePath=realpath($value))==null || !is_dir(this._modulePath))
            throw new CException(Yii.t("yii","The module path \"{path}\" is not a valid directory.",
                array("{path}", $value), null, null));
    }
//    public void setImport($aliases)
//    {
//        foreach($aliases as $alias)
//            Yii.import($alias);
//    }
    public void setAliases(Map<String, String> $mappings)
    {
        for(Entry<String, String> ent: $mappings.entrySet()/* as $name=>$alias*/)
        {
            String $name = ent.getKey();
            String $alias = ent.getValue();
            String $path=Yii.getPathOfAlias( $alias);
            if(($path)!=null)
                Yii.setPathOfAlias($name, $path);
            else
                Yii.setPathOfAlias($name, $alias);
        }
    }
    public CModule getParentModule()
    {
        return this._parentModule;
    }
    public CModule getModule(String $id)
    {
        if(isset(this._modules, $id)/* || array_key_exists($id,this._modules)*/)
            return this._modules.get($id);
        else if(isset(this._moduleConfig, $id))
        {
            Map<String, Object> $config = (Map<String, Object>) this._moduleConfig.get($id);
            if(!isset($config, "enabled") || null != $config.get("enabled"))
            {
                Object $class = $config.get("class");
                $config.remove("class");
                $config.remove("enabled");
                CModule $module;
                if(this==Yii.app())
                    $module=(CModule) Yii.createComponent($class,$id,null,$config);
                else
                    $module=(CModule) Yii.createComponent($class,this.getId()+"/"+$id,this,$config);
                this._modules.put($id, $module);
                return $module;
            }
        }
        return null;
    }
    
    public boolean hasModule(int $id)
    {
        return isset(this._moduleConfig, $id) || isset(this._modules, $id);
    }
    public Map getModules()
    {
        return this._moduleConfig;
    }
    public void setModules(Map<String, Object> $modules)
    {
        for(Entry<String, Object> ent : $modules.entrySet()/* as $id=>$module*/)
        {
            String $id = ent.getKey();
            Object module = ent.getValue();
            Map<String, Object> $module = null;
            if((module instanceof Number))
            {
                $id=module + "";
                $module=array();
            }
            else
            {
                $module = (Map<String, Object>) module;
            }
            if(!isset((Map)$module, "class"))
            {
                Yii.setPathOfAlias($id,this.getModulePath()+DIRECTORY_SEPARATOR+$id);
                ($module).put("class", $id+"."+ucfirst($id)+"Module");
            }
            if(isset(this._moduleConfig, $id))
            {
                Map<String, Object> map = new HashMap<String, Object>();
                map.putAll($module);
                map.putAll((Map<? extends String, ? extends Object>) this._moduleConfig.get($id));
                this._moduleConfig.put($id, map);
            }
            else
                this._moduleConfig.put((String) $id, $module);
        }
    }
    public boolean hasComponent(String $id)
    {
        return isset(this._components, $id) || isset(this._componentConfig, $id);
    }
    public Object getComponent(String $id, boolean $createIfNull/*=true*/)
    {
        if(isset(this._components, $id))
            return this._components.get($id);
        else if(isset(this._componentConfig, $id) && $createIfNull)
        {
            Object $config = this._componentConfig.get($id);
            if(!isset((Map)$config, "enabled") || null != ((Map)$config).get("enabled"))
            {
                ((Map)$config).remove("enabled");
                IApplicationComponent $component = Yii.createComponent($config);
                $component.init();
                this._components.put($id, $component);
                return $component;
            }
        }
        
        return null;
    }
    public Object getComponent(String $id)
    {
        return getComponent($id, true);
    }
    public void setComponent(String $id, IApplicationComponent $component, boolean $merge/*=true*/)
    {
        if($component==null)
        {
            this._components.remove($id);
            return;
        }
        
        if($component instanceof IApplicationComponent)
        {
            this._components.put($id, $component);
            if(!((IApplicationComponent)$component).getIsInitialized())
                ((IApplicationComponent)$component).init();
            return;
        }
    }
    public void setComponent(String $id, Map<String, Object> $component, boolean $merge/*=true*/)
    {
        if($component==null)
        {
            this._components.remove($id);
            return;
        }
        else if(isset(this._components, $id))
        {
            if(isset((Map)$component, "class")
                    && get_class(this._components.get($id))!=$component.get("class"))
            {
                this._components.remove($id);
                this._componentConfig.put($id, $component); //we should ignore merge here
                return;
            }
            for(Entry<String, Object> ent : $component.entrySet()/* as $key=>$value*/)
            {
                String $key = ent.getKey();
                Object $value = ent.getValue();
                if($key!="class")
                {
                    Field f;
                    try
                    {
                        f = this._components.get($id).getClass().getField($key);
                        if (null != f && f.isAccessible())
                        {
                            f.set(this._components.get($id), $value);
                        }
                    } catch (NoSuchFieldException | SecurityException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // this._components[$id].$key=$value;
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
        }
        else if(isset(this._componentConfig.get($id), "class") && isset($component, "class")
            && (this._componentConfig.get($id)).get("class")!=($component).get("class"))
        {
            this._componentConfig.put($id, $component); //we should ignore merge here
            return;
        }
        if(isset(this._componentConfig, $id) && $merge)
        {
            this._componentConfig.get($id).putAll($component);
        }
        else
            this._componentConfig.put($id, $component);
    }
    
    public Map getComponents(boolean $loadedOnly/*=true*/)
    {
        if($loadedOnly)
            return this._components;
        else
        {
            Map ret = new HashMap();
            ret.putAll(this._components);
            ret.putAll(this._componentConfig);
            return ret;
        }
    }
    
    public void setComponents(Map<String, IApplicationComponent> $components, boolean $merge/*=true*/)
    {
        for(Entry<String, IApplicationComponent> ent : $components.entrySet()/* as $id=>$component*/)
        {
            String $id = ent.getKey();
            IApplicationComponent $component = ent.getValue();
            this.setComponent($id,$component,$merge);
        }
    }
    
    public void setComponents(Map<String, IApplicationComponent> $components)
    {
        setComponents($components, true);
    }
    
    public void configure(Map<String, Object>$config)
    {
        for (Entry<String, Object> ent : $config.entrySet())
        {
            String $key = ent.getKey();
            Object $value = ent.getValue();
            Field f;
            try
            {
                f = this.getClass().getDeclaredField($key);
                if (null != f)
                {
                    f.set(this, $value);
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
    protected void preloadComponents()
    {
        for(Object $id : this.preload.values()/* as $id*/)
            this.getComponent((String)$id, true);
    }
    
    protected void preinit()
    {
    }
    
    protected void init()
    {
    }
}
