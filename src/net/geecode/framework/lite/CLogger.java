/**
 * 
 */
package net.geecode.framework.lite;

import static net.geecode.php.base.Global.array;
import static net.geecode.php.base.Global.is_string;
import static net.geecode.php.base.Global.isset;
import static net.geecode.php.base.Global.strncasecmp;
import static net.geecode.php.base.Global.strtolower;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.geecode.framework.base.CComponent;

/**
 * @author David
 *
 */
public class CLogger extends CComponent
{
    public static final String LEVEL_TRACE="trace";
    public static final String LEVEL_WARNING="warning";
    public static final String LEVEL_ERROR="error";
    public static final String LEVEL_INFO="info";
    public static final String LEVEL_PROFILE="profile";
    public int autoFlush=10000;
    public boolean autoDump=false;
    private Map<String, Object> _logs=array();
    private int _logCount=0;
    private String _levels;
    private String _categories;
    private Map<String, Object> _except=array();
    private Stack _timings;
    private boolean _processing=false;
    
    public void log(String $message, String $level/*="info"*/, String $category/*="application"*/)
    {
        this._logs[]=array($message,$level,$category,microtime(true));
        this._logCount++;
        if(this.autoFlush>0 && this._logCount>=this.autoFlush && !this._processing)
        {
            this._processing=true;
            this.flush(this.autoDump);
            this._processing=false;
        }
    }
    
    public Map getLogs(String $levels/*=""*/,Map $categories/*=array()*/, Map $except/*=array()*/)
    {
        this._levels=preg_split("/[\\s,]+/",strtolower($levels),-1,PREG_SPLIT_NO_EMPTY);
        if (is_string($categories))
            this._categories=preg_split("/[\\s,]+/",strtolower($categories),-1,PREG_SPLIT_NO_EMPTY);
        else
            this._categories=array_filter(array_map("strtolower",$categories));
        if (is_string($except))
            this._except=preg_split("/[\\s,]+/",strtolower($except),-1,PREG_SPLIT_NO_EMPTY);
        else
            this._except=array_filter(array_map("strtolower",$except));
        Map<String, Object> ret = this._logs;
        if(!($levels.isEmpty()))
            ret=array_values(array_filter(ret, Arrays.asList(this,"filterByLevel")));
        if(!(this._categories.isEmpty()) || !(this._except.isEmpty()))
            ret=array_values(array_filter(ret, Arrays.asList(this,"filterByCategory")));
        return ret;
    }
    
    private function filterByCategory($value)
    {
        return this.filterAllCategories($value, 2);
    }
    
    private function filterTimingByCategory($value)
    {
        return this.filterAllCategories($value, 1);
    }
    
    private function filterAllCategories($value, $index)
    {
        $cat=strtolower($value[$index]);
        $ret=empty($this._categories);
        foreach($this._categories as $category)
        {
            if($cat===$category || (($c=rtrim($category,".*"))!==$category && strpos($cat,$c)===0))
                $ret=true;
        }
        if($ret)
        {
            foreach($this._except as $category)
            {
                if($cat===$category || (($c=rtrim($category,".*"))!==$category && strpos($cat,$c)===0))
                    $ret=false;
            }
        }
        return $ret;
    }
    
    private function filterByLevel($value)
    {
        return in_array(strtolower($value[1]),$this._levels);
    }
    
    public long getExecutionTime()
    {
        return microtime(true)-YII_BEGIN_TIME;
    }
    
    public long getMemoryUsage()
    {
        if(function_exists("memory_get_usage"))
            return memory_get_usage();
        else
        {
            Map<String, Object> $output = array();
            if(strncmp(PHP_OS,"WIN",3)==0)
            {
                exec("tasklist /FI \"PID eq " + getmypid() + "\" /FO LIST",$output);
                return isset($output[5])?preg_replace("/[\\D]/","",$output[5])*1024 : 0;
            }
            else
            {
                $pid=getmypid();
                exec("ps -eo%mem,rss,pid | grep $pid", $output);
                $output=explode("  ",$output[0]);
                return isset($output[1]) ? $output[1]*1024 : 0;
            }
        }
    }
    public Stack<Object> getProfilingResults(String $token/*=null*/, String $categories/*=null*/,
            boolean $refresh/*=false*/)
    {
        if(this._timings==null || $refresh)
            this.calculateTimings();
        if($token==null && $categories==null)
            return this._timings;
        Stack<Object[]> $timings = this._timings;
        if($categories!=null) {
            this._categories=preg_split("/[\\s,]+/",strtolower($categories),-1,PREG_SPLIT_NO_EMPTY);
            $timings=array_filter($timings,array(this,"filterTimingByCategory"));
        }
        Stack<Object> $results = new Stack<Object>();
        for(Object[] $timing : $timings)
        {
            if($token==null || $timing[0]==$token)
                $results.push($timing[2]);
        }
        return $results;
    }
    private void calculateTimings()
    {
        this._timings=new Stack();
        Stack $stack = new Stack();
        for(Object log : this._logs.values())
        {
            List $log = (List) log;
            if($log.get(0) != CLogger.LEVEL_PROFILE)
                continue;
            String $message = (String) $log.get(0);
            String $level = (String) $log.get(1);
            String $category = (String) $log.get(2);
            String $timestamp = (String) $log.get(3);
//            list($message,$level,$category,$timestamp)=$log;
            int $delta;
            if(!strncasecmp($message,"begin:",6))
            {
                $log.set(0, $message.substring(6));
                $stack.push($log);
            }
            else if(!strncasecmp($message,"end:",4))
            {
                String $token = $message.substring(4);
                List $last = (List) $stack.pop();
                if(($last)!=null && $last.get(0)==$token)
                {
                    $delta=(Integer)$log.get(3)-(Integer)$last.get(3);
                    this._timings.push(Arrays.asList($message,$category,$delta));
                }
                else
                    throw new CException(Yii.t("yii","CProfileLogRoute found a mismatching code block \"{token}\". Make sure the calls to Yii::beginProfile() and Yii::endProfile() be properly nested.",
                        array("{token}",$token), null, null));
            }
        }
        long $now=microtime(true);
        Object[] $last=(Object[]) $stack.pop();
        while($last!=null)
        {
            long $delta = $now-(Integer)$last[3];
            this._timings.push(Arrays.asList($last[0],$last[2],$delta));
        }
    }
    
    public void flush(boolean $dumpLogs/*=false*/)
    {
        this.onFlush(new CEvent(this, array("dumpLogs", $dumpLogs)));
        this._logs=array();
        this._logCount=0;
    }
    
    public void onFlush(CEvent $event)
    {
        this.raiseEvent("onFlush", $event);
    }
}
