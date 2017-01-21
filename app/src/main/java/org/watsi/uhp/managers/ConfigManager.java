package org.watsi.uhp.managers;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

import com.rollbar.android.Rollbar;

import org.watsi.uhp.R;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Static method for reading config settings stored
 * in the res/xml/config.xml file
 */
public class ConfigManager {
    private static String ROLLBAR_API_KEY = "ROLLBAR_API_KEY";

    public static String getRollbarApiKey(Activity activity) {
        String key = readConfig(activity).get(ConfigManager.ROLLBAR_API_KEY);
        if (key == null) {
            throw new RuntimeException("must set ROLLBAR_API_KEY in res/xml/secret.xml");
        }
        return key;
    }

    private static Map<String,String> readConfig(Activity activity) {
        Map<String,String> configMap = new HashMap<String,String>();
        Resources res = activity.getResources();
        XmlResourceParser xrp = res.getXml(R.xml.secret);

        int eventType = -1;
        try {
            while(eventType != XmlResourceParser.END_DOCUMENT) {
                if (xrp.getEventType() == XmlResourceParser.START_TAG) {
                    if (xrp.getName().equals("entry")) {
                        String key = xrp.getAttributeValue(null, "key");
                        xrp.next();
                        String value = xrp.getText();
                        configMap.put(key, value);
                    }
                }
                xrp.next();
                eventType = xrp.getEventType();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.d("UHP", e.getMessage());
            Rollbar.reportException(e);
        }
        return configMap;
    }
}