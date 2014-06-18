/**
 * 
 */
package net.geecode.framework.lite;

import static net.geecode.php.base.Global.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.geecode.framework.base.CApplication;
import net.geecode.framework.base.CException;

/**
 * @author David
 *
 */

public class YiiBase
{
    public static Map<String, Object> classMap = array("");
    public static boolean enableIncludePath=true;
    private static Map<String, Object> _aliases = array("system", YII_PATH,"zii",YII_ZII_PATH); // alias => path
    private static Map<String, Object> _imports=array();                   // alias => class name or directory
    private static List<String> _includePaths;                      // list of include paths
    private static CApplication _app;
    private static CLogger _logger;
    public static String getVersion()
    {
        return "1.1.14";
    }
    
    public static Object createWebApplication(Map<String, Object> config/*=null*/)
    {
        return createApplication(CWebApplication.class, config);
    }
    
//    public static Object createConsoleApplication(Map<String, Object> config/*=null*/)
//    {
//        return createApplication(CConsoleApplication.class, config);
//    }
    
    public static Object createApplication(Class $class, Map<String, Object> $config/*=null*/)
    {
        return $class.getConstructor(new Class[]{$config.getClass()}).newInstance($config);
    }
    
    public static CApplication app()
    {
        return _app;
    }
    
    public static void setApplication(CApplication app)
    {
        if(_app==null || app==null)
            _app = app;
        else
            throw new CException(Yii.t("yii","Yii application can only be created once."));
    }
    
    public static String getFrameworkPath()
    {
        return YII_PATH;
    }
    public static IApplicationComponent createComponent(Object conf, Object ... $args)
    {
        String type = null;
        Map<String, Object> config = (Map<String, Object>) conf;
        if(is_string(conf))
        {
            type=(String)conf;
            config=array();
        }
        else if(isset(config, "class"))
        {
            type = (String) config.get("class");
            config.remove("class");
        }
        else
            throw new CException(Yii.t("yii","Object configuration must be an array containing a \"class\" element."));
//        if(!class_exists(type,false))
//            type = Yii::import(type,true);
        int $n = $args.length + 1;
        Object $object = null;
        Class cls = Class.forName(type);
        if($n>1)
        {
            if($n==2)
                $object=$args[0].getClass();
            else if($n==3)
                $object=cls.getConstructors()[0].newInstance($args);
            else if($n==4)
                $object=cls.getConstructors()[0].newInstance($args);
            else
            {
//                unset($args[0]);
                
                Class<?> $class = Class.forName(type);/*new ReflectionClass($type);*/
                // Note: ReflectionClass::newInstanceArgs() is available for PHP 5.1.3+
                // $object=$class->newInstanceArgs($args);
//                $object = call_user_func_array(array($class,"newInstance"),$args);
            }
        }
        else
            $object=cls.newInstance();
        for(Entry<String, Object> ent : config.entrySet()/* as $key,$value*/)
        {
            String $key = ent.getKey();
            Object $value = ent.getValue();
            Field f = $object.getClass().getDeclaredField($key);
            if (null != f)
            {
                f.set($object, $value);
            }
//            $object->$key=$value;
        }
        return (IApplicationComponent) $object;
    }
    
//    public static function _import(String alias, boolean forceInclude/*=false*/)
//    {
//        if(isset($_imports, alias))  // previously imported
//            return $_imports.get(alias);
//        if(class_exists(alias,false) || interface_exists(alias,false))
//            return self::$_imports[$alias]=$alias;
//        if(($pos=strrpos($alias,"\\"))!==false) // a class name in PHP 5.3 namespace format
//        {
//            $namespace=str_replace("\\",".",ltrim(substr($alias,0,$pos),"\\"));
//            if(($path=self::getPathOfAlias($namespace))!==false)
//            {
//                $classFile=$path.DIRECTORY_SEPARATOR.substr($alias,$pos+1).".php";
//                if($forceInclude)
//                {
//                    if(is_file($classFile))
//                        require($classFile);
//                    else
//                        throw new CException(Yii::t("yii","Alias "{alias}" is invalid. Make sure it points to an existing PHP file and the file is readable.",array("{alias}",$alias)));
//                    self::$_imports[$alias]=$alias;
//                }
//                else
//                    self::$classMap[$alias]=$classFile;
//                return $alias;
//            }
//            else
//            {
//                // try to autoload the class with an autoloader
//                if (class_exists($alias,true))
//                    return self::$_imports[$alias]=$alias;
//                else
//                    throw new CException(Yii::t("yii","Alias "{alias}" is invalid. Make sure it points to an existing directory or file.",
//                        array("{alias}",$namespace)));
//            }
//        }
//        if(($pos=strrpos($alias,"."))===false)  // a simple class name
//        {
//            if($forceInclude && self::autoload($alias))
//                self::$_imports[$alias]=$alias;
//            return $alias;
//        }
//        $className=(string)substr($alias,$pos+1);
//        $isClass=$className!=="*";
//        if($isClass && (class_exists($className,false) || interface_exists($className,false)))
//            return self::$_imports[$alias]=$className;
//        if(($path=self::getPathOfAlias($alias))!==false)
//        {
//            if($isClass)
//            {
//                if($forceInclude)
//                {
//                    if(is_file($path.".php"))
//                        require($path.".php");
//                    else
//                        throw new CException(Yii::t("yii","Alias "{alias}" is invalid. Make sure it points to an existing PHP file and the file is readable.",array("{alias}",$alias)));
//                    self::$_imports[$alias]=$className;
//                }
//                else
//                    self::$classMap[$className]=$path.".php";
//                return $className;
//            }
//            else  // a directory
//            {
//                if(self::$_includePaths===null)
//                {
//                    self::$_includePaths=array_unique(explode(PATH_SEPARATOR,get_include_path()));
//                    if(($pos=array_search(".",self::$_includePaths,true))!==false)
//                        unset(self::$_includePaths[$pos]);
//                }
//                array_unshift(self::$_includePaths,$path);
//                if(self::$enableIncludePath && set_include_path(".".PATH_SEPARATOR.implode(PATH_SEPARATOR,self::$_includePaths))===false)
//                    self::$enableIncludePath=false;
//                return self::$_imports[$alias]=$path;
//            }
//        }
//        else
//            throw new CException(Yii::t("yii","Alias "{alias}" is invalid. Make sure it points to an existing directory or file.",
//                array("{alias}", $alias)));
//    }
    
