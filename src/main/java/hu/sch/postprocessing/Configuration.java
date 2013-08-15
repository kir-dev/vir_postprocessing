package hu.sch.postprocessing;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.security.AMSecurityPropertiesException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author aldaris
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Configuration {

    private static final String PAP_SVC_NAME = "VirAuthPAPService";
    private static final String USER_ORG_DN = "Organization";
    private static final String JNDI_NAME = "sunAMVirauthPAPServiceJndiName";
    private static Debug debug = Debug.getInstance("PAPConfigReader");

    public static String getJndiName(SSOToken token) throws Exception {
        String cookieName = null;
        String orgDN = token.getProperty(USER_ORG_DN);

        if (debug.messageEnabled()) {
            debug.message("user orgdn is " + orgDN);
        }

        if (orgDN == null || orgDN.isEmpty()) {
            throw new Exception("User has no organization property in their session");
        }

        Map attrs = getAMRealmConfig(PAP_SVC_NAME, orgDN);
        cookieName = getConfigValueFromMap(attrs, JNDI_NAME);

        if (cookieName == null || cookieName.isEmpty()) {
            throw new Exception("No cookie name found in config");
        }

        return cookieName;
    }

    public static Map<String, Set> getAMRealmConfig(String serviceName, String orgDN)
            throws Exception {
        Map serviceAttrMap = new HashMap();

        try {
            SSOToken token = getPrivilegedSSOToken();

            if ((SSOTokenManager.getInstance().isValidToken(token))) {
                serviceAttrMap = getAMServiceAttributes(token, serviceName, orgDN,
                        "1.0");
            }
            if (debug.messageEnabled()) {
                debug.message("Retrieved the AM Service Attribute List :" + serviceName);
            }
        } catch (Exception ex) {
            if (debug.errorEnabled()) {
                debug.error("Error loading realm attrs for svc " + serviceName, ex);
            }
            throw ex;
        }

        return serviceAttrMap;
    }

    private static Map getAMServiceAttributes(SSOToken token, String serviceName, String orgDN, String version)
            throws Exception {
        Map serviceAttrMap = null;

        try {
            if (token != null && serviceName != null && version != null) {
                ServiceConfigManager scm = new ServiceConfigManager(token,
                        serviceName,
                        version);
                ServiceConfig sc = scm.getOrganizationConfig(orgDN,
                        null);
                serviceAttrMap = sc.getAttributes();
            } else {
                if (debug.errorEnabled()) {
                    debug.error("Error retrieving AM Service Attributes, Attributes null or Invalid Token.");
                }
                throw new Exception("Error retrieving AM Service, Attributes null or Invalid Token.");
            }
        } catch (SMSException smse) {
            if (debug.errorEnabled()) {
                debug.error("SMS API Exception ", smse);
            }
            throw smse;
        } catch (SSOException ssoe) {
            if (debug.errorEnabled()) {
                debug.error("SSO API Exception during SMS API calls", ssoe);
            }
            throw ssoe;
        }

        return serviceAttrMap;
    }

    /**
     * This method authenticates the Admin and if successful returns a
     *  Priviledge SSO Token
     *
     * @throws SSOException
     * @return SSO Token
     */
    private static SSOToken getPrivilegedSSOToken() throws SSOException {
        SSOToken token = null;

        try {
            token = (SSOToken) AccessController.doPrivileged(
                    com.sun.identity.security.AdminTokenAction.getInstance());
        } catch (AMSecurityPropertiesException aspe) {
            if (debug.errorEnabled()) {
                debug.error("SSO Token not found" + aspe);
            }

            throw new SSOException("Unable to fetch token: " + aspe.getMessage());
        }

        if (debug.messageEnabled()) {
            debug.message("Successfully receieved Priviledged SSO Token : " + token);
        }

        return token;
    }

    /**
     * Thie method returns is bascially used to read the AM service Map to
     *  return the value for a particular key
     * @param serviceAttrMap HashMap
     * @param key Key for which value needs to be retrieved
     * @return String : value
     * @throws Exception
     */
    public List getConfigValueSetFromMap(Map<String, Set> serviceAttrMap, String key)
            throws Exception {
        List value = new ArrayList();

        if (!serviceAttrMap.isEmpty()) {
            if (serviceAttrMap.get(key) != null) {
                Iterator<String> itr = serviceAttrMap.get(key).iterator();

                while (itr.hasNext()) {
                    value.add(itr.next());
                }
            } else {
                if (debug.messageEnabled()) {
                    debug.message("Key " + key + " not found, returning empty list");
                }

                return Collections.EMPTY_LIST;
            }
        }
        return value;
    }

    public static String getConfigValueFromMap(Map<String, Set> serviceAttrMap, String key)
            throws Exception {
        String value = "";

        if (!serviceAttrMap.isEmpty()) {
            if (serviceAttrMap.get(key) != null) {
                Iterator<String> itr = serviceAttrMap.get(key).iterator();
                value = itr.next();
            } else {
                if (debug.warningEnabled()) {
                    debug.warning("No value for key " + key + " found in map!");
                }
                return null;
            }
        }

        return value;
    }
}
