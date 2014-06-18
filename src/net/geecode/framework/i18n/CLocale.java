/**
 * 
 * CLocale class file.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @link http://www.yiiframework.com/
 * @copyright 2008-2013 Yii Software LLC
 * @license http://www.yiiframework.com/license/
 */
package net.geecode.framework.i18n;

import static net.geecode.php.base.Global.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.geecode.framework.base.CComponent;
import net.geecode.framework.base.CException;
import net.geecode.framework.lite.Yii;

/**
 * CLocale represents the data relevant to a locale.
 *
 * The data includes the number formatting information and date formatting information.
 *
 * @property string $id The locale ID (in canonical form).
 * @property CNumberFormatter $numberFormatter The number formatter for this locale.
 * @property CDateFormatter $dateFormatter The date formatter for this locale.
 * @property string $decimalFormat The decimal format.
 * @property string $currencyFormat The currency format.
 * @property string $percentFormat The percent format.
 * @property string $scientificFormat The scientific format.
 * @property array $monthNames Month names indexed by month values (1-12).
 * @property array $weekDayNames The weekday names indexed by weekday values (0-6, 0 means Sunday, 1 Monday, etc.).
 * @property string $aMName The AM name.
 * @property string $pMName The PM name.
 * @property string $dateFormat Date format.
 * @property string $timeFormat Date format.
 * @property string $dateTimeFormat Datetime format, i.e., the order of date and time.
 * @property string $orientation The character orientation, which is either 'ltr' (left-to-right) or 'rtl' (right-to-left).
 * @property array $pluralRules Plural forms expressions.
 *
 * @author Qiang Xue <qiang.xue@gmail.com>
 * @package system.i18n
 * @since 1.0
 */
public class CLocale extends CComponent
{
    /**
     * @var string the directory that contains the locale data. If this property is not set,
     * the locale data will be loaded from 'framework/i18n/data'.
     * @since 1.1.0
     */
    public static String dataPath;

    private String _id;
    private Map<String, Object> _data;
    private CDateFormatter _dateFormatter;
    private CNumberFormatter _numberFormatter;
    private static Map<String, CLocale> locales;
    
    /**
     * Returns the instance of the specified locale.
     * Since the constructor of CLocale is protected, you can only use
     * this method to obtain an instance of the specified locale.
     * @param string $id the locale ID (e.g. en_US)
     * @return CLocale the locale instance
     */
    public static CLocale getInstance(String id)
    {
        locales= new HashMap<String, CLocale>();
        if(isset(locales, id))
            return locales.get(id);
        else
        {
            CLocale l = new CLocale(id);
            locales.put(id, l);
            return l;
        }
    }

    /**
     * @return array IDs of the locales which the framework can recognize
     */
    public static Object getLocaleIDs()
    {
        if(locales==null)
        {
            locales=new HashMap<String, CLocale>();
            dataPath = CLocale.dataPath==null
                    ? dirname(__FILE__) + DIRECTORY_SEPARATOR + "data"
                            : CLocale.dataPath;
            folder = opendir(dataPath);
            while((file=readdir(folder))!=false)
            {
                fullPath=dataPath + DIRECTORY_SEPARATOR + file;
                if(substr($file,-4)==".php" && is_file(fullPath))
                    $locales[]=substr($file,0,-4);
            }
            closedir($folder);
            sort($locales);
        }
        return $locales;
    }

    /**
     * Constructor.
     * Since the constructor is protected, please use {@link getInstance}
     * to obtain an instance of the specified locale.
     * @param string $id the locale ID (e.g. en_US)
     * @throws CException if given locale id is not recognized
     */
    protected void CLocale(String id)
    {
        this._id = CLocale.getCanonicalID(id);
        dataPath = CLocale.dataPath==null
                ? dirname(__FILE__) + DIRECTORY_SEPARATOR + "data"
                        : CLocale.dataPath;
        String dataFile = dataPath + DIRECTORY_SEPARATOR + this._id + ".php";
//        if(is_file(dataFile))
//            this._data=require(dataFile);
//        else
//            throw new CException(Yii.t("yii", "Unrecognized locale \"{locale}\".", array("{locale}", id)));
    }

