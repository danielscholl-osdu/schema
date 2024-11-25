package org.opengroup.osdu.schema.util;

import com.google.common.base.Strings;
import org.opengroup.osdu.schema.util.config.OpenIDTokenProvider;

public class TestAuthUtils extends AuthUtil {

    public static final String INTEGRATION_TESTER_TOKEN = "PRIVILEGED_USER_TOKEN";
    private OpenIDTokenProvider openIDTokenProvider;

    public TestAuthUtils() {
        token = System.getProperty(INTEGRATION_TESTER_TOKEN, System.getenv(INTEGRATION_TESTER_TOKEN));

        if (Strings.isNullOrEmpty(token)) {
            openIDTokenProvider = new OpenIDTokenProvider();
        }
    }

    @Override
    public synchronized String getToken() throws Exception {
        if (Strings.isNullOrEmpty(token)) {
            token = openIDTokenProvider.getToken();
        }
        return "Bearer " + token;
    }
}
