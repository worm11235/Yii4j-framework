/**
 * 
 */
package net.geecode.framework.lite;

import static net.geecode.php.base.Global.$_SERVER;
import static net.geecode.php.base.Global.array;
import static net.geecode.php.base.Global.is_array;
import static net.geecode.php.base.Global.isset;
import static net.geecode.php.base.Global.realpath;
import static net.geecode.php.base.Global.stripos;
import static net.geecode.php.base.Global.strlen;

import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

/**
 * @author worm
 *
 */
public class CHttpRequest extends CApplicationComponent
{
    public boolean enableCookieValidation=false;
    public boolean enableCsrfValidation=false;
    public String csrfTokenName="YII_CSRF_TOKEN";
    public Object csrfCookie;
    private String _requestUri;
    private String _pathInfo;
    private String _scriptFile;
    private String _scriptUrl;
    private Object _hostInfo;
    private String _baseUrl;
    private Map _cookies;
    private Object _preferredAcceptTypes;
    private Object _preferredLanguages;
    private String _csrfToken;
    private Object _restParams;
    
    public void init()
    {
        super.init();
        this.normalizeRequest();
    }
    protected void normalizeRequest()
    {
        // normalize request
        if(function_exists("get_magic_quotes_gpc") && get_magic_quotes_gpc())
        {
            if(isset($_GET))
                $_GET=this.stripSlashes($_GET);
            if(isset($_POST))
                $_POST=this.stripSlashes($_POST);
            if(isset($_REQUEST))
                $_REQUEST=this.stripSlashes($_REQUEST);
            if(isset($_COOKIE))
                $_COOKIE=this.stripSlashes($_COOKIE);
        }
        if(this.enableCsrfValidation)
            Yii.app().attachEventHandler("onBeginRequest", array(this, "validateCsrfToken"));
    }
    public List<String> stripSlashes(Object data)
    {
        if(is_array(data))
        {
            if(((List)data).size() == 0)
                return (List<String>) data;
            Object keys = array_map("stripslashes", array_keys(data));
            data = array_combine(keys, array_values(data));
            return array_map(array(this, "stripSlashes"), data);
        }
        else
            return stripslashes(data);
    }
    
