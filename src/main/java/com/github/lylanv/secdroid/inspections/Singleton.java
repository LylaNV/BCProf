package com.github.lylanv.secdroid.inspections;

import com.github.lylanv.secdroid.utils.ThreeStringKey;
import com.github.lylanv.secdroid.utils.TwoStringKey;

import java.util.Map;
import java.util.HashMap;

public class Singleton {
    //    public static Project project;
    public static Map<String, Double> redAPICalls = new HashMap<>(); //holds red API call's name and its energy cost
    public static Map<String, Double> jointRedAPICalls = new HashMap<>();
    public static Map<String, Double> hwAPICalls = new HashMap<>();
    public static int NUMBER_OF_RED_APIS;

    public static Map<ThreeStringKey, Integer> methodsAPICallsCountMap = new HashMap<>();
    public static Map<TwoStringKey, Double> methodsAPICallsTotalEnergyCostMap = new HashMap<>();

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
    public static void setAPICallsMap() {
        //TODO: you have some the same method expression such as query, updated, addView, findViewById, setText and show
        // consider to do something with them. Their energy consumption could be different
        //redAPICalls.clear();

        //Activity and Context
        redAPICalls.put("onCreate",0.00039); //NOT REAL, NOT RED API CALL
        redAPICalls.put("performClick",0.00000000349246); // android.view.View.performClick() & android.widget.CompoundButton.performClick()
        redAPICalls.put("performItemClick",1.0);
        redAPICalls.put("CrashInfo",1.0);
        redAPICalls.put("dispatchWindowFocusChanged",1.0);
        redAPICalls.put("onChange",1.0);
        redAPICalls.put("sendAccessibilityEvent",1.0);
        redAPICalls.put("getInt",1.0);
        redAPICalls.put("getStringExtra",0.000000000139699);
        redAPICalls.put("getIntExtra", 0.000000000167638);
        redAPICalls.put("putExtra", 0.000000000167638); //NOT REAL, NOT RED API CALL
        redAPICalls.put("i",0.000000000176); //NOT REAL, NOT RED API CALL
        redAPICalls.put("d",0.000000000176); //NOT REAL, NOT RED API CALL
        redAPICalls.put("finish",0.00039); //NOT REAL
        redAPICalls.put("cancelAll",1.0);
        redAPICalls.put("startActivityForResult",0.00041215);
        redAPICalls.put("findViewById",0.0000000000256114); //2,56E-11
        redAPICalls.put("getPhoneType",1.0);
        redAPICalls.put("writeBundle",1.0);
        redAPICalls.put("clear",1.0);
        redAPICalls.put("createTypedArrayList",1.0);
        redAPICalls.put("getPixel",1.0);

        //Database
        redAPICalls.put("getReadableDatabase",1.0);
        redAPICalls.put("getWritableDatabase",1.0);
        redAPICalls.put("openOrCreateDatabase",1.0);
        redAPICalls.put("openDatabase",1.0);
        redAPICalls.put("update",1.0); //android.database.sqlite.SQLiteDatabase.update(java.lang.String#android.content.ContentValues#java.lang.String#java.lang.String[])
        redAPICalls.put("query",1.0); //android.database.sqlite.SQLiteQueryBuilder.query(android.database.sqlite.SQLiteDatabase#java.lang.String[]#java.lang.String#java.lang.String[]#java.lang.String#java.lang.String#java.lang.String#java.lang.String#android.os.CancellationSignal)
        //and android.database.sqlite.SQLiteDatabase.query(java.lang.String#java.lang.String[]#java.lang.String#java.lang.String[]#java.lang.String#java.lang.String#java.lang.String)---
        //and android.database.sqlite.SQLiteDatabase.query(boolean#java.lang.String#java.lang.String[]#java.lang.String#java.lang.String[]#java.lang.String#java.lang.String#java.lang.String#java.lang.String)---
        redAPICalls.put("insertOrThrow",1.0);
        redAPICalls.put("execSQL",1.0);
        redAPICalls.put("delete",1.0);
        redAPICalls.put("insert",1.0);
        redAPICalls.put("insertWithOnConflict",1.0);
        //redAPICalls.put("openOrCreateDatabase",1.0); //Repetitive
        redAPICalls.put("updateWithOnConflict",1.0);
        redAPICalls.put("deleteDatabase",1.0);
        redAPICalls.put("longForQuery",1.0);
        redAPICalls.put("getCount",1.0);
        redAPICalls.put("executeUpdateDelete",1.0);
        redAPICalls.put("setVersion",1.0);
        redAPICalls.put("rawQuery",1.0);
        redAPICalls.put("getVersion",1.0);
        redAPICalls.put("buildQuery",1.0);
        redAPICalls.put("endTransaction",1.0);
        redAPICalls.put("rawQueryWithFactory",1.0);
        redAPICalls.put("executeInsert",1.0);
        redAPICalls.put("queryWithFactory",1.0);
        redAPICalls.put("close",1.0);
        redAPICalls.put("onAllReferencesReleased",1.0);

        //File Manipulation
        redAPICalls.put("newSerializer",1.0);
        redAPICalls.put("openFileInput",1.0);
        redAPICalls.put("openRawResource",1.0);

        //Geolocation
        redAPICalls.put("getGpsStatus",1.0);
        redAPICalls.put("getCellLocation",1.0);

        //GUI Manipulation
        redAPICalls.put("setContentView",0.0000136); //1,36085E-05
        redAPICalls.put("show",1.0);
        redAPICalls.put("setComposingText",1.0);
        redAPICalls.put("makeText",1.0);
        redAPICalls.put("notifyDataSetChanged",1.0);
        redAPICalls.put("setSelection",1.0);
        //redAPICalls.put("update",1.0); // Repetitive - android.content.ContentResolver.update(android.net.Uri#android.content.ContentValues#java.lang.String#java.lang.String[])---
        //redAPICalls.put("query",1.0); // Repetitive - android.content.ContentResolver.query(android.net.Uri#java.lang.String[]#java.lang.String#java.lang.String[]#java.lang.String) and
        // android.content.ContentResolver.query(android.net.Uri#java.lang.String[]#java.lang.String#java.lang.String[]#java.lang.String#android.os.CancellationSignal)---
        // and android.content.ContentProvider.query(android.net.Uri#java.lang.String[]#java.lang.String#java.lang.String[]#java.lang.String#android.os.CancellationSignal)---
        redAPICalls.put("addView",1.0);
        redAPICalls.put("setProgress",1.0);
        redAPICalls.put("onCreateInputConnection",1.0);
        //redAPICalls.put("sendAccessibilityEvent",1.0); //Repetitive
        redAPICalls.put("layoutChildren",1.0);
        redAPICalls.put("release",1.0);
        redAPICalls.put("removeAllViews",1.0);
        //redAPICalls.put("addView",1.0);
        redAPICalls.put("setPressed",1.0);
        //redAPICalls.put("findViewById",1.0); // Repetitive - android.view.Window.findViewById(int)---
        redAPICalls.put("setText",0.00000000349246);
        redAPICalls.put("getText",0.00000000349246);//NOT REAL, NOT RED API CALL
        redAPICalls.put("getTextBounds",1.0);
        redAPICalls.put("focusableViewAvailable",1.0);
        redAPICalls.put("dismiss",1.0);
        //redAPICalls.put("setText",1.0); // Repetitive - android.widget.TextView.setText(java.lang.CharSequence#android.widget.TextView.BufferType)---
        redAPICalls.put("dispatchSetPressed",1.0);
        redAPICalls.put("loadLabel",1.0);
        redAPICalls.put("loadIcon",1.0);
        redAPICalls.put("setMax",1.0);
        redAPICalls.put("refreshDrawableState",1.0);
        redAPICalls.put("getEditable",1.0);
        redAPICalls.put("setEnabled",1.0);
        redAPICalls.put("setSecondaryProgress",1.0);
        //redAPICalls.put("show",1.0); //Repetitive
        redAPICalls.put("create",1.0);
        redAPICalls.put("cancel",1.0);
        redAPICalls.put("drawText",1.0);
        redAPICalls.put("setLayoutParams",1.0);
        redAPICalls.put("setClickable",1.0);
        redAPICalls.put("invalidateChild",1.0);
        redAPICalls.put("getLongPressTimeout",1.0);

        //Image Manipulation
        redAPICalls.put("setImageResource",1.0);
        redAPICalls.put("decodeResourceStream",1.0);
        redAPICalls.put("connect",1.0);
        redAPICalls.put("createFromResourceStream",1.0);
        redAPICalls.put("sendBroadcast",1.0);
        redAPICalls.put("decodeStream",1.0);
        redAPICalls.put("createBitmap",1.0);
        redAPICalls.put("startAnimation",1.0);
        redAPICalls.put("playSoundEffect",1.0);
        redAPICalls.put("start",1.0);
        //redAPICalls.put("release",1.0); //Repetitive
        redAPICalls.put("TranslateAnimation",1.0);

        //Networking
        redAPICalls.put("createSocket",1.0);
        redAPICalls.put("getScanResults",1.0);

        //Services
        redAPICalls.put("getStackTraceString",1.0);
        redAPICalls.put("handleMessage",1.0);
        redAPICalls.put("Signature",1.0);
        redAPICalls.put("getDateFormat",1.0);
        redAPICalls.put("getExternalFilesDir",1.0);
        redAPICalls.put("getTimeFormat",1.0);
        redAPICalls.put("getConfiguration",1.0);
        redAPICalls.put("bindService",1.0);
        redAPICalls.put("onStartCommand",1.0);
        redAPICalls.put("dispatchMessage",1.0);
        redAPICalls.put("setNotificationUri",1.0);
        redAPICalls.put("getEnabledAccessibilityServiceList",1.0);
        redAPICalls.put("e",0.000000000176); //NOT REAL, NOT RED API CALL

        //Web
        redAPICalls.put("WebView",1.0);
        redAPICalls.put("loadData",1.0);
        redAPICalls.put("getProgress",1.0);
        redAPICalls.put("sync",1.0);
        redAPICalls.put("loadDataWithBaseURL",1.0);
        redAPICalls.put("loadUrl",1.0);


//        redAPICalls.put("performClick",1.0);
//        redAPICalls.put("getIntExtra",1.0);
//        redAPICalls.put("i",1.0);
//        redAPICalls.put("finish",1.0);
//        redAPICalls.put("cancelAll",1.0);
//        redAPICalls.put("startActivityForResult",1.0);
//        redAPICalls.put("findViewById",1.0);
//        redAPICalls.put("getPhoneType",1.0);
//        redAPICalls.put("clear",1.0);
//        redAPICalls.put("getPixel",1.0);
//        redAPICalls.put("getReadableDatabase",1.0);
//        redAPICalls.put("getWritableDatabase",1.0);
//        redAPICalls.put("openDatabase",1.0);
//        redAPICalls.put("update",1.0);
//        redAPICalls.put("query",1.0);
//        redAPICalls.put("insertOrThrow",1.0);
//        redAPICalls.put("execSQL",1.0);
//        redAPICalls.put("delete",1.0);
//        redAPICalls.put("insert",1.0);
//        redAPICalls.put("openOrCreateDatabase",1.0);
//        redAPICalls.put("rawQuery",1.0);
//        redAPICalls.put("getVersion",1.0);
//        redAPICalls.put("endTransaction",1.0);
//        redAPICalls.put("executeInsert",1.0);
//        redAPICalls.put("openRawResource",1.0);
//        redAPICalls.put("getGpsStatus",1.0);
//        redAPICalls.put("setContentView",1.0);
//        redAPICalls.put("show",1.0);
//        redAPICalls.put("makeText",1.0);
//        redAPICalls.put("notifyDataSetChanged",1.0);
//        redAPICalls.put("setSelection",1.0);
//        redAPICalls.put("addView",1.0);
//        redAPICalls.put("removeAllViews",1.0);
//        redAPICalls.put("setText",1.0);
//        redAPICalls.put("getTextBounds",1.0);
//        redAPICalls.put("setTextColor",1.0);
//        redAPICalls.put("dismiss",1.0);
//        redAPICalls.put("setMax",1.0);
//        redAPICalls.put("setEnabled",1.0);
//        redAPICalls.put("drawText",1.0);
//        redAPICalls.put("setLayoutParams",1.0);
//        redAPICalls.put("setClickable",1.0);
//        redAPICalls.put("getLongPressTimeout",1.0);
//        redAPICalls.put("setImageResource",1.0);
//        redAPICalls.put("openRawResource",1.0);
//        redAPICalls.put("decodeStream",1.0);
//        redAPICalls.put("createBitmap",1.0);
//        redAPICalls.put("startAnimation",1.0);
//        redAPICalls.put("getStackTraceString",1.0);
//        redAPICalls.put("getDateFormat",1.0);
//        redAPICalls.put("onStartCommand",1.0);
//        redAPICalls.put("dispatchMessage",1.0);
//        redAPICalls.put("e",1.0);
//        redAPICalls.put("loadData",1.0);
//        redAPICalls.put("loadDataWithBaseURL",1.0);
//        redAPICalls.put("loadUrl",1.0);

        NUMBER_OF_RED_APIS = redAPICalls.size();
    }

    public void fillMethodsAPICallsCountMap(Map<ThreeStringKey, Integer> methodsAPICallsCountMap) {
        this.methodsAPICallsCountMap = methodsAPICallsCountMap;
    }

    public void fillMethodsAPICallsEnergyMap(Map<TwoStringKey, Double> methodsAPICallsTotalEnergyCostMap) {
        this.methodsAPICallsTotalEnergyCostMap = methodsAPICallsTotalEnergyCostMap;
    }

    private void setJointAPICallsMap(){
        jointRedAPICalls.clear();

        jointRedAPICalls.put("create",1.0);
        jointRedAPICalls.put("start",1.0);
        jointRedAPICalls.put("stop",1.0);
        jointRedAPICalls.put("pause",1.0);
        jointRedAPICalls.put("resume",1.0);

    }


    private void setJointRedAPIsParents(){
        hwAPICalls.put("MediaPlayer.create",1.0);
    }


//    public int getEnergyConsumptionOfRedAPI(String redAPICallName){
//        return redAPICalls.get(redAPICallName);
//    }

//    public void setProject(final Project project) {
//        this.project = project;
//    }
//
//    public Project getProject(){
//        return project;
//    }
}
