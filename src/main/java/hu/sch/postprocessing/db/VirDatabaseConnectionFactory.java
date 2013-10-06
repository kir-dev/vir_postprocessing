package hu.sch.postprocessing.db;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;
import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 *
 * @author aldaris
 */
public class VirDatabaseConnectionFactory {

    private static volatile VirDatabaseConnectionFactory INSTANCE = null;
    private final Debug debug = Debug.getInstance("PostProcess");
    private final String jndi;

    private VirDatabaseConnectionFactory(final String jndiName) {
        jndi = jndiName;
    }

    public static synchronized VirDatabaseConnectionFactory getInstance(final String jndiName) {
        if (INSTANCE == null) {
            INSTANCE = new VirDatabaseConnectionFactory(jndiName);
        }
        return INSTANCE;
    }

    public Connection getConnection() throws AuthLoginException {
        if (jndi != null && !jndi.isEmpty()) {
            try {
                final Context initctx = new InitialContext();
                final DataSource ds = (DataSource) initctx.lookup(jndi);
                debug.message("Datasource Acquired: " + ds.toString());
                return ds.getConnection();
            } catch (NamingException ex) {
                debug.error("Unable to look up JNDI datasource", ex);
                throw new AuthLoginException("vipAuthVir", "virError", null);
            } catch (SQLException ex) {
                debug.error("SQL Exception while retrieving connection", ex);
                throw new AuthLoginException("vipAuthVir", "virError", null);
            }
        }
        throw new IllegalStateException("Unable to create database connectionpool");
    }
}
