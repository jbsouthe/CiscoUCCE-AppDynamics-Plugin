package com.cisco.josouthe.backend;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.ExitCall;
import com.appdynamics.agent.api.ExitTypes;
import com.appdynamics.agent.api.Transaction;
import com.appdynamics.agent.api.impl.NoOpTransaction;
import com.appdynamics.apm.appagent.api.DataScope;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.SDKStringMatchType;
import com.appdynamics.instrumentation.sdk.template.AGenericInterceptor;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.IReflector;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.ReflectorException;
import com.cisco.josouthe.MetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SIPBackendInterceptor  extends AGenericInterceptor {
    /*
    sipSendTo	com.dynamicsoft.DsLibs.DsSipLlApi.DsSipTcpConnection	sendTo	sipRequest_sipHost	POSITION_GATHERER_TYPE	3	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	m_sipRequest.m_URI.m_strHost
    sipSendTo	com.dynamicsoft.DsLibs.DsSipLlApi.DsSipTcpConnection	sendTo	sipRequest_sipUser	POSITION_GATHERER_TYPE	3	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	m_sipRequest.m_URI.m_strUser
    sipSendTo	com.dynamicsoft.DsLibs.DsSipLlApi.DsSipTcpConnection	sendTo	sipResponse_StatusCodeStr	POSITION_GATHERER_TYPE	3	GETTER_METHODS_OBJECT_DATA_TRANSFORMER_TYPE	m_sipResponse.m_StatusCode.toString()
     */

    IReflector sipRequest_sipHost, sipRequest_sipUser, sipResponse_StatusCodeStr;

    private HashSet<DataScope> dataScopesAll;

    public SIPBackendInterceptor() {
        super();

        dataScopesAll = new HashSet<DataScope>() {{
            add(DataScope.SNAPSHOTS);
            add(DataScope.ANALYTICS);
        }};

        sipRequest_sipHost = getNewReflectionBuilder().accessFieldValue("m_sipRequest", true).accessFieldValue("m_URI", true).accessFieldValue("m_strHost", true).build(); //String?
        sipRequest_sipUser = getNewReflectionBuilder().accessFieldValue("m_sipRequest", true).accessFieldValue("m_URI", true).accessFieldValue("m_strUser", true).build(); //String?
        sipResponse_StatusCodeStr = getNewReflectionBuilder().accessFieldValue("m_sipResponse", true).accessFieldValue("m_StatusCode", true).invokeInstanceMethod("toString", true).build(); //String

        getLogger().info(String.format("Initialized plugin version %s, build date %s, contact gecos: %s", MetaData.VERSION, MetaData.BUILDTIMESTAMP, MetaData.GECOS));
    }

    @Override
    public Object onMethodBegin(Object objectIntercepted, String className, String methodName, Object[] parameters) {
        Object contextObject = parameters[3];
        Transaction transaction = AppdynamicsAgent.getTransaction();
        if( transaction instanceof NoOpTransaction ) return null;
        transaction.collectData("sipRequest_sipHost", getReflectiveString(contextObject, sipRequest_sipHost, "UNKNOWN-HOST"), dataScopesAll);
        transaction.collectData("sipRequest_sipUser", getReflectiveString(contextObject, sipRequest_sipUser, "UNKNOWN-USER"), dataScopesAll);
        transaction.collectData("sipResponse_StatusCodeStr", getReflectiveString(contextObject, sipResponse_StatusCodeStr, "UNKNOWN-STATUS"), dataScopesAll);
        Map<String,String> map = new HashMap<>();
        map.put("Address", String.valueOf(parameters[1]));
        ExitCall exitCall = transaction.startExitCall(map, "SIP", ExitTypes.CUSTOM, false);
        //we should really be using this exit call correlation header to embed for correlation
        return exitCall;
    }

    @Override
    public void onMethodEnd(Object state, Object objectIntercepted, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        ExitCall exitCall = (ExitCall) state;
        if( exitCall == null ) return;
        if( exception != null ) {
            AppdynamicsAgent.getTransaction().markAsError("Error in SIP Exitcall: "+ exception.getMessage());
        }
        exitCall.end();
    }

    @Override
    public List<Rule> initializeRules() {
        ArrayList<Rule> rules = new ArrayList<>();
        //Captures the SIP send to for a given destination
        rules.add(new Rule.Builder("com.dynamicsoft.DsLibs.DsSipLlApi.DsSipTcpConnection")
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("sendTo")
                .methodStringMatchType(SDKStringMatchType.EQUALS)
                .build());
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
