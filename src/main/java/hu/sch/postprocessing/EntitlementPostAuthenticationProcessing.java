package hu.sch.postprocessing;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.shared.debug.Debug;
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
            + "WHERE usr_id = ? AND grp_membership.membership_end IS null) "
            + "ORDER BY grp_id;";
    private static final String ENTITLEMENT_ATTRIBUTE = "eduPersonEntitlement";
    private static final String ENTITLEMENT_PREFIX = "urn:geant:niif.hu:sch.bme.hu:entitlement:";
    private static final String ENTITLEMENT_SEPARATOR = "|";
    private static final String SESSION_ENTITLEMENT_ATTRIBUTE = "am.protected.eduPersonEntitlement";
    private static final String URN_SEPARATOR = ":";
    private static Debug debug = Debug.getInstance("PostProcess");

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
            while (rs.next()) {
                //az első elem elé nem kell szeparátor
                if (!rs.isFirst()) {
                    entitlementStr.append(ENTITLEMENT_SEPARATOR);
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
                        + ssoToken.getProperty(ENTITLEMENT_ATTRIBUTE));
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
