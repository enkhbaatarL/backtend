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

    public static BufferedImage addTextWatermark(BufferedImage image, String watermarkText) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage watermarked = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = watermarked.createGraphics();

        // 원본 이미지 그리기
        g2d.drawImage(image, 0, 0, null);

        int fontSize = 30;

        // 워터마크 스타일 설정
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f); // 투명도 낮게
        g2d.setComposite(alphaChannel);
        g2d.setColor(new Color(255, 0, 0, 80)); // 빨간색 + 투명도
        g2d.setFont(new Font("Dialog", Font.BOLD, fontSize)); // todo 폰트... 깨질 수 있으므로 다운받을까 함

        // 회전 설정 (대각선 방향으로)
        g2d.rotate(Math.toRadians(-30), (double) width / 2, (double) height / 2);

        // 문자열 길이와 폰트 기반으로 간격 계산
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(watermarkText);
        int textHeight = fontMetrics.getHeight();

        int stepX = textWidth + 100;  // 텍스트 너비 + 여유 간격
        int stepY = textHeight + 80;  // 텍스트 높이 + 여유 간격

        for (int x = -width; x < width * 2; x += stepX) {
            for (int y = -height; y < height * 2; y += stepY) {
                g2d.drawString(watermarkText, x, y);
            }
        }

        g2d.dispose();
        return resize(watermarked, 0.35);
    }

    public static BufferedImage resize(BufferedImage src, double scale) {
        int w = (int) (src.getWidth() * scale);
        int h = (int) (src.getHeight() * scale);
        Image tmp = src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        BufferedImage dst = new BufferedImage(w, h, src.getType());
        Graphics2D g2 = dst.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.drawImage(src, 0, 0, w, h, null);
        } finally {
            g2.dispose();
        }
        return dst;
    }

}
