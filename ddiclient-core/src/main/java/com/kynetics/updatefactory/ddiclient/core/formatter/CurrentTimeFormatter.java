/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core.formatter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class CurrentTimeFormatter {

    private final TimeZone timeZone;
    private final SimpleDateFormat simpleDateFormat;

    public CurrentTimeFormatter(){
        this("UTC", "yyyyMMdd'T'HHmmss");
    }

    public CurrentTimeFormatter(String timeZoneString, String formatString){
        timeZone = TimeZone.getTimeZone(timeZoneString);
        simpleDateFormat = new SimpleDateFormat(formatString);
        simpleDateFormat.setTimeZone(timeZone);
    }

    public String formatCurrentTime(){
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(new Date());
        return simpleDateFormat.format(calendar.getTime());
    }
}
