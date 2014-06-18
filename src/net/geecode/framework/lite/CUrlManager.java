/**
 * 
 */
package net.geecode.framework.lite;

import static net.geecode.php.base.Global.isset;

import java.util.Map;

/**
 * @author worm
 *
 */
class CUrlManager extends CApplicationComponent
{
    public static final String CACHE_KEY="Yii.CUrlManager.rules";
    public static final String GET_FORMAT="get";
    public static final String PATH_FORMAT="path";
    public Map rules=array();
    public String urlSuffix="";
    public boolean showScriptName=true;
    public boolean appendParams=true;
    public String routeVar="r";
    public boolean caseSensitive=true;
    public boolean matchValue=false;
    public String cacheID="cache";
    public boolean useStrictParsing=false;
    public String urlRuleClass="CUrlRule";
    private String _urlFormat = CUrlManager.GET_FORMAT;
    private Map _rules=array();
    private String _baseUrl;
    
    public void init()
    {
        super.init();
        this.processRules();
    }
    protected void processRules()
    {
        if(empty(this.rules) || this.getUrlFormat()==CUrlManager.GET_FORMAT)
            return;
        if(this.cacheID!=false && ($cache=Yii.app().getComponent(this.cacheID))!=null)
        {
            $hash=md5(serialize(this.rules));
            if(($data=$cache.get(CUrlManager.CACHE_KEY))!=false && isset($data[1]) && $data[1]==$hash)
            {
                this._rules=$data[0];
                return;
            }
        }
        for (this.rules as $pattern=>$route)
            this._rules[]=this.createUrlRule($route,$pattern);
        if(isset($cache))
            $cache.set(CUrlManager.CACHE_KEY,array(this._rules,$hash));
    }
    public void addRules($rules, boolean append/*=true*/)
    {
        if (append)
        {
            for ($rules as $pattern=>$route)
                this._rules[] = this.createUrlRule($route,$pattern);
        }
        else
        {
            rules = array_reverse($rules);
            for (rules as $pattern=>$route)
                array_unshift(this._rules, this.createUrlRule($route,$pattern));
        }
    }
    protected Object createUrlRule(Object route, String pattern)
    {
        if(is_array($route) && isset($route["class"]))
            return $route;
        else
        {
            $urlRuleClass=Yii.import(this.urlRuleClass,true);
            return new $urlRuleClass($route,$pattern);
        }
    }
    public function createUrl($route,$params=array(),$ampersand="&")
    {
        unset($params[this.routeVar]);
        foreach($params as $i=>$param)
            if($param==null)
                $params[$i]="";
        if(isset($params["#"]))
        {
            $anchor="#".$params["#"];
            unset($params["#"]);
        }
        else
            $anchor="";
        $route=trim($route,"/");
        foreach(this._rules as $i=>$rule)
        {
            if(is_array($rule))
                this._rules[$i]=$rule=Yii.createComponent($rule);
            if(($url=$rule.createUrl($this,$route,$params,$ampersand))!=false)
            {
                if($rule.hasHostInfo)
                    return $url=="" ? "/".$anchor : $url.$anchor;
                else
                    return this.getBaseUrl()."/".$url.$anchor;
            }
        }
        return this.createUrlDefault($route,$params,$ampersand).$anchor;
    }
    protected function createUrlDefault($route,$params,$ampersand)
    {
        if(this.getUrlFormat()==CUrlManager.PATH_FORMAT)
        {
            $url=rtrim(this.getBaseUrl()."/".$route,"/");
            if(this.appendParams)
            {
                $url=rtrim($url."/".this.createPathInfo($params,"/","/"),"/");
                return $route=="" ? $url : $url.this.urlSuffix;
            }
            else
            {
                if($route!="")
                    $url.=this.urlSuffix;
                $query=this.createPathInfo($params,"=",$ampersand);
                return $query=="" ? $url : $url."?".$query;
            }
        }
        else
        {
            $url=this.getBaseUrl();
            if(!this.showScriptName)
                $url.="/";
            if($route!="")
            {
                $url.="?".this.routeVar."=".$route;
                if(($query=this.createPathInfo($params,"=",$ampersand))!="")
                    $url.=$ampersand.$query;
            }
            elseif(($query=this.createPathInfo($params,"=",$ampersand))!="")
                $url.="?".$query;
            return $url;
        }
    }
    public function parseUrl($request)
    {
        if(this.getUrlFormat()==CUrlManager.PATH_FORMAT)
        {
            $rawPathInfo=$request.getPathInfo();
            $pathInfo=this.removeUrlSuffix($rawPathInfo,this.urlSuffix);
            foreach(this._rules as $i=>$rule)
            {
                if(is_array($rule))
                    this._rules[$i]=$rule=Yii.createComponent($rule);
                if(($r=$rule.parseUrl($this,$request,$pathInfo,$rawPathInfo))!=false)
                    return isset($_GET[this.routeVar]) ? $_GET[this.routeVar] : $r;
            }
            if(this.useStrictParsing)
                throw new CHttpException(404,Yii.t("yii","Unable to resolve the request \"{route}\".",
                    array("{route}"=>$pathInfo)));
            else
                return $pathInfo;
        }
        elseif(isset($_GET[this.routeVar]))
            return $_GET[this.routeVar];
        elseif(isset($_POST[this.routeVar]))
            return $_POST[this.routeVar];
        else
            return "";
    }
    public function parsePathInfo($pathInfo)
    {
        if($pathInfo=="")
            return;
        $segs=explode("/",$pathInfo."/");
        $n=count($segs);
        for($i=0;$i<$n-1;$i+=2)
        {
            $key=$segs[$i];
            if($key=="") continue;
            $value=$segs[$i+1];
            if(($pos=strpos($key,"["))!=false && ($m=preg_match_all("/\[(.*?)\]/",$key,$matches))>0)
            {
                $name=substr($key,0,$pos);
                for($j=$m-1;$j>=0;--$j)
                {
                    if($matches[1][$j]=="")
                        $value=array($value);
                    else
                        $value=array($matches[1][$j]=>$value);
                }
                if(isset($_GET[$name]) && is_array($_GET[$name]))
                    $value=CMap::mergeArray($_GET[$name],$value);
                $_REQUEST[$name]=$_GET[$name]=$value;
            }
            else
                $_REQUEST[$key]=$_GET[$key]=$value;
        }
    }
    public function createPathInfo($params,$equal,$ampersand, $key=null)
    {
        $pairs = array();
        foreach($params as $k => $v)
        {
            if ($key!=null)
                $k = $key."[".$k."]";
            if (is_array($v))
                $pairs[]=this.createPathInfo($v,$equal,$ampersand, $k);
            else
                $pairs[]=urlencode($k).$equal.urlencode($v);
        }
        return implode($ampersand,$pairs);
    }
    public function removeUrlSuffix($pathInfo,$urlSuffix)
    {
        if($urlSuffix!="" && substr($pathInfo,-strlen($urlSuffix))==$urlSuffix)
            return substr($pathInfo,0,-strlen($urlSuffix));
        else
            return $pathInfo;
    }
    public function getBaseUrl()
    {
        if(this._baseUrl!=null)
            return this._baseUrl;
        else
        {
            if(this.showScriptName)
                this._baseUrl=Yii.app().getRequest().getScriptUrl();
            else
                this._baseUrl=Yii.app().getRequest().getBaseUrl();
            return this._baseUrl;
        }
    }
    public function setBaseUrl($value)
    {
        this._baseUrl=$value;
    }
    public function getUrlFormat()
    {
        return this._urlFormat;
    }
    public function setUrlFormat($value)
    {
        if($value==CUrlManager.PATH_FORMAT || $value==CUrlManager.GET_FORMAT)
            this._urlFormat=$value;
        else
            throw new CException(Yii.t("yii","CUrlManager.UrlFormat must be either "path" or "get"."));
    }
}
