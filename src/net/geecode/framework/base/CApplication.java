/**
 * 
 */
package net.geecode.framework.base;

import java.io.File;
import java.util.Map;

import net.geecode.framework.base.CException;
import net.geecode.framework.lite.CEvent;
import net.geecode.framework.lite.CModule;
import net.geecode.framework.lite.Yii;
import net.geecode.php.base.Global;
import static net.geecode.php.base.Global.*;

/**
 * @author David
 *
 */
public abstract class CApplication extends CModule
{
    public String name="My Application";
    public String charset="UTF-8";
    public String sourceLanguage="en_us";
    private String _id;
    private String _basePath;
    private String _runtimePath;
    private String _extensionPath;
    private Map<String, Object> _globalState;
    private boolean _stateChanged;
    private boolean _ended=false;
    private String _language;
    private String _homeUrl;
    
    abstract public void processRequest();
    
    public CApplication(Map $config/*=null*/) throws CException
    {
        super(null, null, $config);
        Yii.setApplication(this);
        // set basePath at early as possible to avoid trouble
//        if(is_string($config))
//            $config=require($config);
        if(isset($config, "basePath"))
        {
            this.setBasePath((String) $config.get("basePath"));
            $config.remove("basePath");
        }
        else
            this.setBasePath("protected");
        Yii.setPathOfAlias("application", this.getBasePath());
        Yii.setPathOfAlias("webroot", dirname((String)$_SERVER.get("SCRIPT_FILENAME")));
        if(isset($config, "extensionPath"))
        {
            this.setExtensionPath((String) $config.get("extensionPath"));
            $config.remove("extensionPath");
        }
        else
            Yii.setPathOfAlias("ext", this.getBasePath() + DIRECTORY_SEPARATOR + "extensions");
        if(isset($config, "aliases"))
        {
            this.setAliases((Map<String, String>) $config.get("aliases"));
            $config.remove("aliases");
        }
        this.preinit();
        this.initSystemHandlers();
        this.registerCoreComponents();
        this.configure($config);
        this.attachBehaviors(this.behaviors);
        this.preloadComponents();
        this.init();
    }
    
    public void run()
    {
        if(this.hasEventHandler("onBeginRequest"))
            this.onBeginRequest(new CEvent(this));
        register_shutdown_function(array(this,"end"),0,false);
        this.processRequest();
        if(this.hasEventHandler("onEndRequest"))
            this.onEndRequest(new CEvent(this));
    }
    
    public void end(int status/*=0*/, boolean exit/*=true*/)
    {
        if(this.hasEventHandler("onEndRequest"))
            this.onEndRequest(new CEvent(this));
        if(exit)
            exit(status);
    }
    
    public void end(int status/*=0*/)
    {
        end(status, true);
    }
    
