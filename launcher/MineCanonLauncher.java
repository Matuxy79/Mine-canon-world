import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * A local desktop hub. Minecraft authentication and game startup remain with
 * the official launcher; this app never edits saves, mods, or third-party JARs.
 */
public class MineCanonLauncher {
    static final Color BG = new Color(12, 16, 19);
    static final Color PANEL = new Color(19, 25, 29);
    static final Color TEXT = new Color(248, 242, 230);
    static final Color MUTED = new Color(148, 169, 180);
    static final Color ACCENT = new Color(255, 119, 42);
    static final Color GREEN = new Color(149, 209, 126);
    static final Color LINE = new Color(42, 57, 67);
    static final String FORGE = "forge-1.20.1-47.4.26-installer.jar";
    final Path root;
    final Path preferences;
    final Properties settings = new Properties();
    final JFrame frame = new JFrame("MineCanon | John Matukutire");
    final BufferedImage artwork;
    final JPanel pages = new JPanel(new CardLayout());
    final JLabel status = label("Ready. Your canon awaits.", 12, MUTED);
    final JLabel launcherPath = label("", 12, MUTED);
    final List<JButton> navigation = new java.util.ArrayList<>();
    final List<JButton> actions = new java.util.ArrayList<>();

    MineCanonLauncher(Path root) throws IOException {
        this.root = root;
        artwork = loadArtwork(root.resolve("launcher").resolve("assets").resolve("canon-landscape.png"));
        preferences = root.resolve("launcher").resolve("launcher.properties");
        if (Files.exists(preferences)) {
            try (InputStream input = Files.newInputStream(preferences)) {
                settings.load(input);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String executable = System.getProperty("jpackage.app-path");
                Path start = executable == null ? Path.of(System.getProperty("user.dir"))
                        : Path.of(executable).getParent();
                Path root = locateRoot(start);
                new MineCanonLauncher(root).show();
            } catch (Exception error) {
                error.printStackTrace();
                JOptionPane.showMessageDialog(null, error.getMessage(),
                        "MineCanon could not start", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    static Path locateRoot(Path start) throws IOException {
        Path path = start.toAbsolutePath().normalize();
        for (int depth = 0; path != null && depth < 6; depth++, path = path.getParent()) {
            if (Files.isDirectory(path.resolve("saves"))
                    && Files.isDirectory(path.resolve("mods-1.20.1-base"))) return path;
        }
        throw new IOException("Cannot find the Mine Canon World folders. Run the launch script inside this repository.");
    }

    static BufferedImage loadArtwork(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) throw new IOException("MineCanon artwork is not a supported image: " + path);
        return image;
    }

    static long countJars(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) throw new IOException("Mod folder not found: " + folder);
        try (Stream<Path> paths = Files.walk(folder)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".jar"))
                    .count();
        }
    }

    static Path forgeInstaller(Path root) throws IOException {
        Path mods = root.resolve("mods-1.20.1-base");
        for (Path candidate : List.of(mods.resolve(FORGE), mods.resolve("custom-jar-forgeui").resolve(FORGE))) {
            if (Files.isRegularFile(candidate)) {
                // Git LFS pointers are text, not runnable installers.
                try (InputStream input = Files.newInputStream(candidate)) {
                    if (input.read() != 'P' || input.read() != 'K') {
                        throw new IOException("The Forge installer is not a downloaded JAR. Fetch its Git LFS content first.");
                    }
                }
                return candidate;
            }
        }
        throw new IOException("Forge installer not found. Expected " + FORGE + " in mods-1.20.1-base.");
    }

