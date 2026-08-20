package com.whl.quickjs.wrapper;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JSUtils<T> {

    public static boolean isEmpty(Object obj) {
        if (obj == null) return true;
        else if (obj instanceof CharSequence) return ((CharSequence) obj).length() == 0;
        else if (obj instanceof Collection) return ((Collection) obj).isEmpty();
        else if (obj instanceof Map) return ((Map) obj).isEmpty();
        else if (obj.getClass().isArray()) return Array.getLength(obj) == 0;

        return false;
    }

    public static boolean isNotEmpty(CharSequence str) {
        return !isEmpty(str);
    }

    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    public JSArray toArray(QuickJSContext ctx, List<T> items) {
        JSArray array = ctx.createJSArray();
        if (items == null || items.isEmpty()) return array;
        for (int i = 0; i < items.size(); i++) array.push(items.get(i));
        return array;
    }

    public JSArray toArray(QuickJSContext ctx, byte[] bytes) {
        JSArray array = ctx.createJSArray();
        if (bytes == null || bytes.length == 0) return array;
        for (byte aByte : bytes) array.push((int) aByte);
        return array;
    }

    public JSArray toArray(QuickJSContext ctx, T[] arrays) {
        JSArray array = ctx.createJSArray();
        if (arrays == null || arrays.length == 0) return array;
        for (T t : arrays) {
            array.push(t);
        }
        return array;
    }

    public JSObject toObj(QuickJSContext ctx, Map<String, T> map) {
        JSObject obj = ctx.createJSObject();
        if (map == null || map.isEmpty()) return obj;
        for (String s : map.keySet()) {
            obj.set(s, map.get(s));
        }
        return obj;
    }
    public static org.json.JSONObject toJsonObject(JSObject obj) {
        org.json.JSONObject json = new org.json.JSONObject();
        try {
            String[] keys = obj.getKeys();
            if (keys != null) {
                for (String key : keys) {
                    Object v = obj.getProperty(key);
                    if (v instanceof JSObject) {
                        json.put(key, toJsonObject((JSObject) v));
                    } else if (v instanceof JSArray) {
                        json.put(key, toJsonArray((JSArray) v));
                    } else {
                        json.put(key, v);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static org.json.JSONArray toJsonArray(JSArray array) {
        return array.toJSONArray();
    }

}
