package com.appdynamics.isdk.ciscoUCCE.dataCollector;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.Transaction;
import com.appdynamics.agent.api.impl.NoOpTransaction;
import com.appdynamics.apm.appagent.api.DataScope;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.template.AGenericInterceptor;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.IReflector;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.ReflectorException;
import com.appdynamics.isdk.ciscoUCCE.MetaData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ControllerDataCollector extends AGenericInterceptor {
    /*
    continueCall	com.audium.server.controller.Controller	continueCall	ActionName	POSITION_GATHERER_TYPE	4	TO_STRING_OBJECT_DATA_TRANSFORMER_TYPE
    continueCall	com.audium.server.controller.Controller	continueCall	ANI	POSITION_GATHERER_TYPE	2	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	getAni()
    continueCall	com.audium.server.controller.Controller	continueCall	AppName	POSITION_GATHERER_TYPE	2	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	getApplicationName()
    continueCall	com.audium.server.controller.Controller	continueCall	CALL_SESSIONID	POSITION_GATHERER_TYPE	2	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	getSessionId()
    continueCall	com.audium.server.controller.Controller	continueCall	DNIS	POSITION_GATHERER_TYPE	2	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	getDnis()
    continueCall	com.audium.server.controller.Controller	continueCall	ElementName	POSITION_GATHERER_TYPE	3	TO_STRING_OBJECT_DATA_TRANSFORMER_TYPE
     */
    IReflector toString, getAni, getApplicationName, getSessionId, getDnis;
    private HashSet<DataScope> dataScopesAll;

    public ControllerDataCollector() {
        super();

        dataScopesAll = new HashSet<DataScope>() {{
            add(DataScope.SNAPSHOTS);
            add(DataScope.ANALYTICS);
        }};

        toString = getNewReflectionBuilder().invokeInstanceMethod("toString", true).build(); //String
        getAni = getNewReflectionBuilder().invokeInstanceMethod("getAni", true).build(); //String?
        getApplicationName = getNewReflectionBuilder().invokeInstanceMethod("getApplicationName", true).build(); //String?
        getSessionId = getNewReflectionBuilder().invokeInstanceMethod("getSessionId", true).build(); //String?
        getDnis = getNewReflectionBuilder().invokeInstanceMethod("getDnis", true).build(); //String?

        getLogger().info(String.format("Initialized plugin version %s, build date %s, contact gecos: %s", MetaData.VERSION, MetaData.BUILDTIMESTAMP, MetaData.GECOS));
    }

    @Override
    public Object onMethodBegin(Object objectIntercepted, String className, String methodName, Object[] params) {
        return null;
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        Object sessionContext = params[2];
        Object element = params[3];
        Object action = params[4];
        Transaction transaction = AppdynamicsAgent.getTransaction();
        if( transaction instanceof NoOpTransaction ) return;
        transaction.collectData("ActionName", getReflectiveString(action, toString, "UNKNOWN-ACTION"), dataScopesAll);
        transaction.collectData("ANI", getReflectiveString(sessionContext, getAni, "UNKNOWN-ANI"), dataScopesAll);
        transaction.collectData("AppName", getReflectiveString(sessionContext, getApplicationName, "UNKNOWN-APPLICATION"), dataScopesAll);
        transaction.collectData("CALL_SESSIONID", getReflectiveString(sessionContext, getSessionId, "UNKNOWN-SESSIONID"), dataScopesAll);
        transaction.collectData("DNIS", getReflectiveString(sessionContext, getDnis, "UNKNOWN-DNIS"), dataScopesAll);
        transaction.collectData("ElementName", getReflectiveString(element, toString, "UNKNOWN-ELEMENT"), dataScopesAll);
    }

    @Override
    public List<Rule> initializeRules() {
        List<Rule> rules = new ArrayList<Rule>();

        rules.add(new Rule.Builder(
                "com.audium.server.controller.Controller")
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("continueCall")
                .build()
        );
        return rules;
    }

    private String getReflectiveString(Object object, IReflector method, String defaultString) {
        String value = defaultString;
        if( object == null || method == null ) return defaultString;
        try{
            value = (String) method.execute(object.getClass().getClassLoader(), object);
            if( value == null ) return defaultString;
        } catch (ReflectorException e) {
            this.getLogger().info("Error in reflection call, exception: "+ e.getMessage(),e);
        }
        return value;
    }
}