    void show() throws IOException {
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("Panel.background", PANEL);
        UIManager.put("OptionPane.messageForeground", TEXT);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1280, 850);
        frame.setMinimumSize(new Dimension(1000, 740));
        frame.setLocationRelativeTo(null);
        frame.setIconImage(icon());
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(BG);
        shell.add(sidebar(), BorderLayout.WEST);
        JPanel main = column(BG, 24);
        main.add(header());
        main.add(Box.createVerticalStrut(22));
        pages.setOpaque(false);
        pages.setAlignmentX(Component.LEFT_ALIGNMENT);
        pages.add(home(), "Home");
        pages.add(world(), "Worlds");
        pages.add(mods(), "Mods");
        pages.add(setup(), "Settings");
        main.add(pages);
        main.add(Box.createVerticalStrut(14));
        status.setBorder(new EmptyBorder(8, 0, 0, 0));
        main.add(status);
        shell.add(main, BorderLayout.CENTER);
        frame.setContentPane(shell);
        select("Home");
        frame.setVisible(true);
    }

    JPanel sidebar() {
        JPanel side = column(PANEL, 22);
        side.setPreferredSize(new Dimension(212, 0));
        JLabel monogram = new JLabel(new ImageIcon(icon()));
        monogram.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(monogram);
        side.add(Box.createVerticalStrut(12));
        side.add(serif("MINE CANON", 23));
        side.add(Box.createVerticalStrut(8));
        side.add(label("W O R L D   L A U N C H E R", 10, MUTED));
        side.add(Box.createVerticalStrut(46));
        for (String title : List.of("Home", "Worlds", "Mods", "Settings")) {
            JButton button = button(title, false, () -> select(title));
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            navigation.add(button);
            side.add(button);
            side.add(Box.createVerticalStrut(10));
        }
        side.add(Box.createVerticalGlue());
        side.add(label("CREATED BY", 10, MUTED));
        side.add(Box.createVerticalStrut(8));
        side.add(label("John Matukutire", 16, TEXT));
        side.add(Box.createVerticalStrut(8));
        side.add(label("Many stories. One world.", 11, MUTED));
        side.add(Box.createVerticalStrut(22));
        side.add(label("MINECANON  /  LOCAL INSTANCE", 10, GREEN));
        return side;
    }

    JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        header.add(serif("Your world. Your canon.", 30), BorderLayout.WEST);
        header.add(label("1.20.1  /  FORGE", 12, GREEN), BorderLayout.EAST);
        return header;
    }

    JPanel home() {
        JPanel home = column(BG, 0);
        Landscape hero = new Landscape(artwork);
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.setLayout(new BorderLayout());
        hero.setBorder(new EmptyBorder(28, 30, 26, 30));
        hero.setPreferredSize(new Dimension(800, 380));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 440));
        JPanel copy = column(null, 0);
        copy.add(label("JOHN MATUKUTIRE PRESENTS", 11, TEXT));
        copy.add(Box.createVerticalStrut(22));
        copy.add(serif("MINE", 64));
        copy.add(serif("CANON", 64));
        copy.add(Box.createVerticalStrut(14));
        copy.add(serif("One field. Every age.", 23));
        copy.add(Box.createVerticalGlue());
        copy.add(label("C A N O N   H U N T E R S   &   M O N S T E R S", 12, TEXT));
        hero.add(copy, BorderLayout.CENTER);
        home.add(hero);
        home.add(Box.createVerticalStrut(18));
        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 0));
        cards.setOpaque(false);
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 106));
        cards.add(stat("01 / YOUR WORLD", "Minecraft world", "Your local crossover save"));
        cards.add(stat("02 / GAME VERSION", "1.20.1", "Java Edition target"));
        cards.add(stat("03 / MOD LOADER", "Forge 47.4.26", "Installer available in Settings"));
        home.add(cards);
        home.add(Box.createVerticalStrut(20));
        home.add(label("YOUR LOCAL WORLD  /  MINECRAFT 1.20.1", 11, ACCENT));
        home.add(Box.createVerticalStrut(10));
        home.add(serif("Enter the world. Shape the lore.", 24));
        home.add(Box.createVerticalStrut(14));
        JPanel links = row();
        links.add(action("OPEN MINECRAFT  >", true, this::launchMinecraft));
        links.add(action("Set up Forge", false, this::launchForge));
        links.add(action("Open world folder", false, () -> open(root.resolve("saves").resolve("minecraft world"))));
        home.add(links);
        home.add(Box.createVerticalStrut(12));
        home.add(label("Choose your Mine Canon profile in the official launcher.", 12, MUTED));
        home.add(Box.createVerticalGlue());
        return home;
    }

    JPanel world() {
        JPanel page = page("Your world. Your rules.", "The original Mine Canon World save lives here.");
        page.add(stat("LOCAL SAVE", "minecraft world", "saves\\minecraft world"));
        page.add(Box.createVerticalStrut(20));
        page.add(paragraph("The launcher opens Minecraft, not the save itself. Select a compatible 1.20.1 profile "
                + "in the official launcher, then choose your world in Singleplayer."));
        page.add(Box.createVerticalStrut(18));
        page.add(paragraph("This repository is not automatically your Minecraft game directory. To use this save, "
                + "back it up and copy it into the saves folder of your chosen game directory while Minecraft is closed."));
        page.add(Box.createVerticalStrut(22));
        page.add(action("Open this world folder", true, () -> open(root.resolve("saves").resolve("minecraft world"))));
        page.add(Box.createVerticalStrut(12));
        page.add(action("Open all saves", false, () -> open(root.resolve("saves"))));
        page.add(Box.createVerticalGlue());
        page.add(paragraph("Protect your canon: back up your save before changing versions or mods. "
                + "This hub never copies, overwrites, or edits world files."));
        return page;
    }

    JPanel mods() throws IOException {
        JPanel page = page("Build your crossover.", "Your local collection, not a verified modpack.");
        page.add(stat("LOCAL COLLECTION", countJars(root.resolve("mods-1.20.1-base")) + " JAR files",
                "Includes subfolders, duplicate copies, and installers"));
        page.add(Box.createVerticalStrut(22));
        page.add(paragraph("Compatibility matters. This collection includes mixed loaders and game versions, "
                + "including Forge, Fabric, and server plugins. Do not copy every JAR into a single mods folder."));
        page.add(Box.createVerticalStrut(18));
        page.add(paragraph("Check each mod's Minecraft version, loader, dependencies, and license before use. "
                + "Forge installers belong outside the active game mods folder."));
        page.add(Box.createVerticalStrut(24));
        page.add(action("Open mod collection", true, () -> open(root.resolve("mods-1.20.1-base"))));
        page.add(Box.createVerticalStrut(12));
        page.add(action("Open resource packs", false, () -> open(root.resolve("resourcepacks"))));
        page.add(Box.createVerticalGlue());
        page.add(paragraph("Your files stay yours. No downloads, automatic installs, or third-party JAR modifications."));
        return page;
    }

    JPanel setup() {
        JPanel page = page("Prepare for the portal.", "Use your licensed Minecraft account in the official launcher.");
        page.add(label("01  /  MINECRAFT LAUNCHER", 12, GREEN));
        page.add(Box.createVerticalStrut(12));
        page.add(paragraph("Open Minecraft detects a standard Windows Minecraft Launcher installation. "
                + "If yours is installed elsewhere, choose its executable below."));
        page.add(Box.createVerticalStrut(14));
        updateLauncherPath();
        page.add(launcherPath);
        page.add(Box.createVerticalStrut(12));
        JPanel choose = row();
        choose.add(action("Choose launcher .exe", false, this::chooseLauncher));
        choose.add(action("Use auto-detection", false, this::resetLauncher));
        page.add(choose);
        page.add(Box.createVerticalStrut(30));
        page.add(label("02  /  FORGE 1.20.1", 12, GREEN));
        page.add(Box.createVerticalStrut(12));
        page.add(paragraph("Run Minecraft Java 1.20.1 once, close the game, then run the bundled Forge installer. "
                + "Select Install client in its own window. This hub does not silently install anything."));
        page.add(Box.createVerticalStrut(16));
        page.add(action("Open Forge installer", true, this::launchForge));
        page.add(Box.createVerticalStrut(30));
        page.add(label("03  /  CHOOSE YOUR PROFILE", 12, GREEN));
        page.add(Box.createVerticalStrut(12));
        page.add(paragraph("In Minecraft Launcher, choose your Forge 1.20.1 installation and confirm its game directory. "
                + "Only add compatible mods and a backed-up copy of the world."));
        page.add(Box.createVerticalGlue());
        return page;
    }

    void select(String title) {
        ((CardLayout) pages.getLayout()).show(pages, title);
        for (JButton button : navigation) {
            boolean selected = button.getText().equals(title);
            button.setBackground(selected ? new Color(65, 39, 25) : PANEL);
            button.setForeground(selected ? ACCENT : MUTED);
        }
    }

    void launchMinecraft() {
        String configured = settings.getProperty("minecraftLauncher", "");
        run("Opening Minecraft Launcher...", () -> {
            if (!configured.isBlank()) {
                Path executable = Path.of(configured);
                if (!Files.isRegularFile(executable)) throw new IOException("Configured launcher is missing. Choose it again in Settings.");
                new ProcessBuilder(executable.toString()).start();
                return;
            }
            for (String variable : List.of("ProgramFiles(x86)", "ProgramFiles", "LOCALAPPDATA")) {
                String directory = System.getenv(variable);
                if (directory == null) continue;
                Path executable = Path.of(directory, "Minecraft Launcher", "MinecraftLauncher.exe");
                if (Files.isRegularFile(executable)) {
                    new ProcessBuilder(executable.toString()).start();
                    return;
                }
            }
            String script = "$ErrorActionPreference='Stop'; "
                    + "$app = Get-StartApps | Where-Object { $_.Name -eq 'Minecraft Launcher' } | Select-Object -First 1; "
                    + "if (-not $app) { throw 'Minecraft Launcher was not found. Install it or choose its executable in Settings.' }; "
                    + "Start-Process ('shell:AppsFolder\\' + $app.AppID)";
            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script)
                    .redirectErrorStream(true).start();
            String output;
            try (InputStream input = process.getInputStream()) {
                output = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            if (process.waitFor() != 0) throw new IOException(output.strip());
        }, "Launch request sent. Choose your Minecraft 1.20.1 profile.");
    }

    void launchForge() {
        int answer = JOptionPane.showConfirmDialog(frame,
                "Open the bundled Forge 1.20.1 installer?\nOnly run installers you trust.\n"
                        + "Its window will let you choose whether and where to install.",
                "Forge setup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        run("Opening Forge installer...", () -> {
            Path installer = forgeInstaller(root);
            Path java = Path.of(System.getProperty("java.home"), "bin", "javaw.exe");
            if (!Files.isRegularFile(java)) java = Path.of(System.getProperty("java.home"), "bin", "java");
            Process process = new ProcessBuilder(java.toString(), "-jar", installer.toString())
                    .directory(root.toFile())
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(root.resolve("launcher").resolve("forge-installer.log").toFile()))
                    .redirectErrorStream(true).start();
            if (process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() != 0) {
                throw new IOException("Forge exited with code " + process.exitValue()
                        + ". See launcher\\forge-installer.log for details.");
            }
        }, "Forge start requested. If no window appears, check launcher\\forge-installer.log.");
    }

    void chooseLauncher() {
        JFileChooser picker = new JFileChooser();
        picker.setDialogTitle("Choose the official Minecraft Launcher executable");
        picker.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Windows application (*.exe)", "exe"));
        picker.setAcceptAllFileFilterUsed(false);
        if (picker.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        Path selected = picker.getSelectedFile().toPath().toAbsolutePath();
        if (!Files.isRegularFile(selected) || !selected.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".exe")) {
            error(new IOException("Choose an existing Minecraft Launcher .exe file."));
            return;
        }
        saveSetting(selected.toString());
    }

    void resetLauncher() {
        saveSetting("");
    }

    void saveSetting(String value) {
        Properties next = new Properties();
        next.putAll(settings);
        if (value.isEmpty()) next.remove("minecraftLauncher");
        else next.setProperty("minecraftLauncher", value);
        try (OutputStream output = Files.newOutputStream(preferences)) {
            next.store(output, "Mine Canon World local launcher preferences");
        } catch (IOException failure) {
            error(failure);
            return;
        }
        settings.clear();
        settings.putAll(next);
        updateLauncherPath();
        status.setText("Launcher preference saved.");
    }

    void updateLauncherPath() {
        String path = settings.getProperty("minecraftLauncher", "");
        launcherPath.setText(path.isBlank() ? "Currently using automatic detection" : "Custom launcher selected (hover for path)");
        launcherPath.setToolTipText(path.isBlank() ? "Standard desktop and Microsoft Store installations" : path);
    }

    void open(Path path) {
        run("Opening " + path.getFileName() + "...", () -> {
            if (!Files.isDirectory(path)) throw new IOException("Folder not found: " + path);
            Desktop.getDesktop().open(path.toFile());
        }, "Opened " + path.getFileName() + ".");
    }

    interface Job { void execute() throws Exception; }

    void run(String message, Job job, String success) {
        status.setText(message);
        actions.forEach(button -> button.setEnabled(false));
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                job.execute();
                return null;
            }
            protected void done() {
                actions.forEach(button -> button.setEnabled(true));
                try {
                    get();
                    status.setText(success);
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    error(failure);
                } catch (ExecutionException failure) {
                    error(failure.getCause());
                }
            }
        }.execute();
    }

    void error(Throwable failure) {
        failure.printStackTrace();
        status.setText("Action failed. See the error message.");
        JOptionPane.showMessageDialog(frame, failure.getMessage(), "MineCanon", JOptionPane.ERROR_MESSAGE);
    }

    JButton action(String text, boolean primary, Runnable callback) {
        JButton button = button(text, primary, callback);
        actions.add(button);
        return button;
    }

    static JButton button(String text, boolean primary, Runnable callback) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(primary ? Color.WHITE : TEXT);
        button.setBackground(primary ? ACCENT : LINE);
        button.setBorder(new EmptyBorder(14, 18, 14, 18));
        button.setFocusPainted(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(button.getPreferredSize());
        button.addActionListener(event -> callback.run());
        return button;
    }

    static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", size >= 16 ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    static JLabel serif(String text, int size) {
        JLabel label = label(text, size, TEXT);
        label.setFont(new Font("Georgia", Font.BOLD, size));
        return label;
    }

    static JPanel column(Color background, int padding) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(background != null);
        if (background != null) panel.setBackground(background);
        panel.setBorder(new EmptyBorder(padding, padding, padding, padding));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    static JPanel row() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        return panel;
    }

    static JPanel page(String title, String subtitle) {
        JPanel page = column(BG, 0);
        page.add(serif(title, 30));
        page.add(Box.createVerticalStrut(12));
        page.add(label(subtitle, 13, MUTED));
        page.add(Box.createVerticalStrut(30));
        return page;
    }

    static JPanel stat(String eyebrow, String value, String caption) {
        JPanel card = column(PANEL, 16);
        card.add(label(eyebrow, 10, ACCENT));
        card.add(Box.createVerticalStrut(10));
        card.add(label(value, 20, TEXT));
        card.add(Box.createVerticalStrut(6));
        card.add(label(caption, 10, MUTED));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        return card;
    }

    static JTextArea paragraph(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setForeground(MUTED);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        return area;
    }

    static java.awt.image.BufferedImage icon() {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(BG);
        g.fillRoundRect(0, 0, 64, 64, 14, 14);
        g.setColor(ACCENT);
        g.fillPolygon(new int[] {12, 32, 52, 52, 42, 42, 32, 22, 22, 12},
                new int[] {10, 28, 10, 54, 54, 30, 40, 30, 54, 54}, 10);
        g.dispose();
        return image;
    }

    static class Landscape extends JPanel {
        final BufferedImage image;
        Landscape(BufferedImage image) {
            this.image = image;
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.clip(new RoundRectangle2D.Double(0, 0, w, h, 22, 22));
            double scale = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
            int iw = (int) Math.ceil(image.getWidth() * scale);
            int ih = (int) Math.ceil(image.getHeight() * scale);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
            g.setPaint(new GradientPaint(0, 0, new Color(5, 11, 14, 210), w * .85f, 0, new Color(5, 11, 14, 0)));
            g.fillRect(0, 0, w, h);
            g.dispose();
        }
    }
}
