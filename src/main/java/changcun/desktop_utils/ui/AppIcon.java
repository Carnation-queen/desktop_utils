package changcun.desktop_utils.ui;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * 统一加载打包进 jar 的 /icon.png 作为应用图标。
 * 若资源缺失（例如某些运行环境），则回退到程序绘制的“电源”图标，保证始终有图标可用。
 */
public final class AppIcon {

    private static final String RESOURCE = "/icon.png";
    private static BufferedImage source;

    private AppIcon() {
    }

    /** 主窗口 / 任务栏使用的图标（原始尺寸，由系统负责缩放）。 */
    public static Image windowIcon() {
        BufferedImage src = source();
        if (src != null) {
            return src;
        }
        return fallback(64);
    }

    /** 系统托盘使用的小图标，缩放到目标尺寸以避免模糊。 */
    public static Image trayIcon(int size) {
        if (size <= 0) {
            size = 16;
        }
        BufferedImage src = source();
        if (src != null) {
            return src.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        }
        return fallback(size);
    }

    private static BufferedImage source() {
        if (source == null) {
            try (InputStream in = AppIcon.class.getResourceAsStream(RESOURCE)) {
                if (in != null) {
                    source = ImageIO.read(in);
                }
            } catch (IOException ignored) {
                // 读取失败时使用程序绘制的回退图标
            }
        }
        return source;
    }

    /** 在内存中绘制一个“电源”样式的回退图标。 */
    private static Image fallback(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 蓝色圆形底
        g.setColor(new Color(0x2F6BFF));
        g.fillOval(0, 0, size, size);

        // 白色电源符号：圆环 + 竖线
        g.setColor(Color.WHITE);
        float stroke = Math.max(1.5f, size / 8f);
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int cx = size / 2;
        int ringInset = Math.max(2, size / 5);
        g.drawOval(ringInset, ringInset, size - ringInset * 2, size - ringInset * 2);

        int topY = Math.max(2, size / 5);
        int midY = size / 2 + size / 10;
        g.drawLine(cx, topY, cx, midY);

        g.dispose();
        return img;
    }
}
