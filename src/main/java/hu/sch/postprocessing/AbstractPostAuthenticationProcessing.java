package hu.sch.postprocessing;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.shared.debug.Debug;
import hu.sch.postprocessing.db.VirDatabaseConnectionFactory;
import java.sql.Connection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author aldaris
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class AbstractPostAuthenticationProcessing implements AMPostAuthProcessInterface {

    private static Debug debug = Debug.getInstance("PostProcess");

    @Override
    public abstract void onLoginSuccess(Map map, HttpServletRequest hsr, HttpServletResponse hsr1, SSOToken ssot)
            throws AuthenticationException;

    @Override
    public void onLoginFailure(Map map, HttpServletRequest hsr, HttpServletResponse hsr1)
            throws AuthenticationException {
    }

    @Override
    public void onLogout(HttpServletRequest hsr, HttpServletResponse hsr1, SSOToken ssot)
            throws AuthenticationException {
    }

    public Connection getConnection(SSOToken token) throws Exception {
        String jndiName = null;
        jndiName = Configuration.getJndiName(token);
        return VirDatabaseConnectionFactory.getInstance(jndiName).getConnection();
    }
}
