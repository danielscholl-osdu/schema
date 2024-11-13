package org.opengroup.osdu.schema.util;

public abstract class AuthUtil {

    public static String token = null;

    public abstract String getToken() throws Exception;
}