    public void onBeginRequest(CEvent event)
    {
        this.raiseEvent("onBeginRequest", event);
    }
    public void onEndRequest(CEvent event)
    {
        if(!this._ended)
        {
            this._ended=true;
            this.raiseEvent("onEndRequest",event);
        }
    }
    public String getId()
    {
        if(this._id!=null)
            return this._id;
        else
            return this._id=sprintf("%x",crc32(this.getBasePath()+this.name));
    }
    public void setId(String id)
    {
        this._id = id;
    }
    public String getBasePath()
    {
        return this._basePath;
    }
    public void setBasePath(String path) throws CException
    {
        if((this._basePath=realpath(path)) == null || !is_dir(this._basePath))
            throw new CException(Yii.t("yii","Application base path \"{path}\" is not a valid directory.",
                array("{path}", path), null, null));
    }
    public String getRuntimePath() throws CException
    {
        if(this._runtimePath!=null)
            return this._runtimePath;
        else
        {
            this.setRuntimePath(this.getBasePath()+DIRECTORY_SEPARATOR+"runtime");
            return this._runtimePath;
        }
    }
    public void setRuntimePath(String path) throws CException
    {
        String $runtimePath = null;
        if(($runtimePath=realpath(path))==null || !is_dir($runtimePath) || !is_writable($runtimePath))
            throw new CException(Yii.t("yii","Application runtime path \"{path}\" is not valid. Please make sure it is a directory writable by the Web server process.",
                array("{path}", path), null, null));
        this._runtimePath=$runtimePath;
    }
    public String getExtensionPath()
    {
        return Yii.getPathOfAlias("ext");
    }
    public void setExtensionPath(String path) throws CException
    {
        String $extensionPath = null;
        if(($extensionPath=realpath(path))==null || !is_dir($extensionPath))
            throw new CException(Yii.t("yii","Extension path \"{path}\" does not exist.",
                array("{path}", path)));
        Yii.setPathOfAlias("ext",$extensionPath);
    }
    public String getLanguage()
    {
        return this._language==null ? this.sourceLanguage : this._language;
    }
    public void setLanguage(String $language)
    {
        this._language=$language;
    }
    public String getTimeZone()
    {
        return date_default_timezone_get();
    }
    public void setTimeZone(String $value)
    {
        date_default_timezone_set($value);
    }
    public File findLocalizedFile(File $srcFile, String $srcLanguage/*=null*/, String $language/*=null*/)
    {
        if($srcLanguage==null)
            $srcLanguage=this.sourceLanguage;
        if($language==null)
            $language=this.getLanguage();
        if($language==$srcLanguage)
            return $srcFile;
        File $desiredFile=new File(dirname($srcFile.getPath()) + DIRECTORY_SEPARATOR + $language
                + DIRECTORY_SEPARATOR  + basename($srcFile));
        return is_file($desiredFile.getPath()) ? $desiredFile : $srcFile;
    }
    public CLocale getLocale(String $localeID/*=null*/)
    {
        return CLocale.getInstance($localeID==null?this.getLanguage():$localeID);
    }
    public String getLocaleDataPath()
    {
        return CLocale.$dataPath==null ? Yii.getPathOfAlias("system.i18n.data") : CLocale.$dataPath;
    }
    public void setLocaleDataPath(String $value)
    {
        CLocale.$dataPath=$value;
    }
    public String getNumberFormatter()
    {
        return this.getLocale().getNumberFormatter();
    }
    public Object getDateFormatter()
    {
        return this.getLocale().getDateFormatter();
    }
    public Object getDb()
    {
        return this.getComponent("db");
    }
    public Object getErrorHandler()
    {
        return this.getComponent("errorHandler");
    }
    public Object getSecurityManager()
    {
        return this.getComponent("securityManager");
    }
    public Object getStatePersister()
    {
        return this.getComponent("statePersister");
    }
    public Object getCache()
    {
        return this.getComponent("cache");
    }
    public Object getCoreMessages()
    {
        return this.getComponent("coreMessages");
    }
    public Object getMessages()
    {
        return this.getComponent("messages");
    }
    public Object getRequest()
    {
        return this.getComponent("request");
    }
    public Object getUrlManager()
    {
        return this.getComponent("urlManager");
    }
    public Object getController()
    {
        return null;
    }
    public String createUrl(String $route, Map $params/*=array()*/, String $ampersand/*="&"*/)
    {
        return this.getUrlManager().createUrl($route,$params,$ampersand);
    }
    
