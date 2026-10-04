import java.util.Arrays;
import javax.net.ssl.SSLContext;

/** Checks Forge's actual profile parser without starting its GUI or installing anything. */
public class ForgeRuntimeCheck {
    public static void main(String[] args) throws Exception {
        for (String module : new String[] {"jdk.unsupported", "jdk.zipfs", "java.logging", "java.naming"}) {
            if (ModuleLayer.boot().findModule(module).isEmpty()) {
                throw new IllegalStateException("Forge runtime is missing module " + module);
            }
        }
        Class.forName("sun.misc.Unsafe");
        if (Arrays.stream(SSLContext.getDefault().getSupportedSSLParameters().getCipherSuites())
                .noneMatch(cipher -> cipher.startsWith("TLS_ECDHE_RSA_"))) {
            throw new IllegalStateException("Forge runtime is missing elliptic-curve TLS support.");
        }
        Object profile = Class.forName("net.minecraftforge.installer.json.Util")
                .getMethod("loadInstallProfile").invoke(null);
        if (profile == null) throw new IllegalStateException("Forge install profile could not be parsed.");
        System.out.println("Forge runtime check passed: install profile parsed, Unsafe and TLS available.");
    }
}
