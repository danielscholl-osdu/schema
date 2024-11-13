package org.opengroup.osdu.schema.util;

import com.google.common.base.Strings;
import org.opengroup.osdu.schema.util.config.OpenIDTokenProvider;

public class TestAuthUtils extends AuthUtil {

//    private OpenIDTokenProvider openIDTokenProvider;
//
//    public TestAuthUtils() {
//            openIDTokenProvider = new OpenIDTokenProvider();
//    }

    @Override
    public synchronized String getToken() throws Exception {
//        if (Strings.isNullOrEmpty(token)) {
            token = new OpenIDTokenProvider().getToken();
//        }
        return "Bearer " + token;
    }
}
