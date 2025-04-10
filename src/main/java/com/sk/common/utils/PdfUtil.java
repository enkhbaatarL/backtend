package com.sk.common.utils;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PdfUtil {

    public static byte[] imageToByteArray(BufferedImage image, String format) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, format, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "Failed to convert image to byte array");
        }
    }

    public static BufferedImage byteArrayToImage(byte[] imageBytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            return ImageIO.read(bais);
        } catch (IOException e) {
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "Failed to convert byte array to image");
        }
    }

    public static BufferedImage addTextWatermark(BufferedImage originalImage, String text) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage watermarked = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = watermarked.createGraphics();

        g2d.drawImage(originalImage, 0, 0, null);

        // 워터마크 스타일
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.setColor(new Color(255, 0, 0, 80)); // 빨간색 + 투명도
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));

        // 중앙 대각선 위치
        g2d.translate(width / 2, height / 2);
        g2d.rotate(Math.toRadians(-30));
        g2d.drawString(text, -g2d.getFontMetrics().stringWidth(text) / 2, 0);

        g2d.dispose();
        return watermarked;
    }

}