    /**
     * Converts a locale ID to its canonical form.
     * In canonical form, a locale ID consists of only underscores and lower-case letters.
     * @param string $id the locale ID to be converted
     * @return string the locale ID in canonical form
     */
    public static String getCanonicalID(String id)
    {
        return strtolower(str_replace("-", "_", id));
    }

    /**
     * @return string the locale ID (in canonical form)
     */
    public String getId()
    {
        return this._id;
    }

    /**
     * @return CNumberFormatter the number formatter for this locale
     */
    public CNumberFormatter getNumberFormatter()
    {
        if(this._numberFormatter==null)
            this._numberFormatter=new CNumberFormatter(this);
        return this._numberFormatter;
    }

    /**
     * @return CDateFormatter the date formatter for this locale
     */
    public CDateFormatter getDateFormatter()
    {
        if(this._dateFormatter==null)
            this._dateFormatter=new CDateFormatter(this);
        return this._dateFormatter;
    }

    /**
     * @param string $currency 3-letter ISO 4217 code. For example, the code "USD" represents the US Dollar and "EUR" represents the Euro currency.
     * @return string the localized currency symbol. Null if the symbol does not exist.
     */
    public String getCurrencySymbol(String currency)
    {
        return (String) (isset(this._data, "currencySymbols") && this._data.get("currencySymbols") instanceof Map
                && isset((Map)this._data.get("currencySymbols"), currency)
                ? ((Map)this._data.get("currencySymbols")).get(currency) : null);
    }

    /**
     * @param string $name symbol name
     * @return string symbol
     */
    public String getNumberSymbol(String name)
    {
        return (String) (isset(this._data, "numberSymbols") && this._data.get("numberSymbols") instanceof Map
                && isset((Map)this._data.get("numberSymbols"), name)
                ? ((Map)this._data.get("numberSymbols")).get(name) : null);
    }

    /**
     * @return string the decimal format
     */
    public String getDecimalFormat()
    {
        return (String) this._data.get("decimalFormat");
    }

    /**
     * @return string the currency format
     */
    public String getCurrencyFormat()
    {
        return (String) this._data.get("currencyFormat");
    }

    /**
     * @return string the percent format
     */
    public String getPercentFormat()
    {
        return (String) this._data.get("percentFormat");
    }

             /**
              * @return string the scientific format
              */
             public String getScientificFormat()
             {
                 return (String) this._data.get("scientificFormat");
             }

             /**
              * @param integer $month month (1-12)
              * @param string $width month name width. It can be 'wide', 'abbreviated' or 'narrow'.
              * @param boolean $standAlone whether the month name should be returned in stand-alone format
              * @return string the month name
              */
             public String getMonthName(String month, String width/*="wide"*/, boolean standAlone/*=false*/)
             {
                 Map<String, Object> map = getMonthNames(width, standAlone);
                 return (String) (isset(map, month) ? map.get(month) : null);
             }

             /**
              * Returns the month names in the specified width.
              * @param string $width month name width. It can be 'wide', 'abbreviated' or 'narrow'.
              * @param boolean $standAlone whether the month names should be returned in stand-alone format
              * @return array month names indexed by month values (1-12)
              */
             public Map<String, Object> getMonthNames(String width/*="wide"*/, boolean standAlone/*=false*/)
             {
                 if(standAlone)
                 {
                     return (Map<String, Object>) (isset(this._data, "monthNamesSA") && this._data.get("monthNamesSA") instanceof Map
                             && isset((Map)this._data.get("monthNamesSA"), width)
                             ? ((Map)this._data.get("monthNamesSA")).get(width) : ((Map)this._data.get("monthNames")).get(width));
                 }
                 else
                 {
                     return (Map<String, Object>) (isset(this._data, "monthNames") && this._data.get("monthNames") instanceof Map
                             && isset((Map)this._data.get("monthNames"), width)
                             ? ((Map)this._data.get("monthNames")).get(width) : ((Map)this._data.get("monthNamesSA")).get(width));
                 }
             }

