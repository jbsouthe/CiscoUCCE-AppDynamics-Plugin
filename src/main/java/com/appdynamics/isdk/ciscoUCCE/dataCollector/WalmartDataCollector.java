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

public class WalmartDataCollector extends AGenericInterceptor {
    /*
    doDataAccessNuance	com.nuance.walmart.store.startcall.walst0107_InitializeCallData_DB	doDataAccess	StoreId	POSITION_GATHERER_TYPE	2	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	getCallflowData().getStoreId()
     */
    IReflector getStoreId;
    private HashSet<DataScope> dataScopesAll;

    public WalmartDataCollector() {
        super();

        dataScopesAll = new HashSet<DataScope>() {{
            add(DataScope.SNAPSHOTS);
            add(DataScope.ANALYTICS);
        }};

        getStoreId = getNewReflectionBuilder().invokeInstanceMethod("getCallflowData", true).invokeInstanceMethod("getStoreId", true).build(); //String

        getLogger().info(String.format("Initialized plugin version %s, build date %s, contact gecos: %s", MetaData.VERSION, MetaData.BUILDTIMESTAMP, MetaData.GECOS));
    }

    @Override
    public Object onMethodBegin(Object objectIntercepted, String className, String methodName, Object[] params) {
        return null;
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        Transaction transaction = AppdynamicsAgent.getTransaction();
        if( transaction instanceof NoOpTransaction ) return;
        transaction.collectData("StoreId", getReflectiveString(params[2], getStoreId, "UNKNOWN-STORE"), dataScopesAll);
    }

    @Override
    public List<Rule> initializeRules() {
        List<Rule> rules = new ArrayList<Rule>();

        rules.add(new Rule.Builder(
                "com.nuance.walmart.store.startcall.walst0107_InitializeCallData_DB")
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("doDataAccess")
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
