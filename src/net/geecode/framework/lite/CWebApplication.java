/**
 * 
 */
package net.geecode.framework.lite;

import static net.geecode.php.base.Global.*;

import java.util.Map;

import net.geecode.framework.base.CApplication;
import net.geecode.framework.base.CException;

/**
 * @author worm
 *
 */
class CWebApplication extends CApplication
{
    public String defaultController="site";
    public String layout="main";
    public Map controllerMap=array();
    public Map catchAllRequest;
    public Object controllerNamespace;
    private String _controllerPath;
    private String _viewPath;
    private String _systemViewPath;
    private String _layoutPath;
    private String _controller;
    private String _theme;
    
    public void processRequest()
    {
        String route = null;
        if(is_array(this.catchAllRequest) && isset(this.catchAllRequest, 0))
        {
            route = (String) this.catchAllRequest.get(0);
            for (array_splice(this.catchAllRequest,1) as $name=>$value)
                $_GET[$name]=$value;
        }
        else
            route = this.getUrlManager().parseUrl(this.getRequest());
        this.runController(route);
    }
    
    protected void registerCoreComponents()
    {
        super.registerCoreComponents();
        Map<String, Object> components = array(
            "session", array(
                "class", "CHttpSession"
            ),
            "assetManager", array(
                "class", "CAssetManager"
            ),
            "user", array(
                "class", "CWebUser"
            ),
            "themeManager", array(
                "class", "CThemeManager"
            ),
            "authManager", array(
                "class", "CPhpAuthManager"
            ),
            "clientScript", array(
                "class", "CClientScript"
            ),
            "widgetFactory", array(
                "class", "CWidgetFactory"
            )
        );
        this.setComponents(components);
    }
    public Object getAuthManager()
    {
        return this.getComponent("authManager");
    }
    public Object getAssetManager()
    {
        return this.getComponent("assetManager");
    }
    public Object getSession()
    {
        return this.getComponent("session");
    }
    public Object getUser()
    {
        return this.getComponent("user");
    }
    public Object getViewRenderer()
    {
        return this.getComponent("viewRenderer");
    }
    public Object getClientScript()
    {
        return this.getComponent("clientScript");
    }
    public Object getWidgetFactory()
    {
        return this.getComponent("widgetFactory");
    }
    public Object getThemeManager()
    {
        return this.getComponent("themeManager");
    }
    public String getTheme()
    {
        if(is_string(this._theme))
            this._theme=this.getThemeManager().getTheme(this._theme);
        return this._theme;
    }
    public void setTheme(String value)
    {
        this._theme = value;
    }
    public void runController(String route)
    {
        Object ca = this.createController(route, null);
        if(ca != null)
        {
            list(controller, actionID) = ca;
            oldController=this._controller;
            this._controller=$controller;
            controller.init();
            controller.run(actionID);
            this._controller=$oldController;
        }
        else
            throw new CHttpException(404,Yii.t("yii", "Unable to resolve the request \"{route}\".",
                array("{route}", route==""?this.defaultController:route)));
    }
    public Object createController(String route, CWebApplication owner/*=null*/)
    {
        if(owner == null)
            owner = this;
        if((route=trim(route,"/"))=="")
            route = owner.defaultController;
        caseSensitive = this.getUrlManager().caseSensitive;
        route += "/";
        int pos=strpos($route,"/");
        while(pos != -1)
        {
            String id = route.substring(0, pos);
            if(!preg_match("/^\\w+$/", id))
                return null;
            if(!caseSensitive)
                id = strtolower(id);
            route = route.substring(pos+1);
            if(!isset($basePath))  // first segment
            {
                if(isset(owner.controllerMap[$id]))
                {
                    return array(
                        Yii.createComponent(owner.controllerMap.get(id), id, owner==this?null:owner),
                        this.parseActionParams(route)
                    );
                }
                if(($module=$owner.getModule($id))!==null)
                    return this.createController($route,$module);
                $basePath=$owner.getControllerPath();
                $controllerID="";
            }
            else
                $controllerID.="/";
            $className=ucfirst($id)."Controller";
            $classFile=$basePath.DIRECTORY_SEPARATOR.$className.".php";
            if($owner.controllerNamespace!==null)
                $className=$owner.controllerNamespace."\\".$className;
            if(is_file($classFile))
            {
                if(!class_exists($className,false))
                    require($classFile);
                if(class_exists($className,false) && is_subclass_of($className,"CController"))
                {
                    $id[0]=strtolower($id[0]);
                    return array(
                        new $className($controllerID.$id,$owner===$this?null:$owner),
                        this.parseActionParams($route),
                    );
                }
                return null;
            }
            $controllerID.=$id;
            $basePath.=DIRECTORY_SEPARATOR.$id;
        }
    }
    protected function parseActionParams($pathInfo)
    {
        if(($pos=strpos($pathInfo,"/"))!==false)
        {
            $manager=this.getUrlManager();
            $manager.parsePathInfo((string)substr($pathInfo,$pos+1));
            $actionID=substr($pathInfo,0,$pos);
            return $manager.caseSensitive ? $actionID : strtolower($actionID);
        }
        else
            return $pathInfo;
    }
    public String getController()
    {
        return this._controller;
    }
    public void setController(String value)
    {
        this._controller=value;
    }
    public String getControllerPath()
    {
        if(this._controllerPath!=null)
            return this._controllerPath;
        else
            return this._controllerPath=this.getBasePath()+DIRECTORY_SEPARATOR+"controllers";
    }
    public void setControllerPath(String value)
    {
        if((this._controllerPath=realpath(value)) == null || !is_dir(this._controllerPath))
            throw new CException(Yii.t("yii","The controller path \"{path}\" is not a valid directory.",
                array("{path}", value)));
    }
    public String getViewPath()
    {
        if(this._viewPath != null)
            return this._viewPath;
        else
            return this._viewPath=this.getBasePath() + DIRECTORY_SEPARATOR + "views";
    }
    public void setViewPath(String path)
    {
        if((this._viewPath=realpath(path)) == null || !is_dir(this._viewPath))
            throw new CException(Yii.t("yii","The view path \"{path}\" is not a valid directory.",
                    array("{path}", path)));
    }
    public String getSystemViewPath()
    {
        if(this._systemViewPath != null)
            return this._systemViewPath;
        else
            return this._systemViewPath=this.getViewPath() + DIRECTORY_SEPARATOR + "system";
    }
    public void setSystemViewPath(String path)
    {
        if((this._systemViewPath=realpath(path)) == null || !is_dir(this._systemViewPath))
            throw new CException(Yii.t("yii","The system view path \"{path}\" is not a valid directory.",
                array("{path}", path)));
    }
    public String getLayoutPath()
    {
        if(this._layoutPath!=null)
            return this._layoutPath;
        else
            return this._layoutPath=this.getViewPath()+DIRECTORY_SEPARATOR+"layouts";
    }
    public void setLayoutPath(String path)
    {
        if((this._layoutPath=realpath(path)) == null || !is_dir(this._layoutPath))
            throw new CException(Yii.t("yii","The layout path \"{path}\" is not a valid directory.",
                array("{path}", path)));
    }
    public boolean beforeControllerAction(String controller, String action)
    {
        return true;
    }
    public void afterControllerAction(String controller, String action)
    {
    }
    public CModule findModule(String id)
    {
        String controller;
        Object module;
        if((controller=this.getController())!=null && (module=controller.getModule())!=null)
        {
            do
            {
                if(($m=$module.getModule($id))!==null)
                    return $m;
            } while(($module=$module.getParentModule())!==null);
        }
        if(($m=this.getModule($id))!==null)
            return $m;
    }
    protected function init()
    {
        parent::init();
        // preload 'request' so that it has chance to respond to onBeginRequest event.
        this.getRequest();
    }
}