             /**
              * @param integer $day weekday (0-7, 0 and 7 means Sunday)
              * @param string $width weekday name width.  It can be 'wide', 'abbreviated' or 'narrow'.
              * @param boolean $standAlone whether the week day name should be returned in stand-alone format
              * @return string the weekday name
              */
             public String getWeekDayName(int day, String width/*="wide"*/, boolean standAlone/*=false*/)
             {
                 day = day % 7;
                 Map<String, Object> map = getWeekDayNames(width, standAlone);
                 return (String) (isset(map, day + "") ? map.get(day + "") : null);
             }

             /**
              * Returns the week day names in the specified width.
              * @param string $width weekday name width.  It can be 'wide', 'abbreviated' or 'narrow'.
              * @param boolean $standAlone whether the week day name should be returned in stand-alone format
              * @return array the weekday names indexed by weekday values (0-6, 0 means Sunday, 1 Monday, etc.)
              */
             public Map<String, Object> getWeekDayNames(String width/*="wide"*/, boolean standAlone/*=false*/)
             {
                 if(standAlone)
                 {
                     return (Map<String, Object>) (isset(this._data, "weekDayNamesSA") && this._data.get("weekDayNamesSA") instanceof Map
                             && isset((Map)this._data.get("weekDayNamesSA"), width)
                             ? ((Map)this._data.get("weekDayNamesSA")).get(width) : ((Map)this._data.get("weekDayNames")).get(width));
                 }
                 else
                 {
                     return (Map<String, Object>) (isset(this._data, "weekDayNames") && this._data.get("weekDayNames") instanceof Map
                             && isset((Map)this._data.get("weekDayNames"), width)
                             ? ((Map)this._data.get("weekDayNames")).get(width) : ((Map)this._data.get("weekDayNamesSA")).get(width));
                 }
                 
             }

             /**
              * @param integer $era era (0,1)
              * @param string $width era name width.  It can be 'wide', 'abbreviated' or 'narrow'.
              * @return string the era name
              */
             public String getEraName(int era, String width/*="wide"*/)
             {
                 return (String) ((Map)((Map)this._data.get("eraNames")).get(width)).get(era);
             }

             /**
              * @return string the AM name
              */
             public String getAMName()
             {
                 return (String) this._data.get("amName");
             }

             /**
              * @return string the PM name
              */
             public String getPMName()
             {
                 return (String) this._data.get("pmName");
             }

             /**
              * @param string $width date format width. It can be 'full', 'long', 'medium' or 'short'.
              * @return string date format
              */
             public String getDateFormat(String width/*="medium"*/)
             {
                 return (String) ((Map<String, Object>)this._data.get("dateFormats")).get(width);
             }

             /**
              * @param string $width time format width. It can be 'full', 'long', 'medium' or 'short'.
              * @return string date format
              */
             public String getTimeFormat(String width/*="medium"*/)
             {
                 return (String) ((Map<String, Object>)this._data.get("timeFormats")).get(width);
             }

             /**
              * @return string datetime format, i.e., the order of date and time.
              */
             public String getDateTimeFormat()
             {
                 return (String) this._data.get("dateTimeFormat");
             }

             /**
              * @return string the character orientation, which is either 'ltr' (left-to-right) or 'rtl' (right-to-left)
              * @since 1.1.2
              */
             public String getOrientation()
             {
                 return (String) (isset(this._data, "orientation")
                         ? this._data.get("orientation")
                                 : "ltr");
             }

             /**
              * @return array plural forms expressions
              */
             public Map<String, Object> getPluralRules()
             {
                 return (Map<String, Object>) (isset(this._data, "pluralRules")
                         ? this._data.get("pluralRules")
                         : array(0+"", "true"));
             }

             /**
              * Converts a locale ID to a language ID.
              * A language ID consists of only the first group of letters before an underscore or dash.
              * @param string $id the locale ID to be converted
              * @return string the language ID
              * @since 1.1.9
              */
             public String getLanguageID(String id)
             {
                 // normalize id
                 id = CLocale.getCanonicalID(id);
                 // remove sub tags
                 int underscorePosition = id.indexOf("_");
                 if(underscorePosition != -1)
                 {
                     id = id.substring(0, underscorePosition);
                 }
                 return id;
             }

