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

public class NuanceSessionDataCollector extends AGenericInterceptor {
    /*
    Nuance_updateSessionObject	com.nuance.framework.cvp.session.ApplicationSessionManager	updateSessionObject	Nuance_AppTag	POSITION_GATHERER_TYPE	0	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	getCallflowData().getAppTag()
    Nuance_updateSessionObject	com.nuance.framework.cvp.session.ApplicationSessionManager	updateSessionObject	Nuance_DepartmentId	POSITION_GATHERER_TYPE	0	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	getCallflowData().getDepartmentId()
    Nuance_updateSessionObject	com.nuance.framework.cvp.session.ApplicationSessionManager	updateSessionObject	Nuance_Extension	POSITION_GATHERER_TYPE	0	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	getCallflowData().getExtension()
    Nuance_updateSessionObject	com.nuance.framework.cvp.session.ApplicationSessionManager	updateSessionObject	nuance_icmData_ani	POSITION_GATHERER_TYPE	0	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	icmData.ani
    Nuance_updateSessionObject	com.nuance.framework.cvp.session.ApplicationSessionManager	updateSessionObject	nuance_icmData_dnis	POSITION_GATHERER_TYPE	0	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	icmData.dnis
    Nuance_updateSessionObject	com.nuance.framework.cvp.session.ApplicationSessionManager	updateSessionObject	Nuance_StoreCityState	POSITION_GATHERER_TYPE	0	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	getCallflowData().getStoreCityState()
    Nuance_updateSessionObject	com.nuance.framework.cvp.session.ApplicationSessionManager	updateSessionObject	Nuance_StoreId	POSITION_GATHERER_TYPE	0	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	getCallflowData().getStoreId()
    Nuance_updateSessionObject	com.nuance.framework.cvp.session.ApplicationSessionManager	updateSessionObject	Nuance_TransferAttempt	POSITION_GATHERER_TYPE	0	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	getCallflowData().getTransferAttempt()
    Nuance_updateSessionObject	com.nuance.framework.cvp.session.ApplicationSessionManager	updateSessionObject	Nuance_XferResult	POSITION_GATHERER_TYPE	0	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	getCallflowData().getXferResult()
     */
    IReflector getAppTag, getDepartmentId, getExtension, getIcmDataAni, getIcmDataDnis, getStoreCityState, getStoreId, getTransferAttempt, getXferResult;
    private HashSet<DataScope> dataScopesAll;

    public NuanceSessionDataCollector() {
        super();

        dataScopesAll = new HashSet<DataScope>() {{
            add(DataScope.SNAPSHOTS);
            add(DataScope.ANALYTICS);
        }};

        getAppTag = getNewReflectionBuilder().invokeInstanceMethod("getCallflowData", true).invokeInstanceMethod("getAppTag", true).build();
        getDepartmentId = getNewReflectionBuilder().invokeInstanceMethod("getCallflowData", true).invokeInstanceMethod("getDepartmentId", true).build();
        getExtension = getNewReflectionBuilder().invokeInstanceMethod("getCallflowData", true).invokeInstanceMethod("getExtension", true).build();
        getStoreCityState = getNewReflectionBuilder().invokeInstanceMethod("getCallflowData", true).invokeInstanceMethod("getStoreCityState", true).build();
        getStoreId = getNewReflectionBuilder().invokeInstanceMethod("getCallflowData", true).invokeInstanceMethod("getStoreId", true).build();
        getTransferAttempt = getNewReflectionBuilder().invokeInstanceMethod("getCallflowData", true).invokeInstanceMethod("getTransferAttempt", true).build();
        getXferResult = getNewReflectionBuilder().invokeInstanceMethod("getCallflowData", true).invokeInstanceMethod("getXferResult", true).build();
        getIcmDataAni = getNewReflectionBuilder().accessFieldValue("icmData", true).accessFieldValue("ani", true).build();
        getIcmDataDnis = getNewReflectionBuilder().accessFieldValue("icmData", true).accessFieldValue("dnis", true).build();

        getLogger().info(String.format("Initialized plugin version %s, build date %s, contact gecos: %s", MetaData.VERSION, MetaData.BUILDTIMESTAMP, MetaData.GECOS));
    }

    @Override
    public Object onMethodBegin(Object objectIntercepted, String className, String methodName, Object[] params) {
        return null;
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        Object session = params[0];
        Transaction transaction = AppdynamicsAgent.getTransaction();
        if( transaction instanceof NoOpTransaction ) return;
        transaction.collectData("Nuance_AppTag", getReflectiveString(session, getAppTag, "UNKNOWN-APPTAG"), dataScopesAll);
        transaction.collectData("Nuance_DepartmentId", getReflectiveString(session, getDepartmentId, "UNKNOWN-DEPARTMENTID"), dataScopesAll);
        transaction.collectData("Nuance_Extension", getReflectiveString(session, getExtension, "UNKNOWN-EXTENSION"), dataScopesAll);
        transaction.collectData("Nuance_StoreCityState", getReflectiveString(session, getStoreCityState, "UNKNOWN-STORECITYSTATE"), dataScopesAll);
        transaction.collectData("Nuance_StoreId", getReflectiveString(session, getStoreId, "UNKNOWN-STOREID"), dataScopesAll);
        transaction.collectData("Nuance_TransferAttempt", getReflectiveString(session, getTransferAttempt, "UNKNOWN-TRANSFERATTEMPT"), dataScopesAll);
        transaction.collectData("Nuance_XferResult", getReflectiveString(session, getXferResult, "UNKNOWN-XFERRESULT"), dataScopesAll);
        transaction.collectData("nuance_icmData_ani", getReflectiveString(session, getIcmDataAni, "UNKNOWN-ICMANI"), dataScopesAll);
        transaction.collectData("nuance_icmData_dnis", getReflectiveString(session, getIcmDataDnis, "UNKNOWN-ICMDNIS"), dataScopesAll);
    }

    @Override
    public List<Rule> initializeRules() {
        List<Rule> rules = new ArrayList<Rule>();

        rules.add(new Rule.Builder(
                "com.nuance.framework.cvp.session.ApplicationSessionManager")
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("updateSessionObject")
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
