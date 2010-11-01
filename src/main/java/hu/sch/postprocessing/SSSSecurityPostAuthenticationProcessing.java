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
