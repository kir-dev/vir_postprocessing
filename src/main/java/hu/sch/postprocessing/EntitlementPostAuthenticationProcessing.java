package hu.sch.postprocessing;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthenticationException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EntitlementPostAuthenticationProcessing extends AbstractPostAuthenticationProcessing {

    private static final String GET_USER_DATA_STMT =
            "SELECT usr_id, usr_email, usr_neptun, usr_firstname, usr_lastname, "
            + "usr_nickname, usr_screen_name, usr_dormitory, usr_room, usr_student_status "
            + "FROM users WHERE usr_screen_name = ?";
    //
    private static final String MEMBERSHIP_STMT =
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
    //
    private static final String ENTITLEMENT_SEPARATOR = "|";
    private static final String ENTITLEMENT_PREFIX =
            "urn:geant:niif.hu:sch.bme.hu:entitlement:";
    //
    private static final String VIRID_PREFIX =
            "urn:mace:terena.org:schac:personalUniqueID:hu:BME-SCH-VIR:person:";
    //
    private static final String URN_SEPARATOR = ":";
    //
    private static final String PRINCIPAL_NAME_PREFIX = "id=";
    private static final String PRINCIPAL_NAME_POSTFIX =
            ",ou=user,o=stud,ou=services,dc=opensso,dc=java,dc=net";
    //
    private static final String STUDENT_STATUS_PREFIX =
            "urn:mace:terena.org:schac:status:sch.hu:student_status:";

    @Override
    public void onLoginSuccess(final Map requestMap, final HttpServletRequest request,
            final HttpServletResponse response, final SSOToken ssoToken)
            throws AuthenticationException {

        debug.message("ProfilePostAuthenticationProcessing.onLoginSuccess()");
        try (Connection connection = getConnection(ssoToken)) {
            final String uid = ssoToken.getPrincipal().getName()
                    .replace(PRINCIPAL_NAME_POSTFIX, "")
                    .replace(PRINCIPAL_NAME_PREFIX, "");

            debug.message("uid=" + uid);

            if (uid == null || uid.isEmpty()) {
                debug.error("Cannot get uid from session, returning");
                return;
            }

            setProfileInSession(connection, ssoToken, uid);

        } catch (Exception ex) {
            debug.warning("Exception in ProfilePostAuthenticationProcessing: ", ex);
        }
    }

    private void setProfileInSession(final Connection connection, final SSOToken ssoToken,
            final String uid) throws SQLException, SSOException {

        long virid = 0;
        try (PreparedStatement stmt = connection.prepareStatement(GET_USER_DATA_STMT)) {

            stmt.setString(1, uid);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    virid = rs.getLong("usr_id");

                    if (virid <= 0) {
                        debug.error("invalid usr_id=" + virid);
                    }

                    ssoToken.setProperty("am.protected.uid", uid);

                    fillUserProfile(ssoToken, rs, virid);
                    fillMemberships(connection, virid, ssoToken);
                }
            }
        }

    }

    private void fillUserProfile(final SSOToken ssoToken, final ResultSet userIdResult,
            final long virid) throws SQLException, SSOException {

        final String mail = userIdResult.getString("usr_email");
        final String studentStatus = userIdResult.getString("usr_student_status");
        final String firstName = userIdResult.getString("usr_firstname");
        final String lastName = userIdResult.getString("usr_lastname");
        final String nickname = userIdResult.getString("usr_nickname");
        final String dormitory = userIdResult.getString("usr_dormitory");
        final String room = userIdResult.getString("usr_room");
        final String commonName = userIdResult.getString("usr_lastname")
                + " " + userIdResult.getString("usr_firstname");

        if (debug.messageEnabled()) {
            final StringBuilder msg = new StringBuilder();
            msg.append("virid=").append(virid).append("\n");
            msg.append("mail=").append(mail).append("\n");
            msg.append("student_status=").append(studentStatus).append("\n");
            msg.append("firstname=").append(firstName).append("\n");
            msg.append("lastname=").append(lastName).append("\n");
            msg.append("common name=").append(commonName).append("\n");
            msg.append("nickname=").append(nickname).append("\n");
            msg.append("dormitory=").append(dormitory).append("\n");
            msg.append("room=").append(room).append("\n");

            debug.message(msg.toString());
        }

        ssoToken.setProperty("am.protected.schacPersonalUniqueId",
                VIRID_PREFIX + virid);

        ssoToken.setProperty("am.protected.schacUserStatus",
                STUDENT_STATUS_PREFIX + studentStatus.toLowerCase());

        if (mail != null) {
            ssoToken.setProperty("am.protected.mail", mail);
        }

        if (firstName != null) {
            ssoToken.setProperty("am.protected.givenName", firstName);
        }

        if (lastName != null) {
            ssoToken.setProperty("am.protected.sn", lastName);
        }

        ssoToken.setProperty("am.protected.cn", commonName);

        if (nickname != null) {
            ssoToken.setProperty("am.protected.displayName", nickname);
        }

        if (dormitory != null && room != null) {
            ssoToken.setProperty("am.protected.roomNumber", dormitory + " " + room);
        }

//        debug.message("neptun=" + userIdResult.getString("usr_neptun"));
//        ssoToken.setProperty("am.protected.schacPersonalUniqueCode",
//                userIdResult.getString("usr_neptun"));
    }

    protected void mapToEntitlement(StringBuilder sb, int groupId, String groupName,
            String entitlementType) {

        sb.append(ENTITLEMENT_PREFIX);
        sb.append(entitlementType);
        sb.append(URN_SEPARATOR);
        sb.append(groupName);
        sb.append(URN_SEPARATOR);
        sb.append(groupId);
    }

    private StringBuilder getEntitlementString(final Connection connection, final Long virid)
            throws SQLException {

        final StringBuilder entitlementStr;

        try (PreparedStatement stmt = connection.prepareStatement(MEMBERSHIP_STMT)) {

            stmt.setLong(1, virid);
            stmt.setLong(2, virid);
            try (ResultSet rs = stmt.executeQuery()) {

                entitlementStr = new StringBuilder(400);
                while (rs.next()) {
                    //az első elem elé nem kell szeparátor
                    if (!rs.isFirst()) {
                        entitlementStr.append(ENTITLEMENT_SEPARATOR);
                    }

                    final String groupName = rs.getString("grp_name");
                    final int groupId = rs.getInt("grp_id");
                    final String post = rs.getString("pttip_name");
                    mapToEntitlement(entitlementStr, groupId, groupName, post);

                    if (debug.messageEnabled()) {
                        debug.message("Entitlement in group: " + groupName
                                + ", post: " + post);
                    }
                }
            }
        }

        return entitlementStr;
    }

    private void fillMemberships(final Connection connection, long virid,
            final SSOToken ssoToken) throws SQLException, SSOException {

        final String entitlementStr =
                getEntitlementString(connection, virid).toString();

        ssoToken.setProperty("eduPersonEntitlement", entitlementStr);
        ssoToken.setProperty("am.protected.eduPersonEntitlement", entitlementStr);

        if (debug.messageEnabled()) {
            debug.message("Entitlement attribute was set to: " + entitlementStr);
        }
    }
}
