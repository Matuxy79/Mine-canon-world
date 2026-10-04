import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import javax.imageio.ImageIO;

public class MineCanonIcon {
    public static void main(String[] args) throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(MineCanonLauncher.icon(), "png", png);
        byte[] pixels = png.toByteArray();
        ByteBuffer ico = ByteBuffer.allocate(22 + pixels.length).order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short) 0).putShort((short) 1).putShort((short) 1);
        ico.put((byte) 64).put((byte) 64).put((byte) 0).put((byte) 0);
        ico.putShort((short) 1).putShort((short) 32).putInt(pixels.length).putInt(22);
        ico.put(pixels);
        Files.write(Path.of(args[0]), ico.array());
    }
}
