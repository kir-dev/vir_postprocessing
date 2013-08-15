package hu.sch.postprocessing;

import com.iplanet.sso.SSOToken;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 *
 * @author aldaris
 */
@Test
public class EntitlementsTest {

    private EntitlementPostAuthenticationProcessing processing;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private SSOToken token;

    @BeforeSuite
    protected void setUp() throws Exception {
        processing = new EntitlementPostAuthenticationProcessing() {

            @Override
            public Connection getConnection(SSOToken token) throws Exception {
                Class dc = Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
                Driver driverInstance = (Driver) dc.newInstance();
                return driverInstance.connect("jdbc:derby:target/korok", null);
            }
        };
        req = mock(HttpServletRequest.class);
        resp = mock(HttpServletResponse.class);
        token = mock(SSOToken.class);
    }

    @DataProvider(name = "entitlementsData")
    public String[][] entitlementsData() {
        return new String[][]{
                    {"1", ""},
                    {"2", "urn:geant:niif.hu:sch.bme.hu:entitlement:tag:KIR fejlesztők és üzemeltetők:1"},
                    {"3", ""},
                    {"4", "urn:geant:niif.hu:sch.bme.hu:entitlement:tag:Pizzásch:3|urn:geant:niif.hu:sch.bme.hu:entitlement:körvezető:Pizzásch:3"},
                    {"5", "urn:geant:niif.hu:sch.bme.hu:entitlement:feldolgozás alatt:KIR fejlesztők és üzemeltetők:1"},
                    {"6", "urn:geant:niif.hu:sch.bme.hu:entitlement:tag:La'Place:5|"
                        + "urn:geant:niif.hu:sch.bme.hu:entitlement:gazdaságis:La'Place:5|"
                        + "urn:geant:niif.hu:sch.bme.hu:entitlement:PR menedzser:La'Place:5"},
                    {"7", "urn:geant:niif.hu:sch.bme.hu:entitlement:tag:Pizzásch:3|"
                        + "urn:geant:niif.hu:sch.bme.hu:entitlement:tag:Gofffree:4"},
                    {"8", "urn:geant:niif.hu:sch.bme.hu:entitlement:tag:La'Place:5|"
                        + "urn:geant:niif.hu:sch.bme.hu:entitlement:volt körvezető:SVIE:2|"
                        + "urn:geant:niif.hu:sch.bme.hu:entitlement:tag:SVIE:2"},
                    {"9", "urn:geant:niif.hu:sch.bme.hu:entitlement:tag:SVIE:2|"
                        + "urn:geant:niif.hu:sch.bme.hu:entitlement:körvezető:SVIE:2|"
                        + "urn:geant:niif.hu:sch.bme.hu:entitlement:tag:Gofffree:4"}
                };
    }

    @Test(dataProvider = "entitlementsData")
    public void testEntitlements(final String virid, final String expected) throws Exception {
        when(token.getProperty(eq("am.protected.schacPersonalUniqueId"))).thenReturn(virid);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] arg = invocation.getArguments();
                assertTrue(checkEntitlements(arg[1].toString(), expected));
                return null;
            }
        }).when(token).setProperty(eq("am.protected.eduPersonEntitlement"), anyString());
        processing.onLoginSuccess(new HashMap(), req, resp, token);
    }

    /**
     * Mivel elég random sorrendben lehetnek az entitlementek az order by aktuális
     * viselkedése alapján, így ezzel a függvénnyel ellenőrizzük, hogy minden jog
     * átadódott-e.
     *
     * @param result A PAP-tól eredményül kapott entitlementstring
     * @param expected A helyes érték
     * @return megegyezik-e a két kapott entitlement
     */
    public boolean checkEntitlements(String result, String expected) {
        if (result.equals(expected)) {
            return true;
        }

        if (result.length() == expected.length()) {
            if (new HashSet<String>(Arrays.asList(result.split("\\|"))).equals(
                    new HashSet<String>(Arrays.asList(expected.split("\\|"))))) {
                return true;
            }
        }

        return false;
    }
}
