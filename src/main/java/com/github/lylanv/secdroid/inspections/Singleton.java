package com.github.lylanv.secdroid.inspections;

import java.util.Map;
import java.util.HashMap;

public class Singleton {
    //    public static Project project;
    public static Map<String, Integer> redAPICalls = new HashMap<>(); //holds red API call's name and its energy cost
    public static Map<String, Integer> jointRedAPICalls = new HashMap<>();
    public static Map<String, Integer> hwAPICalls = new HashMap<>();
    public static int NUMBER_OF_RED_APIS;

//    public Singleton(final Project project) {
//        setAPICallsMap();
//        Singleton.project = project;
//    }

    public Singleton() {
        setAPICallsMap();
        setJointAPICallsMap();
        setJointRedAPIsParents();
    }

    // This function adds the API calls to ReadAPICalls set
    private void setAPICallsMap() {
        //TODO: you have some the same method expression such as query, updated, addView, findViewById, setText and show
        // consider to do something with them. Their energy consumption could be different
        redAPICalls.clear();

        //Activity and Context
        redAPICalls.put("performClick",1); // android.view.View.performClick() & android.widget.CompoundButton.performClick()
        redAPICalls.put("performItemClick",1);
        redAPICalls.put("CrashInfo",1);
        redAPICalls.put("dispatchWindowFocusChanged",1);
        redAPICalls.put("onChange",1);
        redAPICalls.put("sendAccessibilityEvent",1);
        redAPICalls.put("getInt",1);
        redAPICalls.put("getIntExtra",1);
        redAPICalls.put("i",1);
        redAPICalls.put("finish",1);
        redAPICalls.put("cancelAll",1);
        redAPICalls.put("startActivityForResult",1);
        redAPICalls.put("findViewById",1);
        redAPICalls.put("getPhoneType",1);
        redAPICalls.put("writeBundle",1);
        redAPICalls.put("clear",1);
        redAPICalls.put("createTypedArrayList",1);
        redAPICalls.put("getPixel",1);

        //Database
        redAPICalls.put("getReadableDatabase",1);
        redAPICalls.put("getWritableDatabase",1);
        redAPICalls.put("openOrCreateDatabase",1);
        redAPICalls.put("openDatabase",1);
        redAPICalls.put("update",1); //android.database.sqlite.SQLiteDatabase.update(java.lang.String#android.content.ContentValues#java.lang.String#java.lang.String[])
        redAPICalls.put("query",1); //android.database.sqlite.SQLiteQueryBuilder.query(android.database.sqlite.SQLiteDatabase#java.lang.String[]#java.lang.String#java.lang.String[]#java.lang.String#java.lang.String#java.lang.String#java.lang.String#android.os.CancellationSignal)
        //and android.database.sqlite.SQLiteDatabase.query(java.lang.String#java.lang.String[]#java.lang.String#java.lang.String[]#java.lang.String#java.lang.String#java.lang.String)---
        //and android.database.sqlite.SQLiteDatabase.query(boolean#java.lang.String#java.lang.String[]#java.lang.String#java.lang.String[]#java.lang.String#java.lang.String#java.lang.String#java.lang.String)---
        redAPICalls.put("insertOrThrow",1);
        redAPICalls.put("execSQL",1);
        redAPICalls.put("delete",1);
        redAPICalls.put("insert",1);
        redAPICalls.put("insertWithOnConflict",1);
        //redAPICalls.put("openOrCreateDatabase",1); //Repetitive
        redAPICalls.put("updateWithOnConflict",1);
        redAPICalls.put("deleteDatabase",1);
        redAPICalls.put("longForQuery",1);
        redAPICalls.put("getCount",1);
        redAPICalls.put("executeUpdateDelete",1);
        redAPICalls.put("setVersion",1);
        redAPICalls.put("rawQuery",1);
        redAPICalls.put("getVersion",1);
        redAPICalls.put("buildQuery",1);
        redAPICalls.put("endTransaction",1);
        redAPICalls.put("rawQueryWithFactory",1);
        redAPICalls.put("executeInsert",1);
        redAPICalls.put("queryWithFactory",1);
        redAPICalls.put("close",1);
        redAPICalls.put("onAllReferencesReleased",1);

        //File Manipulation
        redAPICalls.put("newSerializer",1);
        redAPICalls.put("openFileInput",1);
        redAPICalls.put("openRawResource",1);

        //Geolocation
        redAPICalls.put("getGpsStatus",1);
        redAPICalls.put("getCellLocation",1);

        //GUI Manipulation
        redAPICalls.put("setContentView",1);
        redAPICalls.put("show",1);
        redAPICalls.put("setComposingText",1);
        redAPICalls.put("makeText",1);
        redAPICalls.put("notifyDataSetChanged",1);
        redAPICalls.put("setSelection",1);
        //redAPICalls.put("update",1); // Repetitive - android.content.ContentResolver.update(android.net.Uri#android.content.ContentValues#java.lang.String#java.lang.String[])---
        //redAPICalls.put("query",1); // Repetitive - android.content.ContentResolver.query(android.net.Uri#java.lang.String[]#java.lang.String#java.lang.String[]#java.lang.String) and
        // android.content.ContentResolver.query(android.net.Uri#java.lang.String[]#java.lang.String#java.lang.String[]#java.lang.String#android.os.CancellationSignal)---
        // and android.content.ContentProvider.query(android.net.Uri#java.lang.String[]#java.lang.String#java.lang.String[]#java.lang.String#android.os.CancellationSignal)---
        redAPICalls.put("addView",1);
        redAPICalls.put("setProgress",1);
        redAPICalls.put("onCreateInputConnection",1);
        //redAPICalls.put("sendAccessibilityEvent",1); //Repetitive
        redAPICalls.put("layoutChildren",1);
        redAPICalls.put("release",1);
        redAPICalls.put("removeAllViews",1);
        //redAPICalls.put("addView",1);
        redAPICalls.put("setPressed",1);
        //redAPICalls.put("findViewById",1); // Repetitive - android.view.Window.findViewById(int)---
        redAPICalls.put("setText",1);
        redAPICalls.put("getTextBounds",1);
        redAPICalls.put("focusableViewAvailable",1);
        redAPICalls.put("dismiss",1);
        //redAPICalls.put("setText",1); // Repetitive - android.widget.TextView.setText(java.lang.CharSequence#android.widget.TextView.BufferType)---
        redAPICalls.put("dispatchSetPressed",1);
        redAPICalls.put("loadLabel",1);
        redAPICalls.put("loadIcon",1);
        redAPICalls.put("setMax",1);
        redAPICalls.put("refreshDrawableState",1);
        redAPICalls.put("getEditable",1);
        redAPICalls.put("setEnabled",1);
        redAPICalls.put("setSecondaryProgress",1);
        //redAPICalls.put("show",1); //Repetitive
        redAPICalls.put("create",1);
        redAPICalls.put("cancel",1);
        redAPICalls.put("drawText",1);
        redAPICalls.put("setLayoutParams",1);
        redAPICalls.put("setClickable",1);
        redAPICalls.put("invalidateChild",1);
        redAPICalls.put("getLongPressTimeout",1);

        //Image Manipulation
        redAPICalls.put("setImageResource",1);
        redAPICalls.put("decodeResourceStream",1);
        redAPICalls.put("connect",1);
        redAPICalls.put("createFromResourceStream",1);
        redAPICalls.put("sendBroadcast",1);
        redAPICalls.put("decodeStream",1);
        redAPICalls.put("createBitmap",1);
        redAPICalls.put("startAnimation",1);
        redAPICalls.put("playSoundEffect",1);
        redAPICalls.put("start",1);
        //redAPICalls.put("release",1); //Repetitive
        redAPICalls.put("TranslateAnimation",1);

        //Networking
        redAPICalls.put("createSocket",1);
        redAPICalls.put("getScanResults",1);

        //Services
        redAPICalls.put("getStackTraceString",1);
        redAPICalls.put("handleMessage",1);
        redAPICalls.put("Signature",1);
        redAPICalls.put("getDateFormat",1);
        redAPICalls.put("getExternalFilesDir",1);
        redAPICalls.put("getTimeFormat",1);
        redAPICalls.put("getConfiguration",1);
        redAPICalls.put("bindService",1);
        redAPICalls.put("onStartCommand",1);
        redAPICalls.put("dispatchMessage",1);
        redAPICalls.put("setNotificationUri",1);
        redAPICalls.put("getEnabledAccessibilityServiceList",1);
        redAPICalls.put("e",1);

        //Web
        redAPICalls.put("WebView",1);
        redAPICalls.put("loadData",1);
        redAPICalls.put("getProgress",1);
        redAPICalls.put("sync",1);
        redAPICalls.put("loadDataWithBaseURL",1);
        redAPICalls.put("loadUrl",1);


//        redAPICalls.put("performClick",1);
//        redAPICalls.put("getIntExtra",1);
//        redAPICalls.put("i",1);
//        redAPICalls.put("finish",1);
//        redAPICalls.put("cancelAll",1);
//        redAPICalls.put("startActivityForResult",1);
//        redAPICalls.put("findViewById",1);
//        redAPICalls.put("getPhoneType",1);
//        redAPICalls.put("clear",1);
//        redAPICalls.put("getPixel",1);
//        redAPICalls.put("getReadableDatabase",1);
//        redAPICalls.put("getWritableDatabase",1);
//        redAPICalls.put("openDatabase",1);
//        redAPICalls.put("update",1);
//        redAPICalls.put("query",1);
//        redAPICalls.put("insertOrThrow",1);
//        redAPICalls.put("execSQL",1);
//        redAPICalls.put("delete",1);
//        redAPICalls.put("insert",1);
//        redAPICalls.put("openOrCreateDatabase",1);
//        redAPICalls.put("rawQuery",1);
//        redAPICalls.put("getVersion",1);
//        redAPICalls.put("endTransaction",1);
//        redAPICalls.put("executeInsert",1);
//        redAPICalls.put("openRawResource",1);
//        redAPICalls.put("getGpsStatus",1);
//        redAPICalls.put("setContentView",1);
//        redAPICalls.put("show",1);
//        redAPICalls.put("makeText",1);
//        redAPICalls.put("notifyDataSetChanged",1);
//        redAPICalls.put("setSelection",1);
//        redAPICalls.put("addView",1);
//        redAPICalls.put("removeAllViews",1);
//        redAPICalls.put("setText",1);
//        redAPICalls.put("getTextBounds",1);
//        redAPICalls.put("setTextColor",1);
//        redAPICalls.put("dismiss",1);
//        redAPICalls.put("setMax",1);
//        redAPICalls.put("setEnabled",1);
//        redAPICalls.put("drawText",1);
//        redAPICalls.put("setLayoutParams",1);
//        redAPICalls.put("setClickable",1);
//        redAPICalls.put("getLongPressTimeout",1);
//        redAPICalls.put("setImageResource",1);
//        redAPICalls.put("openRawResource",1);
//        redAPICalls.put("decodeStream",1);
//        redAPICalls.put("createBitmap",1);
//        redAPICalls.put("startAnimation",1);
//        redAPICalls.put("getStackTraceString",1);
//        redAPICalls.put("getDateFormat",1);
//        redAPICalls.put("onStartCommand",1);
//        redAPICalls.put("dispatchMessage",1);
//        redAPICalls.put("e",1);
//        redAPICalls.put("loadData",1);
//        redAPICalls.put("loadDataWithBaseURL",1);
//        redAPICalls.put("loadUrl",1);

        NUMBER_OF_RED_APIS = redAPICalls.size();
    }


    private void setJointAPICallsMap(){
        jointRedAPICalls.clear();

        jointRedAPICalls.put("create",1);
        jointRedAPICalls.put("start",1);
        jointRedAPICalls.put("stop",1);
        jointRedAPICalls.put("pause",1);
        jointRedAPICalls.put("resume",1);

    }

    private void setJointRedAPIsParents(){
        hwAPICalls.put("MediaPlayer.create",1);
    }

//    public void setProject(final Project project) {
//        this.project = project;
//    }
//
//    public Project getProject(){
//        return project;
//    }
}
