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
import com.sun.identity.authentication.spi.AuthenticationException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings({"unchecked", "rawtypes"})
public class EntitlementPostAuthenticationProcessing extends AbstractPostAuthenticationProcessing {

    private static final String MEMBERSHIP_STATEMENT =
            "SELECT grp_membership.grp_id, groups.grp_name, poszttipus.pttip_name "
            + "FROM grp_membership JOIN groups USING (grp_id) "
            + "JOIN poszt ON poszt.grp_member_id = grp_membership.id "
            + "JOIN poszttipus ON poszt.pttip_id = poszttipus.pttip_id "
            + "WHERE (grp_membership.usr_id=? AND membership_end is null) "
            + "UNION "
            + "(SELECT grp_membership.grp_id, groups.grp_name, 'tag' AS pttip_name "
            + "FROM grp_membership JOIN groups USING (grp_id) "
            + "LEFT OUTER JOIN poszt ON poszt.grp_member_id = grp_membership.id "
            + "WHERE (poszt.pttip_id <> 6 OR poszt.pttip_id IS null) AND " //feldolgozás alattiak ne kapjanak tag jogot
            + "usr_id = ? AND grp_membership.membership_end IS null) "
            + "ORDER BY grp_id";
    private static final String ENTITLEMENT_ATTRIBUTE = "eduPersonEntitlement";
    private static final String ENTITLEMENT_PREFIX = "urn:geant:niif.hu:sch.bme.hu:entitlement:";
    private static final String ENTITLEMENT_SEPARATOR = "|";
    private static final String SESSION_ENTITLEMENT_ATTRIBUTE = "am.protected.eduPersonEntitlement";
    private static final String URN_SEPARATOR = ":";

    @Override
    public void onLoginSuccess(Map requestMap, HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken)
            throws AuthenticationException {
        debug.message("EntitlementPostAuthenticationProcessing.onLoginSuccess()");
        Connection connection = null;
        try {
            //előszedem a virid attribútumot a sessionből
            String viridprop = ssoToken.getProperty("am.protected.schacPersonalUniqueId");
            if (viridprop == null) {
                debug.message("Cannot get VIRID from session, returning");
                return;
            }
            debug.message("Got VIRID: " + viridprop);
            String[] viridarr = viridprop.split(":");
            //a virid ennek az utolsó szám tagja
            long virid = Long.parseLong(viridarr[viridarr.length - 1]);

            connection = getConnection(ssoToken);
            PreparedStatement stmt = connection.prepareStatement(MEMBERSHIP_STATEMENT);
            stmt.setLong(1, virid);
            stmt.setLong(2, virid);
            ResultSet rs = stmt.executeQuery();

            //itt fog összeálni a nagy entitlement string, elemenként elválasztva
            StringBuilder entitlementStr = new StringBuilder(400);
            boolean first = true;
            while (rs.next()) {
                //az első elem elé nem kell szeparátor
                if (!first) {
                    entitlementStr.append(ENTITLEMENT_SEPARATOR);
                } else {
                    first = false;
                }
                String groupName = rs.getString("grp_name");
                int groupId = rs.getInt("grp_id");
                String post = rs.getString("pttip_name");
                debug.message("Entitlement in group: " + groupName + ", post: " + post);
                mapToEntitlement(entitlementStr, groupId, groupName, post);
            }

            ssoToken.setProperty(ENTITLEMENT_ATTRIBUTE, entitlementStr.toString());
            ssoToken.setProperty(SESSION_ENTITLEMENT_ATTRIBUTE, entitlementStr.toString());
            if (debug.messageEnabled()) {
                debug.message("Entitlement attribute was set to: "
                        + entitlementStr.toString());
            }
        } catch (Exception ex) {
            debug.warning("Exception in EntitlementPostAuthenticationProcessing: ", ex);
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ex) {
                debug.warning("Cannot close database connection", ex);
            }
        }
    }

    protected void mapToEntitlement(StringBuilder sb, int groupId, String groupName, String entitlementType) {
        sb.append(ENTITLEMENT_PREFIX);
        sb.append(entitlementType);
        sb.append(URN_SEPARATOR);
        sb.append(groupName);
        sb.append(URN_SEPARATOR);
        sb.append(groupId);
    }
}