    public String createAbsoluteUrl(String $route, Map $params/*=array()*/,
            String $schema/*=""*/, String $ampersand/*="&"*/)    {
        String $url=this.createUrl($route,$params,$ampersand);
        if(strpos($url,"http")==0)
            return $url;
        else
            return this.getRequest().getHostInfo($schema).$url;
    }
    public String getBaseUrl(boolean $absolute/*=false*/)
    {
        return this.getRequest().getBaseUrl($absolute);
    }
    public String getHomeUrl()
    {
        if(this._homeUrl==null)
        {
            if(this.getUrlManager().showScriptName)
                return this.getRequest().getScriptUrl();
            else
                return this.getRequest().getBaseUrl()+"/";
        }
        else
            return this._homeUrl;
    }
    public void setHomeUrl(String $value)
    {
        this._homeUrl=$value;
    }
    public Object getGlobalState(String $key, Object $defaultValue/*=null*/)
    {
        if(this._globalState==null)
            this.loadGlobalState();
        if(isset(this._globalState, $key))
            return this._globalState[$key];
        else
            return $defaultValue;
    }
    public void setGlobalState(String $key, Object $value, Object $defaultValue/*=null*/)
    {
        if(this._globalState==null)
            this.loadGlobalState();
        $changed=this._stateChanged;
        if($value==$defaultValue)
        {
            if(isset(this._globalState, $key))
            {
                this._globalState.remove($key);
                this._stateChanged=true;
            }
        }
        else if(!isset(this._globalState, $key) || this._globalState.get($key)!=$value)
        {
            this._globalState.put($key, $value)/*[$key]=$value*/;
            this._stateChanged=true;
        }
        if(this._stateChanged!=$changed)
            this.attachEventHandler("onEndRequest",array(this,"saveGlobalState"));
    }
    public void clearGlobalState(String $key)
    {
        this.setGlobalState($key,true,true);
    }
    public void loadGlobalState()
    {
        $persister=this.getStatePersister();
        if((this._globalState=$persister.load())==null)
            this._globalState=array();
        this._stateChanged=false;
        this.detachEventHandler("onEndRequest",array(this,"saveGlobalState"));
    }
    public void saveGlobalState()
    {
        if(this._stateChanged)
        {
            this._stateChanged=false;
            this.detachEventHandler("onEndRequest",array(this,"saveGlobalState"));
            this.getStatePersister().save(this._globalState);
        }
    }
    public void handleException(Exception $exception)
    {
        // disable error capturing to avoid recursive errors
        restore_error_handler();
        restore_exception_handler();
        String $category = "exception." + get_class($exception);
        if($exception instanceof CHttpException)
            $category += "." + $exception.getMessage()/*statusCode*/;
        // php <5.2 doesn't support string conversion auto-magically
        String $message = $exception.toString();
        if(isset(Global.$_SERVER, "REQUEST_URI"))
            $message += "\nREQUEST_URI=" + $_SERVER.get("REQUEST_URI");
        if(isset($_SERVER, "HTTP_REFERER"))
            $message += "\nHTTP_REFERER=" + $_SERVER.get("HTTP_REFERER");
        $message += "\n---";
        Yii.log($message, CLogger.LEVEL_ERROR, $category);
        try
        {
            CExceptionEvent $event = new CExceptionEvent(this,$exception);
            this.onException($event);
            if(!$event.handled)
            {
                Object $handler;
                // try an error handler
                if(($handler=this.getErrorHandler())!=null)
                    $handler.handle($event);
                else
                    this.displayException($exception);
            }
        }
        catch(Exception $e)
        {
            this.displayException($e);
        }
        try
        {
            this.end(1);
        }
        catch(Exception $e)
        {
            // use the most primitive way to log error
            String $msg = get_class($e) + ": " + $e.getMessage() + " (" + $e.getFile() + ":" + $e.getLine()
                    + ")\n";
            $msg += $e.getTraceAsString() + "\n" + "Previous exception:\n";
            $msg += get_class($exception) + ": " + $exception.getMessage() + " (" +
                    $exception.getFile() + ":" + $exception.getLine() + ")\n";
            $msg += $exception.getTraceAsString() + "\n";
            $msg += "$_SERVER=" + var_export($_SERVER,true);
            error_log($msg);
            exit(1);
        }
    }
    public void handleError(boolean $code, String $message, String $file, int $line)
    {
        if($code & error_reporting())
        {
            // disable error capturing to avoid recursive errors
            restore_error_handler();
            restore_exception_handler();
            $log="$message ($file:$line)\nStack trace:\n";
//            $trace=debug_backtrace();
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            // skip the first 3 stacks as they do not tell the error position
//            if((trace.length)>3)
//                $trace=array_slice($trace,3);
            int i = 0;
            for(StackTraceElement el : trace/* as $i=>$t*/)
            {
//                if(!isset($t["file"]))
//                    $t["file"]="unknown";
//                if(!isset($t["line"]))
//                    $t["line"]=0;
//                if(!isset($t["function"]))
//                    $t["function"]="unknown";
                $log += String.format("#%d {%s}({%d}): ", i, el.getFileName(), el.getLineNumber());
//                if(isset($t["object"]) && is_object($t["object"]))
//                    $log .=get_class($t["object"])."->";
//                $log.="{$t['function']}()\n";
                $log += el.getClassName() + "->{" + el.getMethodName() + "}()\n";
            }
            
            if(isset($_SERVER, "REQUEST_URI"))
                $log += "REQUEST_URI=" + $_SERVER["REQUEST_URI"];
            Yii.log($log,CLogger.LEVEL_ERROR,"php");
            
            try
            {
//                Yii::import("CErrorEvent",true);
                $event=new CErrorEvent($this,$code,$message,$file,$line);
                $this.onError($event);
                if(!$event.handled)
                {
                    // try an error handler
                    if(($handler=$this.getErrorHandler())!=null)
                        $handler.handle($event);
                    else
                        $this.displayError($code,$message,$file,$line);
                }
            }
            catch(Exception $e)
            {
                this.displayException($e);
            }
            try
            {
                this.end(1);
            }
            catch(Exception $e)
            {
                // use the most primitive way to log error
                $msg = get_class($e) + ": " + $e.getMessage() + " (" + $e.getFile()
                        + ":" + $e.getLine() + ")\n" + $e.getTraceAsString() + "\n"
                        + "Previous error:\n" + $log + "\n" + "$_SERVER=" + var_export($_SERVER,true);
                error_log($msg);
                exit(1);
            }
        }
    }
    public void onException(CEvent $event)
    {
        this.raiseEvent("onException",$event);
    }
    public void onError(CEvent $event)
    {
        this.raiseEvent("onError",$event);
    }
    public void displayError(boolean $code, String $message, String $file, int $line)
    {
        if(YII_DEBUG)
        {
            echo( "<h1>PHP Error [$code]</h1>\n");
            echo( "<p>$message ($file:$line)</p>\n");
            echo( "<pre>");
//            $trace=debug_backtrace();
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            // skip the first 3 stacks as they do not tell the error position
//            if(count($trace)>3)
//                $trace=array_slice($trace,3);
            int i = 0;
            for(StackTraceElement t : trace)
            {
//                if(!isset($t["file"]))
//                    $t["file"]="unknown";
//                if(!isset($t["line"]))
//                    $t["line"]=0;
//                if(!isset($t["function"]))
//                    $t["function"]="unknown";
                echo (String.format("#%d {%s}({%d}): ", i, t.getFileName(), t.getLineNumber()));
                echo( t.getClassName() + "->");
                echo("{" + t.getMethodName() + "}()\n");
            }
            echo("</pre>");
        }
        else
        {
            echo ("<h1>PHP Error [$code]</h1>\n");
            echo ("<p>$message</p>\n");
        }
    }
    public void displayException(Exception $exception)
    {
        if(YII_DEBUG)
        {
            echo ("<h1>" + ($exception.getClass()) + "</h1>\n");
            echo ("<p>"+$exception.getMessage()+" ("+$exception.getFile()+":"+$exception.getLine()+")</p>");
            echo ("<pre>"+$exception.getTraceAsString()+"</pre>");
        }
        else
        {
            echo ("<h1>"+get_class($exception)+"</h1>\n");
            echo ("<p>"+$exception.getMessage()+"</p>");
        }
    }
    protected void initSystemHandlers()
    {
        if(YII_ENABLE_EXCEPTION_HANDLER)
            set_exception_handler(array(this,"handleException"));
        if(YII_ENABLE_ERROR_HANDLER)
            set_error_handler(array($this,"handleError"),error_reporting());
    }
    protected void registerCoreComponents()
    {
        Map<String, Object> $components = array(
            "coreMessages", array(
                "class", "CPhpMessageSource",
                "language", "en_us",
                "basePath", YII_PATH+DIRECTORY_SEPARATOR+"messages"
            ),
            "db", array(
                "class","CDbConnection"
            ),
            "messages", array(
                "class", "CPhpMessageSource"
            ),
            "errorHandler", array(
                "class", "CErrorHandler"
            ),
            "securityManager", array(
                "class", "CSecurityManager"
            ),
            "statePersister", array(
                "class", "CStatePersister"
            ),
            "urlManager", array(
                "class", "CUrlManager"
            ),
            "request", array(
                "class", "CHttpRequest"
            ),
            "format", array(
                "class", "CFormatter"
            )
        );
        this.setComponents($components);
    }
}