    public static String getPathOfAlias(String $alias)
    {
        if(isset(_aliases, $alias))
            return (String) _aliases.get($alias);
        else if($alias.contains("."))
        {
            int $pos = $alias.indexOf(".");
            String $rootAlias=$alias.substring(0,$pos);
            if(isset(_aliases, $rootAlias))
            {
                _aliases.put($alias, rtrim(_aliases.get($rootAlias)+DIRECTORY_SEPARATOR + str_replace(".",DIRECTORY_SEPARATOR, $alias.substring($pos+1)),"*"+DIRECTORY_SEPARATOR));
                return (String) _aliases.get($alias);
            }
            else if(_app instanceof CWebApplication)
            {
                if(((CWebApplication)_app).findModule($rootAlias)!=null)
                    return getPathOfAlias($alias);
            }
        }
        return null;
    }
    
    public static void setPathOfAlias(String $alias, String $path)
    {
        if($path.isEmpty())
            _aliases.remove($alias);
        else
            _aliases.put($alias, rtrim($path,"\\/"));
    }
    
//    public static boolean autoload(String $className)
//    {
//        // use include so that the error PHP file may appear
//        if(isset(self::$classMap[$className]))
//            include(self::$classMap[$className]);
//        elseif(isset(self::$_coreClasses[$className]))
//            include(YII_PATH.self::$_coreClasses[$className]);
//        else
//        {
//            // include class file relying on include_path
//            if(strpos($className,"\\")===false)  // class without namespace
//            {
//                if(self::$enableIncludePath===false)
//                {
//                    foreach(self::$_includePaths as $path)
//                    {
//                        $classFile=$path.DIRECTORY_SEPARATOR.$className.".php";
//                        if(is_file($classFile))
//                        {
//                            include($classFile);
//                            if(YII_DEBUG && basename(realpath($classFile))!==$className.".php")
//                                throw new CException(Yii::t("yii","Class name "{class}" does not match class file "{file}".", array(
//                                    "{class}",$className,
//                                    "{file}",$classFile,
//                                )));
//                            break;
//                        }
//                    }
//                }
//                else
//                    include($className.".php");
//            }
//            else  // class name with namespace in PHP 5.3
//            {
//                $namespace=str_replace("\\",".",ltrim($className,"\\"));
//                if(($path=self::getPathOfAlias($namespace))!==false)
//                    include($path.".php");
//                else
//                    return false;
//            }
//            return class_exists($className,false) || interface_exists($className,false);
//        }
//        return true;
//    }
    
