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

public class VXMLDocDataCollector extends AGenericInterceptor {
    /*
    getVxmlDocumentFromDomCache	com.cisco.voicebrowser.VxmlDocument	getVxmlDocumentFromDomCache	UniqueId	POSITION_GATHERER_TYPE	0	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	ctx.uniqueId
    getVxmlDocumentFromDomCache	com.cisco.voicebrowser.VxmlDocument	getVxmlDocumentFromDomCache	VXMLReqURL	POSITION_GATHERER_TYPE	1	TO_STRING_OBJECT_DATA_TRANSFORMER_TYPE
     */

    IReflector toString, getCTXUniqueId, getStateString;
    private HashSet<DataScope> dataScopesAll, dataScopeSnapshot;

    public VXMLDocDataCollector() {
        super();

        dataScopesAll = new HashSet<DataScope>() {{
            add(DataScope.SNAPSHOTS);
            add(DataScope.ANALYTICS);
        }};

        dataScopeSnapshot = new HashSet<DataScope>() {{ add(DataScope.SNAPSHOTS); }};

        toString = getNewReflectionBuilder().invokeInstanceMethod("toString", true).build(); //String
        getCTXUniqueId = getNewReflectionBuilder().accessFieldValue("ctx", true).accessFieldValue("uniqueId",true).build();
        getStateString = getNewReflectionBuilder().invokeInstanceMethod("getStateString", true).build(); //String "DONE" == success

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
        transaction.collectData("UniqueId", getReflectiveString(params[0], getCTXUniqueId, "UNKNOWN-UNIQUEID"), dataScopesAll);
        transaction.collectData("VXMLReqURL", getReflectiveString(params[1], toString, "UNKNOWN-VXMLURL"), dataScopesAll);
        String finalState = getReflectiveString( object, getStateString, "UNKNOWN-STATE");
        transaction.collectData("State", finalState, dataScopeSnapshot);
        if( !"DONE".equals(finalState) ) {
            transaction.markAsError(String.format("Final State is not DONE: %s",finalState));
        }
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