             /**
              * Converts a locale ID to a script ID.
              * A script ID consists of only the last four characters after an underscore or dash.
              * @param string $id the locale ID to be converted
              * @return string the script ID
              * @since 1.1.9
              */
             public String getScriptID(String id)
             {
                 // normalize id
                 id = CLocale.getCanonicalID(id);
                 // find sub tags
                 int underscorePosition = id.indexOf("_");
                 if (underscorePosition != -1)
                 {
                     String[] subTag = id.split("_");
                     // script sub tags can be distigused from territory sub tags by length
                     if (subTag[1].length() == 4)
                     {
                         id = subTag[1];
                     }
                     else
                     {
                         id = null;
                     }
                 }
                 else
                 {
                     id = null;
                 }
                 return id;
             }

             /**
              * Converts a locale ID to a territory ID.
              * A territory ID consists of only the last two to three letter or digits after an underscore or dash.
              * @param string $id the locale ID to be converted
              * @return string the territory ID
              * @since 1.1.9
              */
             public String getTerritoryID(String id)
             {
                 // normalize id
                 id = CLocale.getCanonicalID(id);
                 // find sub tags
                 int underscorePosition;
                 if ((underscorePosition = strpos(id, "_")) != -1)
                 {
                     String[] subTag = explode("_", id);
                     // territory sub tags can be distigused from script sub tags by length
                     if (subTag.length > 2 && subTag[2].length() < 4)
                     {
                         id = subTag[2];
                     }
                     else if (strlen(subTag[1]) < 4)
                     {
                         id = subTag[1];
                     }
                     else
                     {
                         id = null;
                     }
                 }
                 else
                 {
                     id = null;
                 }
                 return id;
             }

             /**
              * Gets a localized name from i18n data file (one of framework/i18n/data/ files).
              *
              * @param string $id array key from an array named by $category.
              * @param string $category data category. One of 'languages', 'scripts' or 'territories'.
              * @return string the localized name for the id specified. Null if data does not exist.
              * @since 1.1.9
              */
             public String getLocaleDisplayName(String id/*=null*/, String category/*="languages"*/)
             {
                 id = CLocale.getCanonicalID(id);
                 String val = id;
//                 if (category.equals("languages"))
//                 {
//                     return $this._data[$category][$id];
//                 }
                 if (category.equals("scripts"))
                 {
                     val=this.getScriptID(id);
                 } else if (category.equals("territories"))
                 {
                     val = this.getTerritoryID(id);
                 }
                 
                 if (isset(this._data, category)
                         && this._data.get(category) instanceof Map)
                 {
                     if (isset((Map)this._data.get(category), val))
                         return (String) ((Map)this._data.get(category)).get(val);
                     if (!val.equals(id) && isset((Map)this._data.get(category), id))
                         return (String) ((Map)this._data.get(category)).get(id);
                 }
                 else
                 {
                     return null;
                 }
             }

             /**
              * @param string $id Unicode language identifier from IETF BCP 47. For example, the code "en_US" represents U.S. English and "en_GB" represents British English.
              * @return string the local display name for the language. Null if the language code does not exist.
              * @since 1.1.9
              */
             public String getLanguage(String id)
             {
                 id = this.getLanguageID(id);
                 return this.getLocaleDisplayName(id, "languages");
             }

             /**
              * @param string $id Unicode script identifier from IETF BCP 47. For example, the code "en_US" represents U.S. English and "en_GB" represents British English.
              * @return string the local display name for the script. Null if the script code does not exist.
              * @since 1.1.9
              */
             public String getScript(String id)
             {
                 return this.getLocaleDisplayName(id, "scripts");
             }

             /**
              * @param string $id Unicode territory identifier from IETF BCP 47. For example, the code "en_US" represents U.S. English and "en_GB" represents British English.
              * @return string the local display name for the territory. Null if the territory code does not exist.
              * @since 1.1.9
              */
             public String getTerritory(String id)
             {
                 return this.getLocaleDisplayName(id, "territories");
             }
         }

