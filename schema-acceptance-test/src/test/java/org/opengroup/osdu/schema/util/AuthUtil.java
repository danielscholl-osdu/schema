package org.opengroup.osdu.schema.util;

public abstract class AuthUtil {

    protected static String token = null;

    public abstract String getToken() throws Exception;
}
