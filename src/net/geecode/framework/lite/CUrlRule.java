/**
 * 
 */
package net.geecode.framework.lite;

import java.util.Map;

import net.geecode.framework.base.CComponent;

/**
 * @author worm
 *
 */
abstract class CBaseUrlRule extends CComponent
{
    public boolean hasHostInfo=false;
    abstract public String createUrl(CUrlManager manager, String route, Map params, String ampersand);
    abstract public String parseUrl(CUrlManager manager, CHttpRequest request, String pathInfo, String rawPathInfo);
}
public class CUrlRule extends CBaseUrlRule
{
    public String urlSuffix;
    public boolean caseSensitive;
    public Map<String, Object> defaultParams = array();
    public Object matchValue;
    public Object verb;
    public boolean parsingOnly=false;
    public String route;
    public Map<String, Object> references = array();
    public String routePattern;
    public $pattern;
    public $template;
    public $params=array();
    public $append;
    public $hasHostInfo;
    public function __construct($route,$pattern)
    {
        if(is_array($route))
        {
            foreach(array("urlSuffix", "caseSensitive", "defaultParams", "matchValue", "verb", "parsingOnly") as $name)
            {
                if(isset($route[$name]))
                    $this->$name=$route[$name];
            }
            if(isset($route["pattern"]))
                $pattern=$route["pattern"];
            $route=$route[0];
        }
        $this->route=trim($route,"/");
        $tr2["/"]=$tr["/"]="\\/";
        if(strpos($route,"<")!==false && preg_match_all("/<(\w+)>/",$route,$matches2))
        {
            foreach($matches2[1] as $name)
                $this->references[$name]="<$name>";
        }
        $this->hasHostInfo=!strncasecmp($pattern,"http://",7) || !strncasecmp($pattern,"https://",8);
        if($this->verb!==null)
            $this->verb=preg_split("/[\s,]+/",strtoupper($this->verb),-1,PREG_SPLIT_NO_EMPTY);
        if(preg_match_all("/<(\w+):?(.*?)?>/",$pattern,$matches))
        {
            $tokens=array_combine($matches[1],$matches[2]);
            foreach($tokens as $name=>$value)
            {
                if($value==="")
                    $value="[^\/]+";
                $tr["<$name>"]="(?P<$name>$value)";
                if(isset($this->references[$name]))
                    $tr2["<$name>"]=$tr["<$name>"];
                else
                    $this->params[$name]=$value;
            }
        }
        $p=rtrim($pattern,"*");
        $this->append=$p!==$pattern;
        $p=trim($p,"/");
        $this->template=preg_replace("/<(\w+):?.*?>/","<$1>",$p);
        $this->pattern="/^".strtr($this->template,$tr)."\/";
        if($this->append)
            $this->pattern.="/u";
        else
            $this->pattern.="$/u";
        if($this->references!==array())
            $this->routePattern="/^".strtr($this->route,$tr2)."$/u";
        if(YII_DEBUG && @preg_match($this->pattern,"test")===false)
            throw new CException(Yii::t("yii","The URL pattern "{pattern}" for route "{route}" is not a valid regular expression.",
                array("{route}"=>$route,"{pattern}"=>$pattern)));
    }
    public function createUrl($manager,$route,$params,$ampersand)
    {
        if($this->parsingOnly)
            return false;
        if($manager->caseSensitive && $this->caseSensitive===null || $this->caseSensitive)
            $case="";
        else
            $case="i";
        $tr=array();
        if($route!==$this->route)
        {
            if($this->routePattern!==null && preg_match($this->routePattern.$case,$route,$matches))
            {
                foreach($this->references as $key=>$name)
                    $tr[$name]=$matches[$key];
            }
            else
                return false;
        }
        foreach($this->defaultParams as $key=>$value)
        {
            if(isset($params[$key]))
            {
                if($params[$key]==$value)
                    unset($params[$key]);
                else
                    return false;
            }
        }
        foreach($this->params as $key=>$value)
            if(!isset($params[$key]))
                return false;
        if($manager->matchValue && $this->matchValue===null || $this->matchValue)
        {
            foreach($this->params as $key=>$value)
            {
                if(!preg_match("/\A".$value."\z/u".$case,$params[$key]))
                    return false;
            }
        }
        foreach($this->params as $key=>$value)
        {
            $tr["<$key>"]=urlencode($params[$key]);
            unset($params[$key]);
        }
        $suffix=$this->urlSuffix===null ? $manager->urlSuffix : $this->urlSuffix;
        $url=strtr($this->template,$tr);
        if($this->hasHostInfo)
        {
            $hostInfo=Yii::app()->getRequest()->getHostInfo();
            if(stripos($url,$hostInfo)===0)
                $url=substr($url,strlen($hostInfo));
        }
        if(empty($params))
            return $url!=="" ? $url.$suffix : $url;
        if($this->append)
            $url.="/".$manager->createPathInfo($params,"/","/").$suffix;
        else
        {
            if($url!=="")
                $url.=$suffix;
            $url.="?".$manager->createPathInfo($params,"=",$ampersand);
        }
        return $url;
    }
    public function parseUrl($manager,$request,$pathInfo,$rawPathInfo)
    {
        if($this->verb!==null && !in_array($request->getRequestType(), $this->verb, true))
            return false;
        if($manager->caseSensitive && $this->caseSensitive===null || $this->caseSensitive)
            $case="";
        else
            $case="i";
        if($this->urlSuffix!==null)
            $pathInfo=$manager->removeUrlSuffix($rawPathInfo,$this->urlSuffix);
        // URL suffix required, but not found in the requested URL
        if($manager->useStrictParsing && $pathInfo===$rawPathInfo)
        {
            $urlSuffix=$this->urlSuffix===null ? $manager->urlSuffix : $this->urlSuffix;
            if($urlSuffix!="" && $urlSuffix!=="/")
                return false;
        }
        if($this->hasHostInfo)
            $pathInfo=strtolower($request->getHostInfo()).rtrim("/".$pathInfo,"/");
        $pathInfo.="/";
        if(preg_match($this->pattern.$case,$pathInfo,$matches))
        {
            foreach($this->defaultParams as $name=>$value)
            {
                if(!isset($_GET[$name]))
                    $_REQUEST[$name]=$_GET[$name]=$value;
            }
            $tr=array();
            foreach($matches as $key=>$value)
            {
                if(isset($this->references[$key]))
                    $tr[$this->references[$key]]=$value;
                elseif(isset($this->params[$key]))
                    $_REQUEST[$key]=$_GET[$key]=$value;
            }
            if($pathInfo!==$matches[0]) // there're additional GET params
                $manager->parsePathInfo(ltrim(substr($pathInfo,strlen($matches[0])),"/"));
            if($this->routePattern!==null)
                return strtr($this->route,$tr);
            else
                return $this->route;
        }
        else
            return false;
    }
}
