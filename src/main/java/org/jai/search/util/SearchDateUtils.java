package org.jai.search.util;

import java.util.Date;

import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;

public final class SearchDateUtils
{
    public static final String SEARCH_DATE_FORMAT_YYYY_MM_DD_T_HH_MM_SSSZZ = "yyyy-MM-dd'T'HH:mm:ssZZ";
    
    private static final DateTimeFormatter searchDateFormat = ISODateTimeFormat.dateTimeNoMillis();//new SimpleDateFormat(SEARCH_DATE_FORMAT_YYYY_MM_DD_T_HH_MM_SS_SSSZZ);
    
    private SearchDateUtils()
    {
    }
    
    public static Date getFormattedDate(String dateString)
    {
        return searchDateFormat.parseDateTime(dateString).toDate();
    }
    
    public static String formatDate(Date date)
    {
        return searchDateFormat.print(new DateTime(date).getMillis());
    }
}
