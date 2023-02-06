package com.appdynamics.isdk.ciscoUCCE.dataCollector;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.template.AGenericInterceptor;
import com.appdynamics.isdk.ciscoUCCE.MetaData;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryEventInterceptor extends AGenericInterceptor {

    public InventoryEventInterceptor() {
        //the only purpose of this is to generate an event notifying the controller that the plugin has been initialized.
        Map<String,String> map = new HashMap<>();
        map.put("Init Timestamp", new Date().toString() );
        map.put("Plugin Version", MetaData.VERSION);
        map.put("Build Date", MetaData.BUILDTIMESTAMP);
        map.put("Plugin Contact", MetaData.GECOS);
        map.put("Java Version", System.getProperty("java.version"));
        map.put("Java Vendor", System.getProperty("java.vendor"));
        AppdynamicsAgent.getEventPublisher().publishInfoEvent("Cisco UCCE AppDynamics iSDK Plugin Initialized", map, "APPLICATION_INFO");
    }

    @Override
    public Object onMethodBegin(Object o, String s, String s1, Object[] objects) {
        return null;
    }

    @Override
    public void onMethodEnd(Object o, Object o1, String s, String s1, Object[] objects, Throwable throwable, Object o2) {

    }

    @Override
    public List<Rule> initializeRules() {
        return null;
    }
}
