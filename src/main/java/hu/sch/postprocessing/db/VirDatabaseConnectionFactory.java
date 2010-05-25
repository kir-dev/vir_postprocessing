package hu.sch.postprocessing.db;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;
import hu.sch.postprocessing.Configuration;
import java.sql.Connection;
import java.sql.DriverManager;
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
    private Debug debug = Debug.getInstance("PostProcess");
    private String jndi;

    private VirDatabaseConnectionFactory(String jndiName) {
        jndi = jndiName;
    }

    public static synchronized VirDatabaseConnectionFactory getInstance(String jndiName) {
        if (INSTANCE == null) {
            INSTANCE = new VirDatabaseConnectionFactory(jndiName);
        }
        return INSTANCE;
    }

    public Connection getConnection() throws AuthLoginException {
        if (jndi != null && !jndi.isEmpty()) {
            try {
                Context initctx = new InitialContext();
                DataSource ds = (DataSource) initctx.lookup(jndi);
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
