package com.hackathon.platform.certificate;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.platform.model.CertificateLayout;
import com.hackathon.platform.model.CertificateLayout.CertificateElement;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CertificateGeneratorTest {
  private static final String URL = "https://hackathonplatform.co.za/verify/ABC123";

  private CertificateGenerator generator;
  private Map<String, String> fields;

  @BeforeEach
  void setUp() {
    generator = new CertificateGenerator();
    fields = new HashMap<>();
    fields.put("participantName", "Tung Sahur");
    fields.put("teamName", "Anything");
  }

  private static byte[] png() throws IOException {
    BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(img, "png", out);
    return out.toByteArray();
  }

  private static CertificateLayout layout(String pageSize, CertificateElement... elements) {
    CertificateLayout l = new CertificateLayout();
    l.setPageSize(pageSize);
    l.setElements(List.of(elements));
    return l;
  }

  private static CertificateElement element(String type) {
    CertificateElement el = new CertificateElement();
    el.setType(type);
    el.setX(100);
    el.setY(100);
    return el;
  }

  private static CertificateElement fieldText(String field) {
    CertificateElement el = element("TEXT");
    el.setField(field);
    return el;
  }

  private static CertificateElement staticText(String text) {
    CertificateElement el = element("TEXT");
    el.setStaticText(text);
    return el;
  }

  private static CertificateElement image(String key) {
    CertificateElement el = element("IMAGE");
    el.setImageStorageKey(key);
    return el;
  }

  private byte[] generate(CertificateLayout l, String type) {
    return generator.generate(l, null, Map.of(), fields, URL, type);
  }

  private static String textOf(byte[] pdf) throws IOException {
    try (PDDocument doc = Loader.loadPDF(pdf)) {
      return new PDFTextStripper().getText(doc);
    }
  }

  private static PDPage firstPage(PDDocument doc) {
    return doc.getPage(0);
  }

  private static int imageCount(byte[] pdf) throws IOException {
    try (PDDocument doc = Loader.loadPDF(pdf)) {
      int count = 0;
      for (COSName ignored : firstPage(doc).getResources().getXObjectNames()) {
        count++;
      }
      return count;
    }
  }

  private static Set<String> fontNames(byte[] pdf) throws IOException {
    try (PDDocument doc = Loader.loadPDF(pdf)) {
      PDPage page = firstPage(doc);
      Set<String> names = new HashSet<>();
      for (COSName n : page.getResources().getFontNames()) {
        names.add(page.getResources().getFont(n).getName());
      }
      return names;
    }
  }

  @Test
  void generate_landscapeByDefault_forNullOrUnknownPageSize() throws IOException {
    for (String size : new String[] {null, "A4-landscape", "weird"}) {
      byte[] pdf = generate(layout(size), "WINNER");
      try (PDDocument doc = Loader.loadPDF(pdf)) {
        assertThat(doc.getNumberOfPages()).isEqualTo(1);
        assertThat(firstPage(doc).getMediaBox().getWidth())
            .isGreaterThan(firstPage(doc).getMediaBox().getHeight());
      }
    }
  }

  @Test
  void generate_portrait_isCaseInsensitive() throws IOException {
    byte[] pdf = generate(layout("a4-PORTRAIT"), "WINNER");
    try (PDDocument doc = Loader.loadPDF(pdf)) {
      assertThat(firstPage(doc).getMediaBox().getHeight())
          .isGreaterThan(firstPage(doc).getMediaBox().getWidth());
    }
  }

  @Test
  void generate_textElement_usesFieldValue() throws IOException {
    byte[] pdf = generate(layout("A4-landscape", fieldText("participantName")), "WINNER");
    assertThat(textOf(pdf)).contains("Tung Sahur");
  }

  @Test
  void generate_textElement_missingFieldValue_rendersEmpty() throws IOException {
    byte[] pdf = generate(layout("A4-landscape", fieldText("rank")), "WINNER");
    assertThat(textOf(pdf).trim()).isEmpty();
  }

  @Test
  void generate_textElement_usesStaticTextWhenFieldBlank() throws IOException {
    CertificateElement el = staticText("Congratulations");
    el.setField("  ");
    byte[] pdf = generate(layout("A4-landscape", el), "WINNER");
    assertThat(textOf(pdf)).contains("Congratulations");
  }

  @Test
  void generate_textElement_nullStaticTextAndField_rendersEmpty() throws IOException {
    byte[] pdf = generate(layout("A4-landscape", element("TEXT")), "WINNER");
    assertThat(textOf(pdf).trim()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"left", "center", "right", "CENTER", "RIGHT"})
  void generate_textElement_supportsAllAlignments(String align) throws IOException {
    CertificateElement el = staticText("Aligned");
    el.setAlign(align);
    el.setX(300);
    assertThat(textOf(generate(layout("A4-landscape", el), "WINNER"))).contains("Aligned");
  }

  @ParameterizedTest
  @CsvSource({
    "times,Times-Roman",
    "times-roman,Times-Roman",
    "serif,Times-Roman",
    "times-bold,Times-Bold",
    "times-italic,Times-Italic",
    "courier,Courier",
    "monospace,Courier",
    "helvetica-bold,Helvetica-Bold",
    "bold,Helvetica-Bold",
    "helvetica-oblique,Helvetica-Oblique",
    "italic,Helvetica-Oblique",
    "Helvetica,Helvetica",
    "comic-sans,Helvetica",
    "TIMES,Times-Roman"
  })
  void generate_textElement_resolvesFonts(String requested, String expected) throws IOException {
    CertificateElement el = staticText("Font test");
    el.setFont(requested);
    byte[] pdf = generate(layout("A4-landscape", el), "WINNER");
    assertThat(fontNames(pdf)).containsExactly(expected);
  }

  @Test
  void generate_textElement_nullFont_defaultsToHelvetica() throws IOException {
    CertificateElement el = staticText("Font test");
    el.setFont(null);
    byte[] pdf = generate(layout("A4-landscape", el), "WINNER");
    assertThat(fontNames(pdf)).containsExactly("Helvetica");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "#FF0000", "0x00FF00", "not-a-colour"})
  void generate_textElement_toleratesAnyColourValue(String colour) throws IOException {
    CertificateElement el = staticText("Coloured");
    el.setColor(colour);
    assertThat(textOf(generate(layout("A4-landscape", el), "WINNER"))).contains("Coloured");
  }

  @Test
  void generate_qrElement_smallFontSize_usesMinimumSize() throws IOException {
    CertificateElement qr = element("QR");
    qr.setFontSize(2);
    assertThat(imageCount(generate(layout("A4-landscape", qr), "WINNER"))).isEqualTo(1);
  }

  @Test
  void generate_qrElement_largeFontSize_scalesSize() throws IOException {
    CertificateElement qr = element("QR");
    qr.setFontSize(30);
    assertThat(imageCount(generate(layout("A4-landscape", qr), "WINNER"))).isEqualTo(1);
  }

  @Test
  void generate_imageElement_drawsAssetWhenPresent() throws IOException {
    byte[] pdf =
        generator.generate(
            layout("A4-landscape", image("logo.png")),
            null,
            Map.of("logo.png", png()),
            fields,
            URL,
            "WINNER");
    assertThat(imageCount(pdf)).isEqualTo(1);
  }

  @Test
  void generate_imageElement_skippedWhenKeyNull() throws IOException {
    byte[] pdf =
        generator.generate(
            layout("A4-landscape", image(null)), null, Map.of(), fields, URL, "WINNER");
    assertThat(imageCount(pdf)).isZero();
  }

  @Test
  void generate_imageElement_skippedWhenAssetMapNull() throws IOException {
    byte[] pdf =
        generator.generate(
            layout("A4-landscape", image("logo.png")), null, null, fields, URL, "WINNER");
    assertThat(imageCount(pdf)).isZero();
  }

  @Test
  void generate_imageElement_skippedWhenAssetMissingFromMap() throws IOException {
    byte[] pdf =
        generator.generate(
            layout("A4-landscape", image("logo.png")), null, Map.of(), fields, URL, "WINNER");
    assertThat(imageCount(pdf)).isZero();
  }

  @Test
  void generate_drawsBackgroundImage() throws IOException {
    byte[] pdf = generator.generate(layout("A4-landscape"), png(), Map.of(), fields, URL, "WINNER");
    assertThat(imageCount(pdf)).isEqualTo(1);
  }

  @Test
  void generate_unknownElementType_isIgnored() throws IOException {
    byte[] pdf = generate(layout("A4-landscape", element("SPARKLES")), "WINNER");
    assertThat(imageCount(pdf)).isZero();
    assertThat(textOf(pdf).trim()).isEmpty();
  }

  @Test
  void generate_respectsVisibleForTypes() throws IOException {
    CertificateElement winnerOnly = staticText("Champion");
    winnerOnly.setVisibleForTypes("WINNER");
    CertificateLayout l = layout("A4-landscape", winnerOnly);

    assertThat(textOf(generate(l, "WINNER"))).contains("Champion");
    assertThat(textOf(generate(l, "PARTICIPATION"))).doesNotContain("Champion");
  }
}
