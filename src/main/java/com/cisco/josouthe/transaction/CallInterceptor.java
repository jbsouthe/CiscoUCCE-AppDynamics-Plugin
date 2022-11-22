package com.cisco.josouthe.transaction;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.EntryTypes;
import com.appdynamics.agent.api.Transaction;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.SDKStringMatchType;
import com.appdynamics.instrumentation.sdk.template.AGenericInterceptor;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.IReflector;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.ReflectorException;
import com.cisco.josouthe.MetaData;

import java.util.ArrayList;
import java.util.List;

public class CallInterceptor extends AGenericInterceptor {
    private final String NEW_CALL_CLASS="com.cisco.jasmin.impl.ds.NewInvitationCallback";
    private final String JASMIN_ANSWER_CLASS="com.cisco.jasmin.impl.ds.ConnectionImpl";
    private final String INVOKEVBSESSION_CLASS="com.cisco.voicebrowser.VoiceBrowser";
    private final String SIP_ACK_CLASS="com.dynamicsoft.DsLibs.DsSipLlApi.DsSipClientTransactionIImpl";
    private final String DIAGCHANNELCREATE_CLASS="com.cisco.dialog.DialogChannel";
    private final String GETVXMLDOC_CLASS="com.cisco.voicebrowser.VxmlDocument";

    IReflector getMethod;

    public CallInterceptor() {
        super();
        getLogger().info(String.format("Initialized plugin version %s, build date %s, contact gecos: %s", MetaData.VERSION, MetaData.BUILDTIMESTAMP, MetaData.GECOS));

        getMethod = getNewReflectionBuilder().invokeInstanceMethod("getMethod", true).build();
    }

    @Override
    public Object onMethodBegin(Object objectIntercepted, String className, String methodName, Object[] parameters) {
        String btName="PlaceHolder-BT";
        switch (className) {
            case NEW_CALL_CLASS: { btName="New Call"; break; }
            case JASMIN_ANSWER_CLASS: { btName="Jasmin Answer"; break; }
            case INVOKEVBSESSION_CLASS: { btName="invokeVBSession"; break; }
            case SIP_ACK_CLASS: { btName="Sip ACK"; break; }
            case DIAGCHANNELCREATE_CLASS: { btName="DialogChannelCreate"; break; }
            case GETVXMLDOC_CLASS: { btName="getVxmlDocumentFromDomCache."+ getMethod(parameters[1]); break; }
            default: { getLogger().info(String.format("Unknown class action? for %s.%s(), please review the plugin for what may be missing", className, methodName) ); }
        }
        Transaction transaction = AppdynamicsAgent.startTransaction(btName, getCorrelationHeader(objectIntercepted), EntryTypes.POJO, false);
        return transaction;
    }

    private String getMethod( Object parameter ) {
        try {
            return (String) getMethod.execute(parameter.getClass().getClassLoader(), parameter);
        } catch (ReflectorException e) {
            getLogger().info(String.format("Error attempting to run %s.getMethod(), exception: %s",parameter,e),e);
            return "UNKNOWN";
        }
    }
    private String getCorrelationHeader( Object object ) {
        return null;
    }

    @Override
    public void onMethodEnd(Object state, Object objectIntercepted, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        Transaction transaction = (Transaction) state;
        if( transaction == null ) return;

        if( exception != null )
            transaction.markAsError(exception.getMessage());
        transaction.end();
    }

    @Override
    public List<Rule> initializeRules() {
        ArrayList<Rule> rules = new ArrayList<>();
        //Captures the request for a new call to the VVB server that includes the SIP invite coming to VVB
        rules.add(new Rule.Builder(NEW_CALL_CLASS)
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("invitation")
                .methodStringMatchType(SDKStringMatchType.EQUALS)
                .build());

        //VVB answering the invite with 200 OK
        rules.add(new Rule.Builder(JASMIN_ANSWER_CLASS)
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("answer")
                .methodStringMatchType(SDKStringMatchType.EQUALS)
                .build());

        //Captures the call session for any given call
        rules.add(new Rule.Builder(INVOKEVBSESSION_CLASS)
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("invokeVBSession")
                .methodStringMatchType(SDKStringMatchType.EQUALS)
                .build());

        //Captures the acknowledgement of any SIP communication from the VVB server
        rules.add(new Rule.Builder(SIP_ACK_CLASS)
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("ack")
                .methodStringMatchType(SDKStringMatchType.EQUALS)
                .build());

        //Captures the dialog channel created for a given call
        rules.add(new Rule.Builder(DIAGCHANNELCREATE_CLASS)
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("create")
                .methodStringMatchType(SDKStringMatchType.EQUALS)
                .build());

        //Captures the acknowledgement of any SIP communication from the VVB server
        rules.add(new Rule.Builder(GETVXMLDOC_CLASS)
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("getVxmlDocumentFromDomCache")
                .methodStringMatchType(SDKStringMatchType.EQUALS)
                .build());

        return rules;
    }
}
