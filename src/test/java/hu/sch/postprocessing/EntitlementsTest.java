/**
 * Copyright (c) 2008-2010, Peter Major
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  * Neither the name of the Peter Major nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *  * All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Kir-Dev Team, Hungary
 * and its contributors.
 *
 * THIS SOFTWARE IS PROVIDED BY Peter Major ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Peter Major BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
