import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Builds the MineCanon app icon from the bundled world artwork. Run with an output .ico path at build time. */
public class MineCanonIcon {
    static BufferedImage render(int size) throws IOException {
        BufferedImage art;
        try (InputStream in = MineCanonIcon.class.getResourceAsStream("/assets/world.png")) {
            if (in == null) throw new IOException("Missing /assets/world.png");
            art = ImageIO.read(in);
        }
        int side = Math.min(art.getWidth(), art.getHeight());
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        float arc = size * 0.22f;
        Shape tile = new RoundRectangle2D.Float(0, 0, size, size, arc, arc);
        g.setClip(tile);
        int x = (art.getWidth() - side) / 2, y = (art.getHeight() - side) / 2;
        g.drawImage(art, 0, 0, size, size, x, y, x + side, y + side, null);
        g.setClip(null);
        g.setColor(LauncherUI.ORANGE);
        float stroke = Math.max(2f, size / 32f);
        g.setStroke(new BasicStroke(stroke));
        g.draw(new RoundRectangle2D.Float(stroke / 2, stroke / 2, size - stroke, size - stroke, arc, arc));
        g.dispose();
        return icon;
    }

    public static void main(String[] args) throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(render(256), "png", png);
        byte[] pixels = png.toByteArray();
        ByteBuffer ico = ByteBuffer.allocate(22 + pixels.length).order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short) 0).putShort((short) 1).putShort((short) 1);
        // Width/height byte 0 means 256 px in the ICO directory format.
        ico.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
        ico.putShort((short) 1).putShort((short) 32).putInt(pixels.length).putInt(22);
        ico.put(pixels);
        Files.write(Path.of(args[0]), ico.array());
    }
}
