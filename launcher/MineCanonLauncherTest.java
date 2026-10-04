import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public class MineCanonLauncherTest {
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("mine-canon-launcher-test-");
        try {
            Path mods = Files.createDirectories(temp.resolve("mods-1.20.1-base"));
            Files.createDirectory(temp.resolve("saves"));
            Path launcher = Files.createDirectory(temp.resolve("launcher"));
            check(MineCanonLauncher.locateRoot(launcher).equals(temp), "Locate root from script directory");
            check(MineCanonLauncher.locateRoot(temp).equals(temp), "Locate root from repository");
            expectFailure(() -> MineCanonLauncher.locateRoot(Path.of(System.getProperty("java.home"))));
            Path packaged = Files.createDirectories(launcher.resolve("dist").resolve("MineCanon").resolve("app"));
            check(MineCanonLauncher.locateRoot(packaged).equals(temp), "Locate repository from packaged app");
            check(MineCanonLauncher.countJars(mods) == 0, "Empty collection");
            Files.write(mods.resolve("mod.jar"), new byte[] {80, 75, 3, 4});
            Path nested = Files.createDirectory(mods.resolve("nested"));
            Files.write(nested.resolve("MOD.JAR"), new byte[] {80, 75, 3, 4});
            Files.writeString(nested.resolve("notes.txt"), "not a mod");
            check(MineCanonLauncher.countJars(mods) == 2, "Recursive case-insensitive JAR count");
            expectFailure(() -> MineCanonLauncher.countJars(temp.resolve("missing")));
            expectFailure(() -> MineCanonLauncher.forgeInstaller(temp));
            Path installer = mods.resolve(MineCanonLauncher.FORGE);
            Files.writeString(installer, "version https://git-lfs.github.com/spec/v1");
            expectFailure(() -> MineCanonLauncher.forgeInstaller(temp));
            Files.write(installer, new byte[] {80, 75, 3, 4});
            check(MineCanonLauncher.forgeInstaller(temp).equals(installer), "Find primary installer");
            Files.delete(installer);
            Path fallback = Files.createDirectory(mods.resolve("custom-jar-forgeui")).resolve(MineCanonLauncher.FORGE);
            Files.write(fallback, new byte[] {80, 75, 3, 4});
            check(MineCanonLauncher.forgeInstaller(temp).equals(fallback), "Find fallback installer");
            check(MineCanonLauncher.icon().getWidth() == 64, "Render icon headlessly");
            expectFailure(() -> MineCanonLauncher.loadArtwork(temp.resolve("missing.png")));
            Path invalid = temp.resolve("invalid.png");
            Files.writeString(invalid, "invalid image");
            expectFailure(() -> MineCanonLauncher.loadArtwork(invalid));
            System.out.println("All launcher checks passed.");
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        if (args.length == 2) {
            SwingUtilities.invokeAndWait(() -> {
                MineCanonLauncher app = null;
                try {
                    app = new MineCanonLauncher(Path.of(args[0]));
                    app.show();
                    app.frame.setSize(1000, 740);
                    check(app.frame.isShowing(), "Desktop window is visible");
                    check(app.navigation.size() == 4, "All four navigation pages exist");
                    for (String page : java.util.List.of("Worlds", "Mods", "Settings", "Home")) {
                        app.select(page);
                        app.frame.validate();
                        check(app.navigation.stream()
                                .filter(button -> button.getText().equals(page))
                                .allMatch(button -> button.getForeground().equals(MineCanonLauncher.ACCENT)),
                                "Navigation highlight: " + page);
                        BufferedImage preview = new BufferedImage(app.frame.getWidth(), app.frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                        var painter = preview.createGraphics();
                        app.frame.paintAll(painter);
                        painter.dispose();
                        ImageIO.write(preview, "png", Path.of(args[1] + "-" + page + ".png").toFile());
                    }
                    BufferedImage image = new BufferedImage(app.frame.getWidth(), app.frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                    var graphics = image.createGraphics();
                    app.frame.paintAll(graphics);
                    graphics.dispose();
                    ImageIO.write(image, "png", Path.of(args[1]).toFile());
                    System.out.println("Desktop smoke check passed.");
                } catch (Exception failure) {
                    throw new RuntimeException(failure);
                } finally {
                    if (app != null) app.frame.dispose();
                }
            });
        }
    }

    interface CheckedAction { void run() throws Exception; }

    static void expectFailure(CheckedAction action) throws Exception {
        try {
            action.run();
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError("Expected an explicit IOException");
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
