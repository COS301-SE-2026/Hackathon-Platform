package com.hackathon.platform.certificate;

import com.hackathon.platform.model.CertificateLayout;
import com.hackathon.platform.model.CertificateLayout.CertificateElement;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

@Component
public class CertificateGenerator{
    private static final float A4_LANDSCAPE_WIDTH = PDRectangle.A4.getHeight();
    private static final float A4_LANDSCAPE_HEIGHT = PDRectangle.A4.getWidth();

    public byte[] generate(CertificateLayout layout, byte[] backgroundImageBytes, Map<String, byte[]> imageAssetBytes, Map<String, String> fieldValues, String verificationUrl, String certificateType) {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = resolvePageSize(layout.getPageSize());
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            float width = pageSize.getWidth();
            float height = pageSize.getHeight();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)){
                if(backgroundImageBytes != null){
                    PDImageXObject bg = PDImageXObject.createFromByteArray(doc, backgroundImageBytes, "bg");
                    cs.drawImage(bg, 0, 0, width, height);
                }

                for(CertificateElement el : layout.getElements()) {
                    if(!el.isVisibleFor(certificateType)) {
                        continue;
                    }
                    switch(el.getType()) {
                        case "TEXT" -> drawText(cs, doc, el, width, height, resolveText(el, fieldValues));
                        case "QR" -> drawQrCode(cs, doc, el, width, height, verificationUrl);
                        case "IMAGE" -> drawImage(cs, doc, el, height, imageAssetBytes);
                        default -> {

                        }
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException | WriterException e){
            throw new IllegalStateException("Failed to render the certificate", e);
        }
    }

    private String resolveText(CertificateElement el, Map<String, String> fieldValues) {
        if(el.getField() != null && !el.getField().isBlank()) {
            return fieldValues.getOrDefault(el.getField(), "");
        }
        return el.getStaticText() == null ? "" : el.getStaticText();
    }

    private void drawText(PDPageContentStream cs, PDDocument doc, CertificateElement el, float pageWidth, float pageHeight, String text) throws IOException {
        PDFont font = resolveFont(el.getFont());
        float fontSize = (float) el.getFontSize();
        float textWidth = font.getStringWidth(text)/1000*fontSize;
        float pdfX = (float) el.getX();
        float pdfY = pageHeight - (float) el.getY();

        if("center".equalsIgnoreCase(el.getAlign())) {
            pdfX-=textWidth/2;
        } else if ("right".equalsIgnoreCase(el.getAlign())) {
            pdfX -= textWidth;
        }

        Color colour = parseColour(el.getColor());
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(colour);
        cs.newLineAtOffset(pdfX, pdfY);
        cs.showText(text == null ? "" : text);
        cs.endText();
    }

    private void drawImage(PDPageContentStream cs, PDDocument doc, CertificateElement el, float pageHeight, Map<String, byte[]> imageAssetBytes) throws IOException {
        if(el.getImageStorageKey() == null || imageAssetBytes == null) {
            return;
        }
        byte[] bytes = imageAssetBytes.get(el.getImageStorageKey());
        if(bytes == null){
            return;
        }
        float w = (float) el.getWidth();
        float h = (float) el.getHeight();
        PDImageXObject imageObject = PDImageXObject.createFromByteArray(doc, bytes, "asset");
        float pdfX = (float) el.getX();
        float pdfY = pageHeight - (float) el.getY()-h;
        cs.drawImage(imageObject, pdfX, pdfY, w, h);
    }

    private void drawQrCode(PDPageContentStream cs, PDDocument doc, CertificateElement el, float pageWidth, float pageHeight, String verificationUrl) throws IOException, WriterException {
        int size = Math.max(40, (int) el.getFontSize()*5);
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(verificationUrl, BarcodeFormat.QR_CODE, size, size, null);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix);
        PDImageXObject qrObject = PDImageXObject.createFromByteArray(doc, toPngBytes(qrImage), "qr");
        float pdfX = (float) el.getX();
        float pdfY = pageHeight-(float) el.getY() - size;
        cs.drawImage(qrObject, pdfX, pdfY, size, size);
    }

    private byte[] toPngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private PDFont resolveFont(String fontName) {
        if(fontName == null){
            return new org.apache.pdfbox.pdmodel.font.PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
        Standard14Fonts.FontName resolved =
                switch (fontName.toLowerCase()) {
                    case "times", "times-roman", "serif" -> Standard14Fonts.FontName.TIMES_ROMAN;
                    case "times-bold" -> Standard14Fonts.FontName.TIMES_BOLD;
                    case "times-italic" -> Standard14Fonts.FontName.TIMES_ITALIC;
                    case "courier", "monospace" -> Standard14Fonts.FontName.COURIER;
                    case "helvetica-bold", "bold" -> Standard14Fonts.FontName.HELVETICA_BOLD;
                    case "helvetica-oblique", "italic" -> Standard14Fonts.FontName.HELVETICA_OBLIQUE;
                    default -> Standard14Fonts.FontName.HELVETICA;
                };
        return new org.apache.pdfbox.pdmodel.font.PDType1Font(resolved);
    }

    private Color parseColour(String hex) {
        if(hex == null || hex.isBlank()) {
            return Color.BLACK;
        }
        try {
            return Color.decode(hex);
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }

    private PDRectangle resolvePageSize(String pageSize){
        if("A4-portrait".equalsIgnoreCase(pageSize)) {
            return PDRectangle.A4;
        }
        return new PDRectangle(A4_LANDSCAPE_WIDTH, A4_LANDSCAPE_HEIGHT);
    }
}
