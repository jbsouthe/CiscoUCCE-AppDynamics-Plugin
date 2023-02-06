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

public class InvokeVBSessionDataCollector extends AGenericInterceptor {
    /*
    invokeVBSession	com.cisco.voicebrowser.VoiceBrowser	invokeVBSession	ANI	POSITION_GATHERER_TYPE	1	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	get(CALL_ANI)
    invokeVBSession	com.cisco.voicebrowser.VoiceBrowser	invokeVBSession	AppName	POSITION_GATHERER_TYPE	1	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	get(ApplicationName)
    invokeVBSession	com.cisco.voicebrowser.VoiceBrowser	invokeVBSession	DNIS	POSITION_GATHERER_TYPE	1	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	get(CALL_DNIS)
    invokeVBSession	com.cisco.voicebrowser.VoiceBrowser	invokeVBSession	UniqueId	POSITION_GATHERER_TYPE	1	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	get(UNIQUE_ID)
     */

    IReflector get;
    private HashSet<DataScope> dataScopesAll;

    public InvokeVBSessionDataCollector() {
        super();

        dataScopesAll = new HashSet<DataScope>() {{
            add(DataScope.SNAPSHOTS);
            add(DataScope.ANALYTICS);
        }};

        get = getNewReflectionBuilder().invokeInstanceMethod("get", true, new String[] { String.class.getCanonicalName() }).build();

        getLogger().info(String.format("Initialized plugin version %s, build date %s, contact gecos: %s", MetaData.VERSION, MetaData.BUILDTIMESTAMP, MetaData.GECOS));
    }

    @Override
    public Object onMethodBegin(Object objectIntercepted, String className, String methodName, Object[] params) {
        //doing all the data collection after the method executes so we can be sure the BT has started from another interceptor
        return null;
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        Transaction transaction = AppdynamicsAgent.getTransaction();
        if( transaction instanceof NoOpTransaction ) return;
        transaction.collectData("ANI", getReflectiveString(params[1], get, "CALL_ANI", "UNKNOWN-CALLANI"), dataScopesAll);
        transaction.collectData("AppName", getReflectiveString(params[1], get, "ApplicationName", "UNKNOWN-APPLICATION"), dataScopesAll);
        transaction.collectData("DNIS", getReflectiveString(params[1], get, "CALL_DNIS", "UNKNOWN-CALLDNIS"), dataScopesAll);
        transaction.collectData("UniqueId", getReflectiveString(params[1], get, "UNIQUE_ID", "UNKNOWN-UNIQUEID"), dataScopesAll);
    }

    @Override
    public List<Rule> initializeRules() {
        List<Rule> rules = new ArrayList<Rule>();

        rules.add(new Rule.Builder(
                "com.cisco.voicebrowser.VxmlDocument")
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("getVxmlDocumentFromDomCache")
                .build()
        );
        return rules;
    }

    private String getReflectiveString(Object object, IReflector method, String argument, String defaultString) {
        String value = defaultString;
        if( object == null || method == null ) return defaultString;
        try{
            value = (String) method.execute(object.getClass().getClassLoader(), object, new Object[]{ argument });
            if( value == null ) return defaultString;
        } catch (ReflectorException e) {
            this.getLogger().info("Error in reflection call, exception: "+ e.getMessage(),e);
        }
        return value;
    }
}
