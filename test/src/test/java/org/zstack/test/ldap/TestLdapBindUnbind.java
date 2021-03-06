package org.zstack.test.ldap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.APILogInReply;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.ldap.*;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class TestLdapBindUnbind {
    CLogger logger = Utils.getLogger(TestLdapBindUnbind.class);

    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig kconfig;
    LdapManager ldapManager;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ldap/TestLdap.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("LdapManagerImpl.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        ldapManager = loader.getComponent(LdapManager.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    private void queryLdapServer() throws ApiSenderException {
        ApiSender sender = api.getApiSender();

        // query ldap server
        APIQueryLdapServerMsg msg12 = new APIQueryLdapServerMsg();
        msg12.setConditions(new ArrayList<QueryCondition>());
        msg12.setSession(session);
        APIQueryLdapServerReply reply12 = sender.call(msg12, APIQueryLdapServerReply.class);
        logger.debug(reply12.getInventories().stream().map(LdapServerInventory::getUrl).collect(Collectors.joining(", ")));
    }

    @Test
    public void test() throws ApiSenderException {
        ApiSender sender = api.getApiSender();

        // add ldap server
        APIAddLdapServerMsg msg13 = new APIAddLdapServerMsg();
        msg13.setName("miao");
        msg13.setDescription("miao desc");
        msg13.setUrl("ldap://172.20.12.176:389");
        msg13.setBase("dc=learnitguide,dc=net");
        msg13.setUsername("cn=Manager,dc=learnitguide,dc=net");
        msg13.setPassword("password");
        msg13.setSession(session);
        APIAddLdapServerEvent evt13 = sender.send(msg13, APIAddLdapServerEvent.class);
        logger.debug(evt13.getInventory().getName());
        queryLdapServer();

        // bind account
        AccountInventory ai1 = api.createAccount("ldapuser1", "hello-kitty");
        APIBindLdapAccountMsg msg2 = new APIBindLdapAccountMsg();
        msg2.setAccountUuid(ai1.getUuid());
        msg2.setLdapUid("ldapuser1");
        msg2.setSession(session);
        APIBindLdapAccountEvent evt2 = sender.send(msg2, APIBindLdapAccountEvent.class);
        logger.debug(evt2.getInventory().getUuid());

        // login account
        APILogInByLdapMsg msg3 = new APILogInByLdapMsg();
        msg3.setUid("ldapuser1");
        msg3.setPassword("redhat");
        msg3.setServiceId(bus.makeLocalServiceId(LdapConstant.SERVICE_ID));
        APILogInReply reply3 = sender.call(msg3, APILogInReply.class);
        logger.debug(reply3.getInventory().getAccountUuid());

        // unbind account
        APIUnbindLdapAccountMsg msg4 = new APIUnbindLdapAccountMsg();
        msg4.setUuid(evt2.getInventory().getUuid());
        msg4.setSession(session);
        APIUnbindLdapAccountEvent evt4 = sender.send(msg4, APIUnbindLdapAccountEvent.class);
        Assert.assertTrue(evt4.getErrorCode() == null);
    }
}
