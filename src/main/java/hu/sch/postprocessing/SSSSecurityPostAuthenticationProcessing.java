package hu.sch.postprocessing;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.shared.debug.Debug;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Refactored by: aldaris
 * @author hege
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class SSSSecurityPostAuthenticationProcessing extends AbstractPostAuthenticationProcessing {

    private static final String SAVE_TOKEN_STMT =
            "UPDATE users SET usr_sss_token = ?, "
            + "usr_sss_token_logintime = now() "
            + "WHERE usr_id = ?;";
    private static final String TOKEN_ATTR = "SSSToken";
    private static Debug debug = Debug.getInstance("PostProcess");

    /**
     * Létrehoz a users táblában egy sss_tokent a belépett felhasználóhoz.
     *
     * @param requestMap
     * @param request
     * @param response
     * @param ssoToken
     * @throws AuthenticationException
     */
    @Override
    public void onLoginSuccess(Map requestMap, HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken)
            throws AuthenticationException {

        Connection connection = null;
        try {
            //vir-id előkeresése az sso sessionből
            String viridprop =
                    ssoToken.getProperty("am.protected.schacPersonalUniqueId");
            if (viridprop == null) {
                //ha nincs neki ilyen, akkor békén hagyjuk
                return;
            }
            String[] viridarr = viridprop.split(":");
            //a virid ennek az utolsó szám tagja
            long virid = Long.parseLong(viridarr[viridarr.length - 1]);

            //adatbázis kapcsolat kérése
            connection = getConnection(ssoToken);

            //tokent elmentő lekérdezés
            PreparedStatement saveToken = connection.prepareStatement(SAVE_TOKEN_STMT);
            String tokenString = randomString();
            saveToken.setString(1, tokenString);
            saveToken.setLong(2, virid);

            //token mentése
            saveToken.executeUpdate();
            ssoToken.setProperty(TOKEN_ATTR, tokenString);
        } catch (Exception ex) {
            debug.warning("Exception in SSSSecurityPostAuthenticationProcessing: ", ex);
        } finally {
            try {
                //adatbázis kapcsolat visszaadása
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ex) {
                debug.warning("Cannot close database connection", ex);
            }
        }
    }

    /**
     * Visszaad egy pszeudo-random stringet.
     *
     * @return
     * @throws AuthenticationException
     */
    private String randomString() throws AuthenticationException {
        return UUID.randomUUID().toString();
    }
}