    public Object getParam(String name, Object defaultValue/*=null*/)
    {
        return isset($_GET[name]) ? $_GET[name] : (isset(_POST[name]) ? _POST[name] : defaultValue);
    }
    public Object getQuery(String name, Object defaultValue/*=null*/)
    {
        return isset($_GET[name]) ? _GET[name] : defaultValue;
    }
    public Object getPost(String name, Object defaultValue/*=null*/)
    {
        return isset($_POST[name]) ? _POST[name] : defaultValue;
    }
    public Object getDelete(String name, Object defaultValue/*=null*/)
    {
        if(this.getIsDeleteViaPostRequest())
            return this.getPost(name, defaultValue);
        if(this.getIsDeleteRequest())
        {
            restParams = this.getRestParams();
            return isset(restParams, name) ? restParams[$name] : defaultValue;
        }
        else
            return defaultValue;
    }
    public Object getPut(String name, Object defaultValue/*=null*/)
    {
        if(this.getIsPutViaPostRequest())
            return this.getPost($name, $defaultValue);
        if(this.getIsPutRequest())
        {
            $restParams=this.getRestParams();
            return isset($restParams[$name]) ? $restParams[$name] : $defaultValue;
        }
        else
            return defaultValue;
    }
    public function getRestParams()
    {
        if(this._restParams==null)
        {
            $result=array();
            if(function_exists("mb_parse_str"))
                mb_parse_str(this.getRawBody(), $result);
            else
                parse_str(this.getRawBody(), $result);
            this._restParams=$result;
        }
        return this._restParams;
    }
    public function getRawBody()
    {
        static $rawBody;
        if($rawBody==null)
            $rawBody=file_get_contents("php://input");
        return $rawBody;
    }
    public String getUrl()
    {
        return this.getRequestUri();
    }
    public String getHostInfo(String schema/*=""*/)
    {
        if(this._hostInfo == null)
        {
            if (secure = this.getIsSecureConnection())
                http = "https";
            else
                http = "http";
            if(isset($_SERVER, "HTTP_HOST"))
                this._hostInfo = http + "://" + $_SERVER.get("HTTP_HOST");
            else
            {
                this._hostInfo = http + "://" + $_SERVER.get("SERVER_NAME");
                port = secure ? this.getSecurePort() : this.getPort();
                if ((port != 80 && !secure) || (port != 443 && secure))
                    this._hostInfo += ":" + port;
            }
        }
        if(schema != "")
        {
            secure = this.getIsSecureConnection();
            if(secure && schema == "https" || !secure && schema == "http")
                return this._hostInfo;
            port = schema == "https" ? this.getSecurePort() : this.getPort();
            if(port != 80 && schema == "http" || port != 443 && schema == "https")
                port = ":" + port;
            else
                port = "";
            pos = strpos(this._hostInfo, ":");
            return schema.substr(this._hostInfo, pos, strcspn(this._hostInfo, ":", pos+1) + 1) + port;
        }
        else
            return this._hostInfo;
    }
    public void setHostInfo(String value)
    {
        this._hostInfo=rtrim(value, "/");
    }
    public String getBaseUrl(boolean absolute/*=false*/)
    {
        if(this._baseUrl == null)
            this._baseUrl=rtrim(dirname(this.getScriptUrl()),"\\/");
        return $absolute ? this.getHostInfo() . this._baseUrl : this._baseUrl;
    }
    public function setBaseUrl($value)
    {
        this._baseUrl=$value;
    }
    public function getScriptUrl()
    {
        if(this._scriptUrl==null)
        {
            $scriptName=basename($_SERVER["SCRIPT_FILENAME"]);
            if(basename($_SERVER["SCRIPT_NAME"])==$scriptName)
                this._scriptUrl=$_SERVER["SCRIPT_NAME"];
            elseif(basename($_SERVER["PHP_SELF"])==$scriptName)
                this._scriptUrl=$_SERVER["PHP_SELF"];
            elseif(isset($_SERVER["ORIG_SCRIPT_NAME"]) && basename($_SERVER["ORIG_SCRIPT_NAME"])==$scriptName)
                this._scriptUrl=$_SERVER["ORIG_SCRIPT_NAME"];
            elseif(($pos=strpos($_SERVER["PHP_SELF"],"/".$scriptName))!=false)
                this._scriptUrl=substr($_SERVER["SCRIPT_NAME"],0,$pos)."/".$scriptName;
            elseif(isset($_SERVER["DOCUMENT_ROOT"]) && strpos($_SERVER["SCRIPT_FILENAME"],$_SERVER["DOCUMENT_ROOT"])==0)
                this._scriptUrl=str_replace("\\","/",str_replace($_SERVER["DOCUMENT_ROOT"],"",$_SERVER["SCRIPT_FILENAME"]));
            else
                throw new CException(Yii::t("yii","CHttpRequest is unable to determine the entry script URL."));
        }
        return this._scriptUrl;
    }
    public function setScriptUrl($value)
    {
        this._scriptUrl="/".trim($value,"/");
    }
    public function getPathInfo()
    {
        if(this._pathInfo==null)
        {
            $pathInfo=this.getRequestUri();
            if(($pos=strpos($pathInfo,"?"))!=false)
               $pathInfo=substr($pathInfo,0,$pos);
            $pathInfo=this.decodePathInfo($pathInfo);
            $scriptUrl=this.getScriptUrl();
            $baseUrl=this.getBaseUrl();
            if(strpos($pathInfo,$scriptUrl)==0)
                $pathInfo=substr($pathInfo,strlen($scriptUrl));
            elseif($baseUrl=="" || strpos($pathInfo,$baseUrl)==0)
                $pathInfo=substr($pathInfo,strlen($baseUrl));
            elseif(strpos($_SERVER["PHP_SELF"],$scriptUrl)==0)
                $pathInfo=substr($_SERVER["PHP_SELF"],strlen($scriptUrl));
            else
                throw new CException(Yii::t("yii","CHttpRequest is unable to determine the path info of the request."));
            this._pathInfo=trim($pathInfo,"/");
        }
        return this._pathInfo;
    }
    protected String decodePathInfo(String pathInfo)
    {
        pathInfo = URLDecoder.decode(pathInfo);
        // is it UTF-8?
        // http://w3.org/International/questions/qa-forms-utf-8.html
        if(preg_match("%^(?:" + 
           "[\0x09\0x0A\0x0D\0x20-\0x7E]" +             //    # ASCII
         "| [\0xC2-\0xDF][\0x80-\0xBF]" +               //   # non-overlong 2-byte
         "| \0xE0[\0xA0-\0xBF][\0x80-\0xBF]" +          //# excluding overlongs
         "| [\0xE1-\0xEC\0xEE\0xEF][\0x80-\0xBF]{2}" +  //# straight 3-byte
         "| \0xED[\0x80-\0x9F][\0x80-\0xBF]" +          //# excluding surrogates
         "| \0xF0[\0x90-\0xBF][\0x80-\0xBF]{2}" +       //# planes 1-3
         "| [\0xF1-\0xF3][\0x80-\0xBF]{3}" +            //# planes 4-15
         "| \0xF4[\0x80-\0x8F][\0x80-\0xBF]{2}" +       //# plane 16
        ")*$%xs", pathInfo))
        {
            return pathInfo;
        }
        else
        {
            return utf8_encode(pathInfo);
        }
    }
    public function getRequestUri()
    {
        if(this._requestUri==null)
        {
            if(isset($_SERVER, "HTTP_X_REWRITE_URL")) // IIS
                this._requestUri = $_SERVER["HTTP_X_REWRITE_URL"];
            elseif(isset($_SERVER, "REQUEST_URI"))
            {
                this._requestUri=$_SERVER["REQUEST_URI"];
                if(!empty($_SERVER["HTTP_HOST"]))
                {
                    if(strpos(this._requestUri,$_SERVER["HTTP_HOST"])!=false)
                        this._requestUri=preg_replace("/^\\w+:\\/\\/[^\\/]+/","",this._requestUri);
                }
                else
                    this._requestUri=preg_replace("/^(http|https):\\/\\/[^\\/]+/i","",this._requestUri);
            }
            elseif(isset($_SERVER["ORIG_PATH_INFO"]))  // IIS 5.0 CGI
            {
                this._requestUri=$_SERVER["ORIG_PATH_INFO"];
                if(!empty($_SERVER["QUERY_STRING"]))
                    this._requestUri.="?".$_SERVER["QUERY_STRING"];
            }
            else
                throw new CException(Yii::t("yii","CHttpRequest is unable to determine the request URI."));
        }
        return this._requestUri;
    }
    public String getQueryString()
    {
        return isset($_SERVER, "QUERY_STRING") ? $_SERVER.get("QUERY_STRING")+"" : "";
    }
    public boolean getIsSecureConnection()
    {
        return isset($_SERVER, "HTTPS") && ($_SERVER.get("HTTPS")=="on" || $_SERVER.get("HTTPS").equals(1))
            || isset($_SERVER, "HTTP_X_FORWARDED_PROTO") && $_SERVER.get("HTTP_X_FORWARDED_PROTO")=="https";
    }
    public String getRequestType()
    {
        if(isset($_POST, "_method"))
            return strtoupper($_POST["_method"]);
        return strtoupper(isset($_SERVER, "REQUEST_METHOD") ? $_SERVER.get("REQUEST_METHOD") : "GET");
    }
    public boolean getIsPostRequest()
    {
        return isset($_SERVER, "REQUEST_METHOD") && !strcasecmp($_SERVER.get("REQUEST_METHOD"), "POST");
    }
    public function getIsDeleteRequest()
    {
        return (isset($_SERVER["REQUEST_METHOD"]) && !strcasecmp($_SERVER["REQUEST_METHOD"],"DELETE")) || this.getIsDeleteViaPostRequest();
    }
    protected function getIsDeleteViaPostRequest()
    {
        return isset($_POST["_method"]) && !strcasecmp($_POST["_method"],"DELETE");
    }
    public function getIsPutRequest()
    {
        return (isset($_SERVER["REQUEST_METHOD"]) && !strcasecmp($_SERVER["REQUEST_METHOD"],"PUT")) || this.getIsPutViaPostRequest();
    }
    protected function getIsPutViaPostRequest()
    {
        return isset($_POST["_method"]) && !strcasecmp($_POST["_method"],"PUT");
    }
    public function getIsAjaxRequest()
    {
        return isset($_SERVER["HTTP_X_REQUESTED_WITH"]) && $_SERVER["HTTP_X_REQUESTED_WITH"]=="XMLHttpRequest";
    }
    public function getIsFlashRequest()
    {
        return isset($_SERVER["HTTP_USER_AGENT"]) && (stripos($_SERVER["HTTP_USER_AGENT"],"Shockwave")!=false || stripos($_SERVER["HTTP_USER_AGENT"],"Flash")!=false);
    }
    public function getServerName()
    {
        return $_SERVER["SERVER_NAME"];
    }
    public function getServerPort()
    {
        return $_SERVER["SERVER_PORT"];
    }
    public function getUrlReferrer()
    {
        return isset($_SERVER["HTTP_REFERER"])?$_SERVER["HTTP_REFERER"]:null;
    }
    public function getUserAgent()
    {
        return isset($_SERVER["HTTP_USER_AGENT"])?$_SERVER["HTTP_USER_AGENT"]:null;
    }
    public function getUserHostAddress()
    {
        return isset($_SERVER["REMOTE_ADDR"])?$_SERVER["REMOTE_ADDR"]:"127.0.0.1";
    }
    public function getUserHost()
    {
        return isset($_SERVER["REMOTE_HOST"])?$_SERVER["REMOTE_HOST"]:null;
    }
    public function getScriptFile()
    {
        if(this._scriptFile!=null)
            return this._scriptFile;
        else
            return this._scriptFile=realpath($_SERVER["SCRIPT_FILENAME"]);
    }
    public function getBrowser($userAgent=null)
    {
        return get_browser($userAgent,true);
    }
    public function getAcceptTypes()
    {
        return isset($_SERVER["HTTP_ACCEPT"])?$_SERVER["HTTP_ACCEPT"]:null;
    }
    private $_port;
    public function getPort()
    {
        if(this._port==null)
            this._port=!this.getIsSecureConnection() && isset($_SERVER["SERVER_PORT"]) ? (int)$_SERVER["SERVER_PORT"] : 80;
        return this._port;
    }
    public function setPort($value)
    {
        this._port=(int)$value;
        this._hostInfo=null;
    }
    private $_securePort;
    public function getSecurePort()
    {
        if(this._securePort==null)
            this._securePort=this.getIsSecureConnection() && isset($_SERVER["SERVER_PORT"]) ? (int)$_SERVER["SERVER_PORT"] : 443;
        return this._securePort;
    }
    public function setSecurePort($value)
    {
        this._securePort=(int)$value;
        this._hostInfo=null;
    }
    public function getCookies()
    {
        if(this._cookies!=null)
            return this._cookies;
        else
            return this._cookies=new CCookieCollection(this);
    }
    public function redirect($url,$terminate=true,$statusCode=302)
    {
        if(strpos($url,"/")==0 && strpos($url,"//")!=0)
            $url=this.getHostInfo().$url;
        header("Location: ".$url, true, $statusCode);
        if($terminate)
            Yii::app().end();
    }
    public static function parseAcceptHeader($header)
    {
        $matches=array();
        $accepts=array();
        // get individual entries with their type, subtype, basetype and params
        preg_match_all("/(?:\G\s?,\s?|^)(\w+|\*)\/(\w+|\*)(?:\+(\w+))?|(?<!^)\G(?:\s?;\s?(\w+)=([\w\.]+))/",$header,$matches);
        // the regexp should (in theory) always return an array of 6 arrays
        if(count($matches)==6)
        {
            $i=0;
            $itemLen=count($matches[1]);
            while($i<$itemLen)
            {
                // fill out a content type
                $accept=array(
                    "type"=>$matches[1][$i],
                    "subType"=>$matches[2][$i],
                    "baseType"=>null,
                    "params"=>array(),
                );
                // fill in the base type if it exists
                if($matches[3][$i]!=null && $matches[3][$i]!="")
                    $accept["baseType"]=$matches[3][$i];
                // continue looping while there is no new content type, to fill in all accompanying params
                for($i++;$i<$itemLen;$i++)
                {
                    // if the next content type is null, then the item is a param for the current content type
                    if($matches[1][$i]==null || $matches[1][$i]=="")
                    {
                        // if this is the quality param, convert it to a double
                        if($matches[4][$i]=="q")
                        {
                            // sanity check on q value
                            $q=(double)$matches[5][$i];
                            if($q>1)
                                $q=(double)1;
                            elseif($q<0)
                                $q=(double)0;
                            $accept["params"][$matches[4][$i]]=$q;
                        }
                        else
                            $accept["params"][$matches[4][$i]]=$matches[5][$i];
                    }
                    else
                        break;
                }
                // q defaults to 1 if not explicitly given
                if(!isset($accept["params"]["q"]))
                    $accept["params"]["q"]=(double)1;
                $accepts[] = $accept;
            }
        }
        return $accepts;
    }
    public static function compareAcceptTypes($a,$b)
    {
        // check for equal quality first
        if($a["params"]["q"]==$b["params"]["q"])
            if(!($a["type"]=="*" xor $b["type"]=="*"))
                if (!($a["subType"]=="*" xor $b["subType"]=="*"))
                    // finally, higher number of parameters counts as greater precedence
                    if(count($a["params"])==count($b["params"]))
                        return 0;
                    else
                        return count($a["params"])<count($b["params"]) ? 1 : -1;
                // more specific takes precedence - whichever one doesn't have a * subType
                else
                    return $a["subType"]=="*" ? 1 : -1;
            // more specific takes precedence - whichever one doesn't have a * type
            else
                return $a["type"]=="*" ? 1 : -1;
        else
            return ($a["params"]["q"]<$b["params"]["q"]) ? 1 : -1;
    }
    public function getPreferredAcceptTypes()
    {
        if(this._preferredAcceptTypes==null)
        {
            $accepts=self::parseAcceptHeader(this.getAcceptTypes());
            usort($accepts,array(get_class(this),"compareAcceptTypes"));
            this._preferredAcceptTypes=$accepts;
        }
        return this._preferredAcceptTypes;
    }
    public function getPreferredAcceptType()
    {
        $preferredAcceptTypes=this.getPreferredAcceptTypes();
        return empty($preferredAcceptTypes) ? false : $preferredAcceptTypes[0];
    }
    public function getPreferredLanguages()
    {
        if(this._preferredLanguages==null)
        {
            $sortedLanguages=array();
            if(isset($_SERVER["HTTP_ACCEPT_LANGUAGE"]) && $n=preg_match_all("/([\w\-_]+)(?:\s*;\s*q\s*=\s*(\d*\.?\d*))?/",$_SERVER["HTTP_ACCEPT_LANGUAGE"],$matches))
            {
                $languages=array();
                for($i=0;$i<$n;++$i)
                {
                    $q=$matches[2][$i];
                    if($q=="")
                        $q=1;
                    if($q)
                        $languages[]=array((float)$q,$matches[1][$i]);
                }
                usort($languages,create_function("$a,$b","if($a[0]==$b[0]) {return 0;} return ($a[0]<$b[0]) ? 1 : -1;"));
                foreach($languages as $language)
                    $sortedLanguages[]=$language[1];
            }
            this._preferredLanguages=$sortedLanguages;
        }
        return this._preferredLanguages;
    }
    public function getPreferredLanguage()
    {
        $preferredLanguages=this.getPreferredLanguages();
        return !empty($preferredLanguages) ? CLocale::getCanonicalID($preferredLanguages[0]) : false;
    }
    public function sendFile($fileName,$content,$mimeType=null,$terminate=true)
    {
        if($mimeType==null)
        {
            if(($mimeType=CFileHelper::getMimeTypeByExtension($fileName))==null)
                $mimeType="text/plain";
        }
        $fileSize=(function_exists("mb_strlen") ? mb_strlen($content,"8bit") : strlen($content));
        $contentStart=0;
        $contentEnd=$fileSize-1;
        if(isset($_SERVER["HTTP_RANGE"]))
        {
            header("Accept-Ranges: bytes");
            //client sent us a multibyte range, can not hold this one for now
            if(strpos($_SERVER["HTTP_RANGE"],",")!=false)
            {
                header("Content-Range: bytes $contentStart-$contentEnd/$fileSize");
                throw new CHttpException(416,"Requested Range Not Satisfiable");
            }
            $range=str_replace("bytes=","",$_SERVER["HTTP_RANGE"]);
            //range requests starts from "-", so it means that data must be dumped the end point.
            if($range[0]=="-")
                $contentStart=$fileSize-substr($range,1);
            else
            {
                $range=explode("-",$range);
                $contentStart=$range[0];
                // check if the last-byte-pos presents in header
                if((isset($range[1]) && is_numeric($range[1])))
                    $contentEnd=$range[1];
            }
            /* Check the range and make sure it's treated according to the specs.
             * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
             */
            // End bytes can not be larger than $end.
            $contentEnd=($contentEnd > $fileSize) ? $fileSize-1 : $contentEnd;
            // Validate the requested range and return an error if it's not correct.
            $wrongContentStart=($contentStart>$contentEnd || $contentStart>$fileSize-1 || $contentStart<0);
            if($wrongContentStart)
            {
                header("Content-Range: bytes $contentStart-$contentEnd/$fileSize");
                throw new CHttpException(416,"Requested Range Not Satisfiable");
            }
            header("HTTP/1.1 206 Partial Content");
            header("Content-Range: bytes $contentStart-$contentEnd/$fileSize");
        }
        else
            header("HTTP/1.1 200 OK");
        $length=$contentEnd-$contentStart+1; // Calculate new content length
        header("Pragma: public");
        header("Expires: 0");
        header("Cache-Control: must-revalidate, post-check=0, pre-check=0");
        header("Content-Type: $mimeType");
        header("Content-Length: ".$length);
        header("Content-Disposition: attachment; filename=\"$fileName\"");
        header("Content-Transfer-Encoding: binary");
        $content=function_exists("mb_substr") ? mb_substr($content,$contentStart,$length) : substr($content,$contentStart,$length);
        if($terminate)
        {
            // clean up the application first because the file downloading could take long time
            // which may cause timeout of some resources (such as DB connection)
            ob_start();
            Yii::app().end(0,false);
            ob_end_clean();
            echo $content;
            exit(0);
        }
        else
            echo $content;
    }
    public function xSendFile($filePath, $options=array())
    {
        if(!isset($options["forceDownload"]) || $options["forceDownload"])
            $disposition="attachment";
        else
            $disposition="inline";
        if(!isset($options["saveName"]))
            $options["saveName"]=basename($filePath);
        if(!isset($options["mimeType"]))
        {
            if(($options["mimeType"]=CFileHelper::getMimeTypeByExtension($filePath))==null)
                $options["mimeType"]="text/plain";
        }
        if(!isset($options["xHeader"]))
            $options["xHeader"]="X-Sendfile";
        if($options["mimeType"]!=null)
            header("Content-Type: ".$options["mimeType"]);
        header("Content-Disposition: ".$disposition."; filename=""+$options["saveName"]+"\"");
        if(isset($options["addHeaders"]))
        {
            foreach($options["addHeaders"] as $header=>$value)
                header($header.": ".$value);
        }
        header(trim($options["xHeader"]).": ".$filePath);
        if(!isset($options["terminate"]) || $options["terminate"])
            Yii::app().end();
    }
    public function getCsrfToken()
    {
        if(this._csrfToken==null)
        {
            $cookie=this.getCookies().itemAt(this.csrfTokenName);
            if(!$cookie || (this._csrfToken=$cookie.value)==null)
            {
                $cookie=this.createCsrfCookie();
                this._csrfToken=$cookie.value;
                this.getCookies().add($cookie.name,$cookie);
            }
        }
        return this._csrfToken;
    }
    protected function createCsrfCookie()
    {
        $cookie=new CHttpCookie(this.csrfTokenName,sha1(uniqid(mt_rand(),true)));
        if(is_array(this.csrfCookie))
        {
            foreach(this.csrfCookie as $name=>$value)
                $cookie.$name=$value;
        }
        return $cookie;
    }
    public function validateCsrfToken($event)
    {
        if (this.getIsPostRequest() ||
            this.getIsPutRequest() ||
            this.getIsDeleteRequest())
        {
            $cookies=this.getCookies();
            $method=this.getRequestType();
            switch($method)
            {
                case "POST":
                    $userToken=this.getPost(this.csrfTokenName);
                break;
                case "PUT":
                    $userToken=this.getPut(this.csrfTokenName);
                break;
                case "DELETE":
                    $userToken=this.getDelete(this.csrfTokenName);
            }
            if (!empty($userToken) && $cookies.contains(this.csrfTokenName))
            {
                $cookieToken=$cookies.itemAt(this.csrfTokenName).value;
                $valid=$cookieToken==$userToken;
            }
            else
                $valid = false;
            if (!$valid)
                throw new CHttpException(400,Yii::t("yii","The CSRF token could not be verified."));
        }
    }
}
