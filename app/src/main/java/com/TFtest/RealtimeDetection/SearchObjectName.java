package com.TFtest.RealtimeDetection;

import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchObjectName {
    private String objectName;
    private static final String pattern = "search .*|find .*";
    private static final String pattern1 = "find a\\s*|search a\\s*|find the\\s*|search the\\s*|search\\s*|find\\s*";

    public SearchObjectName() {
        objectName = null;
    }

    public String getObjectName() {
        //Log.e("ObjName", objectName);
        return objectName;
    }

    public void setObjectName(String objectName) {
        if (objectName == null) {
            this.objectName = null;
            return;
        }
        Pattern regExp = Pattern.compile(pattern);
        Matcher matcher = regExp.matcher(objectName);
        if (matcher.find()) {
            objectName = matcher.group(0);
            regExp = Pattern.compile(pattern1);
            matcher = regExp.matcher(objectName);
            this.objectName = matcher.replaceAll("");
            //Log.e("ObjName", objectName);
        } else {
            this.objectName = null;
        }
    }
}
