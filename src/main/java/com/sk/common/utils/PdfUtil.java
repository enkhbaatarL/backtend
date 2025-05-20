package com.sk.common.utils;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.shared.enums.Extension;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PdfUtil {

    /**
     * BufferedImage를 JPEG 바이트 배열로 변환합니다.
     *
     * @param src 변환할 BufferedImage
     * @param quality JPEG 압축 품질 (0.0 ~ 1.0)
     * @return JPEG 바이트 배열
     */
    public static byte[] imageToJpegByteArray(BufferedImage src, float quality) { // 이미지 -> jpeg byte array

        BufferedImage rgbImg;
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            rgbImg = src;
        } else {
            rgbImg = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgbImg.createGraphics();
            g.drawImage(src, 0, 0, null);
            g.dispose();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName(Extension.JPEG.getCode()).next();
            ImageWriteParam param = jpgWriter.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            param.setCompressionQuality(quality); // 0.0 ~ 1.0

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                jpgWriter.setOutput(ios);
                jpgWriter.write(null, new IIOImage(rgbImg, null, null), param);
            } finally {
                jpgWriter.dispose();
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "Failed to convert image to JPEG byte array");
        }
    }

    /**
     * 바이트 배열을 BufferedImage로 변환합니다.
     *
     * @param imageBytes 변환할 바이트 배열
     * @return BufferedImage
     */
    public static BufferedImage byteArrayToImage(byte[] imageBytes) { // byte array -> 이미지
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            return ImageIO.read(bais);
        } catch (IOException e) {
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "Failed to convert byte array to image");
        }
    }

    /**
     * 이미지에 ARGB 형식으로 텍스트 워터마크를 추가합니다.
     *
     * @param image 워터마크를 추가할 이미지
     * @param watermarkText 워터마크로 사용할 텍스트
     * @return 워터마크가 추가된 BufferedImage
     */
    public static BufferedImage addTextWatermarkARGB(BufferedImage image, String watermarkText) { // 이미지에 텍스트 워터마크 추가 (ARGB)
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
        g2d.setFont(new Font("Dialog", Font.BOLD, fontSize));

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
        return watermarked;
    }

    /**
     * 이미지에 RGB 형식으로 텍스트 워터마크를 추가합니다.
     *
     * @param base 워터마크를 추가할 이미지
     * @param text 워터마크로 사용할 텍스트
     * @return 워터마크가 추가된 BufferedImage
     */
    public static BufferedImage addTextWatermarkRGB(BufferedImage base, String text) { // 이미지에 텍스트 워터마크 추가 (RGB)

        int w = base.getWidth();
        int h = base.getHeight();

        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(base, 0, 0, null);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        g.setColor(Color.RED);
        g.setFont(new Font("Dialog", Font.BOLD, 20));
        g.rotate(Math.toRadians(-30), w / 2.0, h / 2.0);

        FontMetrics fm = g.getFontMetrics();
        int stepX = fm.stringWidth(text) + 100;
        int stepY = fm.getHeight() + 80;

        for (int x = -w; x < w * 2; x += stepX)
            for (int y = -h; y < h * 2; y += stepY)
                g.drawString(text, x, y);

        g.dispose();
        return dst;
    }

    /**
     * 이미지 크기를 조정합니다.
     *
     * @param src 크기를 조정할 이미지
     * @param scale 크기 조정 비율
     * @return 크기가 조정된 BufferedImage
     */
    public static BufferedImage resize(BufferedImage src, double scale) { // 이미지 크기 조정
        int w = (int) (src.getWidth() * scale);
        int h = (int) (src.getHeight() * scale);
        BufferedImage dst = new BufferedImage(w, h, src.getType());
        Graphics2D g2 = dst.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(src, 0, 0, w, h, null);
        } finally {
            g2.dispose();
        }
        return dst;
    }

}
