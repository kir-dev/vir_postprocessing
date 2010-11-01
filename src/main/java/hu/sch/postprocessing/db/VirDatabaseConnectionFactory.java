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