    public static void trace(String $msg, String $category/*="application"*/)
    {
        if(YII_DEBUG)
            log($msg, CLogger.LEVEL_TRACE, $category);
    }
    
    public static void log(String $msg, String $level/*=CLogger::LEVEL_INFO*/,
            String $category/*="application"*/)
    {
        if(_logger==null)
            _logger = new CLogger();
        if(YII_DEBUG && YII_TRACE_LEVEL>0 && !$level.equals(CLogger.LEVEL_PROFILE))
        {
            StackTraceElement[] traces = Thread.currentThread().getStackTrace();
//            $traces=debug_backtrace();
            int $count=0;
            for(StackTraceElement trace : traces/* as $trace*/)
            {
                $msg += "\nin " + trace.getFileName()+ " ("+trace.getLineNumber()+")";
                if(++$count>=YII_TRACE_LEVEL)
                    break;
            }
        }
        _logger.log($msg,$level,$category);
    }
    
    public static void log(String msg, String level/*=CLogger::LEVEL_INFO*/)
    {
        log(msg, level, "application");
    }
    
    public static void log(String msg)
    {
        log(msg, CLogger.LEVEL_INFO);
    }
    
    public static void beginProfile(String $token, String $category/*="application"*/)
    {
        log("begin:"+$token, CLogger.LEVEL_PROFILE, $category);
    }
    
    public static void endProfile(String $token, String $category/*="application"*/)
    {
        log("end:"+$token, CLogger.LEVEL_PROFILE, $category);
    }
    
    public static CLogger getLogger()
    {
        if(_logger!=null)
            return _logger;
        else
            return _logger=new CLogger();
    }
    public static void setLogger(CLogger $logger)
    {
        _logger=$logger;
    }
    public static String powered()
    {
        return Yii.t("yii","Powered by {yii}.", array("{yii}", "<a href=\"http://www.yiiframework.com/\" rel=\"external\">Yii Framework</a>"), null, null);
    }
    
    public static String t(String $category,String $message,Map<String, Object> $params/*=array()*/,
            String $source/*=null*/, String $language/*=null*/)
    {
        if(_app!=null)
        {
            if($source==null)
                $source=($category=="yii"||$category=="zii")?"coreMessages":"messages";
            if(($source=(String) _app.getComponent($source))!=null)
                $message=$source.translate($category,$message,$language);
        }
        if($params==array())
            return $message;
        if(!is_array($params))
            $params=array($params);
        if(isset($params, 0)) // number choice
        {
            if($message.contains("|"))
            {
                if(!$message.contains("#"))
                {
                    String[] $chunks = explode("|",$message);
                    Object[] $expressions = _app.getLocale($language).getPluralRules();
                    Object $n;
                    if($n=min($chunks.length, $expressions.length))
                    {
                        for($i=0;$i<$n;$i++)
                            $chunks[$i]=$expressions[$i]+"#"+$chunks[$i];
                        $message=implode("|",$chunks);
                    }
                }
                $message=CChoiceFormat.format($message,$params[0]);
            }
            if(!isset($params, "{n}"))
                $params["{n}"]=$params[0];
            unset($params[0]);
        }
        return $params!=array() ? strtr($message,$params) : $message;
    }
    
    public static String t(String $category,String $message,Map<String, Object> $params/*=array()*/)
    {
        return t($category, $message, $params/*=array()*/, null, null);
    }

