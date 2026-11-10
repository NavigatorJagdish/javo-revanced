package app.javo.security;

import java.util.regex.Pattern;

/** Encodes APK metadata for the two formats used by the root installer. */
public final class RootArguments {
    private static final Pattern PACKAGE_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)*");
    private static final Pattern PROPERTY_CONTROLS = Pattern.compile("[\\p{Cc}\\p{Zl}\\p{Zp}]");

    private RootArguments() {}

    public static String packageName(String value) {
        if (!PACKAGE_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid Android package name");
        }
        return value;
    }

    public static String shellLiteral(String value) {
        if (value.indexOf(0) >= 0) {
            throw new IllegalArgumentException("NUL is invalid in shell arguments");
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    public static String propertyValue(String value) {
        return PROPERTY_CONTROLS.matcher(value).replaceAll(" ");
    }
}
