package com.ralo.luka.polaris;

import java.util.Calendar;

/**
 * Created by Luka on 09.01.2018.
 */

public class PolarisHourAngle {
    public static int getJulianDate(Double location){

        Calendar calendarDate = MainActivity.cal3;
        int year = calendarDate.get(Calendar.YEAR);
        int month = calendarDate.get(Calendar.MONTH) + 1;
        int day = calendarDate.get(Calendar.DAY_OF_MONTH);
        double hour = calendarDate.get(Calendar.HOUR_OF_DAY);
        double minute = calendarDate.get(Calendar.MINUTE);
        double second = calendarDate.get(Calendar.SECOND);
        int isGregorianCal = 1;
        int A;
        int B;
        int C;
        int D;
        double fraction = day + ((hour + (minute / 60) + (second / 60 / 60)) / 24);

        if (year < 1582)
        {
            isGregorianCal = 0;
        }

        if (month < 3)
        {
            year = year - 1;
            month = month + 12;
        }

        A = year / 100;
        B = (2 - A + (A / 4)) * isGregorianCal;

        if (year < 0)
        {
            C = (int)((365.25 * year) - 0.75);
        }
        else
        {
            C = (int)(365.25 * year);
        }

        D = (int)(30.6001 * (month + 1));
        double JD = B + C + D + 1720994.5 + fraction;
        double myLocation = location/15;


        double localSiderealTime = (18.697374558 +myLocation - 2.9075 + 24.06570982441908*((JD-0.04165) -2451545.0))%24;
        int localSiderealTimeInt = (int)(18.697374558 +myLocation- 2.9075+ 24.06570982441908*((JD-0.04165) -2451545.0))%24;
        double minutes =((localSiderealTime- localSiderealTimeInt) *60)%60;
        int polarisLocation = localSiderealTimeInt*60 + (int)minutes;
        return polarisLocation;
    }
}