    public static String t(String $category,String $message)
    {
        return t($category, $message, array(), null, null);
    }
    
    public static void registerAutoloader($callback, boolean append/*=false*/)
    {
        if(append)
        {
            enableIncludePath=false;
            spl_autoload_register($callback);
        }
        else
        {
            spl_autoload_unregister(array("YiiBase","autoload"));
            spl_autoload_register($callback);
            spl_autoload_register(array("YiiBase","autoload"));
        }
    }
    private static Map<String, Object> _coreClasses=array(
        "CApplication" , "/base/CApplication.php",
        "CApplicationComponent" , "/base/CApplicationComponent.php",
        "CBehavior" , "/base/CBehavior.php",
        "CComponent" , "/base/CComponent.php",
        "CErrorEvent" , "/base/CErrorEvent.php",
        "CErrorHandler" , "/base/CErrorHandler.php",
        "CException" , "/base/CException.php",
        "CExceptionEvent" , "/base/CExceptionEvent.php",
        "CHttpException" , "/base/CHttpException.php",
        "CModel" , "/base/CModel.php",
        "CModelBehavior" , "/base/CModelBehavior.php",
        "CModelEvent" , "/base/CModelEvent.php",
        "CModule" , "/base/CModule.php",
        "CSecurityManager" , "/base/CSecurityManager.php",
        "CStatePersister" , "/base/CStatePersister.php",
        "CApcCache" , "/caching/CApcCache.php",
        "CCache" , "/caching/CCache.php",
        "CDbCache" , "/caching/CDbCache.php",
        "CDummyCache" , "/caching/CDummyCache.php",
        "CEAcceleratorCache" , "/caching/CEAcceleratorCache.php",
        "CFileCache" , "/caching/CFileCache.php",
        "CMemCache" , "/caching/CMemCache.php",
        "CRedisCache" , "/caching/CRedisCache.php",
        "CWinCache" , "/caching/CWinCache.php",
        "CXCache" , "/caching/CXCache.php",
        "CZendDataCache" , "/caching/CZendDataCache.php",
        "CCacheDependency" , "/caching/dependencies/CCacheDependency.php",
        "CChainedCacheDependency" , "/caching/dependencies/CChainedCacheDependency.php",
        "CDbCacheDependency" , "/caching/dependencies/CDbCacheDependency.php",
        "CDirectoryCacheDependency" , "/caching/dependencies/CDirectoryCacheDependency.php",
        "CExpressionDependency" , "/caching/dependencies/CExpressionDependency.php",
        "CFileCacheDependency" , "/caching/dependencies/CFileCacheDependency.php",
        "CGlobalStateCacheDependency" , "/caching/dependencies/CGlobalStateCacheDependency.php",
        "CAttributeCollection" , "/collections/CAttributeCollection.php",
        "CConfiguration" , "/collections/CConfiguration.php",
        "CList" , "/collections/CList.php",
        "CListIterator" , "/collections/CListIterator.php",
        "CMap" , "/collections/CMap.php",
        "CMapIterator" , "/collections/CMapIterator.php",
        "CQueue" , "/collections/CQueue.php",
        "CQueueIterator" , "/collections/CQueueIterator.php",
        "CStack" , "/collections/CStack.php",
        "CStackIterator" , "/collections/CStackIterator.php",
        "CTypedList" , "/collections/CTypedList.php",
        "CTypedMap" , "/collections/CTypedMap.php",
        "CConsoleApplication" , "/console/CConsoleApplication.php",
        "CConsoleCommand" , "/console/CConsoleCommand.php",
        "CConsoleCommandBehavior" , "/console/CConsoleCommandBehavior.php",
        "CConsoleCommandEvent" , "/console/CConsoleCommandEvent.php",
        "CConsoleCommandRunner" , "/console/CConsoleCommandRunner.php",
        "CHelpCommand" , "/console/CHelpCommand.php",
        "CDbCommand" , "/db/CDbCommand.php",
        "CDbConnection" , "/db/CDbConnection.php",
        "CDbDataReader" , "/db/CDbDataReader.php",
        "CDbException" , "/db/CDbException.php",
        "CDbMigration" , "/db/CDbMigration.php",
        "CDbTransaction" , "/db/CDbTransaction.php",
        "CActiveFinder" , "/db/ar/CActiveFinder.php",
        "CActiveRecord" , "/db/ar/CActiveRecord.php",
        "CActiveRecordBehavior" , "/db/ar/CActiveRecordBehavior.php",
        "CDbColumnSchema" , "/db/schema/CDbColumnSchema.php",
        "CDbCommandBuilder" , "/db/schema/CDbCommandBuilder.php",
        "CDbCriteria" , "/db/schema/CDbCriteria.php",
        "CDbExpression" , "/db/schema/CDbExpression.php",
        "CDbSchema" , "/db/schema/CDbSchema.php",
        "CDbTableSchema" , "/db/schema/CDbTableSchema.php",
        "CMssqlColumnSchema" , "/db/schema/mssql/CMssqlColumnSchema.php",
        "CMssqlCommandBuilder" , "/db/schema/mssql/CMssqlCommandBuilder.php",
        "CMssqlPdoAdapter" , "/db/schema/mssql/CMssqlPdoAdapter.php",
        "CMssqlSchema" , "/db/schema/mssql/CMssqlSchema.php",
        "CMssqlSqlsrvPdoAdapter" , "/db/schema/mssql/CMssqlSqlsrvPdoAdapter.php",
        "CMssqlTableSchema" , "/db/schema/mssql/CMssqlTableSchema.php",
        "CMysqlColumnSchema" , "/db/schema/mysql/CMysqlColumnSchema.php",
        "CMysqlCommandBuilder" , "/db/schema/mysql/CMysqlCommandBuilder.php",
        "CMysqlSchema" , "/db/schema/mysql/CMysqlSchema.php",
        "CMysqlTableSchema" , "/db/schema/mysql/CMysqlTableSchema.php",
        "COciColumnSchema" , "/db/schema/oci/COciColumnSchema.php",
        "COciCommandBuilder" , "/db/schema/oci/COciCommandBuilder.php",
        "COciSchema" , "/db/schema/oci/COciSchema.php",
        "COciTableSchema" , "/db/schema/oci/COciTableSchema.php",
        "CPgsqlColumnSchema" , "/db/schema/pgsql/CPgsqlColumnSchema.php",
        "CPgsqlCommandBuilder" , "/db/schema/pgsql/CPgsqlCommandBuilder.php",
        "CPgsqlSchema" , "/db/schema/pgsql/CPgsqlSchema.php",
        "CPgsqlTableSchema" , "/db/schema/pgsql/CPgsqlTableSchema.php",
        "CSqliteColumnSchema" , "/db/schema/sqlite/CSqliteColumnSchema.php",
        "CSqliteCommandBuilder" , "/db/schema/sqlite/CSqliteCommandBuilder.php",
        "CSqliteSchema" , "/db/schema/sqlite/CSqliteSchema.php",
        "CChoiceFormat" , "/i18n/CChoiceFormat.php",
        "CDateFormatter" , "/i18n/CDateFormatter.php",
        "CDbMessageSource" , "/i18n/CDbMessageSource.php",
        "CGettextMessageSource" , "/i18n/CGettextMessageSource.php",
        "CLocale" , "/i18n/CLocale.php",
        "CMessageSource" , "/i18n/CMessageSource.php",
        "CNumberFormatter" , "/i18n/CNumberFormatter.php",
        "CPhpMessageSource" , "/i18n/CPhpMessageSource.php",
        "CGettextFile" , "/i18n/gettext/CGettextFile.php",
        "CGettextMoFile" , "/i18n/gettext/CGettextMoFile.php",
        "CGettextPoFile" , "/i18n/gettext/CGettextPoFile.php",
        "CChainedLogFilter" , "/logging/CChainedLogFilter.php",
        "CDbLogRoute" , "/logging/CDbLogRoute.php",
        "CEmailLogRoute" , "/logging/CEmailLogRoute.php",
        "CFileLogRoute" , "/logging/CFileLogRoute.php",
        "CLogFilter" , "/logging/CLogFilter.php",
        "CLogRoute" , "/logging/CLogRoute.php",
        "CLogRouter" , "/logging/CLogRouter.php",
        "CLogger" , "/logging/CLogger.php",
        "CProfileLogRoute" , "/logging/CProfileLogRoute.php",
        "CWebLogRoute" , "/logging/CWebLogRoute.php",
        "CDateTimeParser" , "/utils/CDateTimeParser.php",
        "CFileHelper" , "/utils/CFileHelper.php",
        "CFormatter" , "/utils/CFormatter.php",
        "CLocalizedFormatter" , "/utils/CLocalizedFormatter.php",
        "CMarkdownParser" , "/utils/CMarkdownParser.php",
        "CPasswordHelper" , "/utils/CPasswordHelper.php",
        "CPropertyValue" , "/utils/CPropertyValue.php",
        "CTimestamp" , "/utils/CTimestamp.php",
        "CVarDumper" , "/utils/CVarDumper.php",
        "CBooleanValidator" , "/validators/CBooleanValidator.php",
        "CCaptchaValidator" , "/validators/CCaptchaValidator.php",
        "CCompareValidator" , "/validators/CCompareValidator.php",
        "CDateValidator" , "/validators/CDateValidator.php",
        "CDefaultValueValidator" , "/validators/CDefaultValueValidator.php",
        "CEmailValidator" , "/validators/CEmailValidator.php",
        "CExistValidator" , "/validators/CExistValidator.php",
        "CFileValidator" , "/validators/CFileValidator.php",
        "CFilterValidator" , "/validators/CFilterValidator.php",
        "CInlineValidator" , "/validators/CInlineValidator.php",
        "CNumberValidator" , "/validators/CNumberValidator.php",
        "CRangeValidator" , "/validators/CRangeValidator.php",
        "CRegularExpressionValidator" , "/validators/CRegularExpressionValidator.php",
        "CRequiredValidator" , "/validators/CRequiredValidator.php",
        "CSafeValidator" , "/validators/CSafeValidator.php",
        "CStringValidator" , "/validators/CStringValidator.php",
        "CTypeValidator" , "/validators/CTypeValidator.php",
        "CUniqueValidator" , "/validators/CUniqueValidator.php",
        "CUnsafeValidator" , "/validators/CUnsafeValidator.php",
        "CUrlValidator" , "/validators/CUrlValidator.php",
        "CValidator" , "/validators/CValidator.php",
        "CActiveDataProvider" , "/web/CActiveDataProvider.php",
        "CArrayDataProvider" , "/web/CArrayDataProvider.php",
        "CAssetManager" , "/web/CAssetManager.php",
        "CBaseController" , "/web/CBaseController.php",
        "CCacheHttpSession" , "/web/CCacheHttpSession.php",
        "CClientScript" , "/web/CClientScript.php",
        "CController" , "/web/CController.php",
        "CDataProvider" , "/web/CDataProvider.php",
        "CDataProviderIterator" , "/web/CDataProviderIterator.php",
        "CDbHttpSession" , "/web/CDbHttpSession.php",
        "CExtController" , "/web/CExtController.php",
        "CFormModel" , "/web/CFormModel.php",
        "CHttpCookie" , "/web/CHttpCookie.php",
        "CHttpRequest" , "/web/CHttpRequest.php",
        "CHttpSession" , "/web/CHttpSession.php",
        "CHttpSessionIterator" , "/web/CHttpSessionIterator.php",
        "COutputEvent" , "/web/COutputEvent.php",
        "CPagination" , "/web/CPagination.php",
        "CSort" , "/web/CSort.php",
        "CSqlDataProvider" , "/web/CSqlDataProvider.php",
        "CTheme" , "/web/CTheme.php",
        "CThemeManager" , "/web/CThemeManager.php",
        "CUploadedFile" , "/web/CUploadedFile.php",
        "CUrlManager" , "/web/CUrlManager.php",
        "CWebApplication" , "/web/CWebApplication.php",
        "CWebModule" , "/web/CWebModule.php",
        "CWidgetFactory" , "/web/CWidgetFactory.php",
        "CAction" , "/web/actions/CAction.php",
        "CInlineAction" , "/web/actions/CInlineAction.php",
        "CViewAction" , "/web/actions/CViewAction.php",
        "CAccessControlFilter" , "/web/auth/CAccessControlFilter.php",
        "CAuthAssignment" , "/web/auth/CAuthAssignment.php",
        "CAuthItem" , "/web/auth/CAuthItem.php",
        "CAuthManager" , "/web/auth/CAuthManager.php",
        "CBaseUserIdentity" , "/web/auth/CBaseUserIdentity.php",
        "CDbAuthManager" , "/web/auth/CDbAuthManager.php",
        "CPhpAuthManager" , "/web/auth/CPhpAuthManager.php",
        "CUserIdentity" , "/web/auth/CUserIdentity.php",
        "CWebUser" , "/web/auth/CWebUser.php",
        "CFilter" , "/web/filters/CFilter.php",
        "CFilterChain" , "/web/filters/CFilterChain.php",
        "CHttpCacheFilter" , "/web/filters/CHttpCacheFilter.php",
        "CInlineFilter" , "/web/filters/CInlineFilter.php",
        "CForm" , "/web/form/CForm.php",
        "CFormButtonElement" , "/web/form/CFormButtonElement.php",
        "CFormElement" , "/web/form/CFormElement.php",
        "CFormElementCollection" , "/web/form/CFormElementCollection.php",
        "CFormInputElement" , "/web/form/CFormInputElement.php",
        "CFormStringElement" , "/web/form/CFormStringElement.php",
        "CGoogleApi" , "/web/helpers/CGoogleApi.php",
        "CHtml" , "/web/helpers/CHtml.php",
        "CJSON" , "/web/helpers/CJSON.php",
        "CJavaScript" , "/web/helpers/CJavaScript.php",
        "CJavaScriptExpression" , "/web/helpers/CJavaScriptExpression.php",
        "CPradoViewRenderer" , "/web/renderers/CPradoViewRenderer.php",
        "CViewRenderer" , "/web/renderers/CViewRenderer.php",
        "CWebService" , "/web/services/CWebService.php",
        "CWebServiceAction" , "/web/services/CWebServiceAction.php",
        "CWsdlGenerator" , "/web/services/CWsdlGenerator.php",
        "CActiveForm" , "/web/widgets/CActiveForm.php",
        "CAutoComplete" , "/web/widgets/CAutoComplete.php",
        "CClipWidget" , "/web/widgets/CClipWidget.php",
        "CContentDecorator" , "/web/widgets/CContentDecorator.php",
        "CFilterWidget" , "/web/widgets/CFilterWidget.php",
        "CFlexWidget" , "/web/widgets/CFlexWidget.php",
        "CHtmlPurifier" , "/web/widgets/CHtmlPurifier.php",
        "CInputWidget" , "/web/widgets/CInputWidget.php",
        "CMarkdown" , "/web/widgets/CMarkdown.php",
        "CMaskedTextField" , "/web/widgets/CMaskedTextField.php",
        "CMultiFileUpload" , "/web/widgets/CMultiFileUpload.php",
        "COutputCache" , "/web/widgets/COutputCache.php",
        "COutputProcessor" , "/web/widgets/COutputProcessor.php",
        "CStarRating" , "/web/widgets/CStarRating.php",
        "CTabView" , "/web/widgets/CTabView.php",
        "CTextHighlighter" , "/web/widgets/CTextHighlighter.php",
        "CTreeView" , "/web/widgets/CTreeView.php",
        "CWidget" , "/web/widgets/CWidget.php",
        "CCaptcha" , "/web/widgets/captcha/CCaptcha.php",
        "CCaptchaAction" , "/web/widgets/captcha/CCaptchaAction.php",
        "CBasePager" , "/web/widgets/pagers/CBasePager.php",
        "CLinkPager" , "/web/widgets/pagers/CLinkPager.php",
        "CListPager" , "/web/widgets/pagers/CListPager.php"
    );
}