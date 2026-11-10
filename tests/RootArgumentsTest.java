import app.javo.security.RootArguments;
import java.nio.charset.StandardCharsets;

public final class RootArgumentsTest {
    public static void main(String[] args) throws Exception {
        String shell = args.length == 0 ? "sh" : args[0];
        String metadata = "1.2 \"$(printf unexpected)\" `printf unexpected` 'quoted'\nnext";
        String command = "version=" + RootArguments.shellLiteral(metadata) + "\nprintf '%s' \"$version\"";
        Process shellProcess = new ProcessBuilder(shell).start();
        try (var input = shellProcess.getOutputStream()) {
            input.write(command.getBytes(StandardCharsets.UTF_8));
        }
        String actual = new String(shellProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (shellProcess.waitFor() != 0 || !metadata.equals(actual)) {
            throw new AssertionError("APK version metadata was interpreted by the shell");
        }
        if (!RootArguments.propertyValue("name\r\nid=override").equals("name  id=override")) {
            throw new AssertionError("A module property can span more than one line");
        }
        if (!RootArguments.packageName("app.javo.test_2").equals("app.javo.test_2")) {
            throw new AssertionError("A valid Android package name was changed");
        }
        for (String invalid : new String[]{"../unsafe", "app.bad;echo", "app..invalid", "app.bad/new"}) {
            try {
                RootArguments.packageName(invalid);
                throw new AssertionError("Unsafe package name accepted");
            } catch (IllegalArgumentException expected) {
                // Reject before building a filesystem path or shell command.
            }
        }
        System.out.println("Root installer argument checks passed");
    }
}
