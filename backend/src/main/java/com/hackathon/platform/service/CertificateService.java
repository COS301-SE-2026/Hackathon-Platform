package com.hackathon.platform.service;

import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.HackathonRepository;
import com.hackathon.platform.repository.EventRegistrationRepository;
import com.hackathon.platform.model.User;
import java.util.UUID;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Hackathon;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayOutputStream;

@Service
public class CertificateService {
    private static final float PAGE_WIDTH = PDRectangle.A4.getHeight();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getWidth();

    private static final float[] NAVY = {0.09f, 0.13f, 0.24f};
    private static final float[] GOLD = {0.72f, 0.58f, 0.20f};
    private static final float[] GREY = {0.35f, 0.38f, 0.45f};

    private final EventRepository eventRepo;
    private final HackathonRepository hackRepo;
    private final EventRegistrationRepository eventRegRepo;

    public CertificateService(EventRepository eventRepo, HackathonRepository hackRepo, EventRegistrationRepository eventRegRepo){
        this.eventRepo = eventRepo;
        this.hackRepo = hackRepo;
        this.eventRegRepo = eventRegRepo;
    }

    public byte[] genCertificate(UUID eventId, User user){
        Event event = eventRepo.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if(!eventRegRepo.existsByEventIdAndUserId(eventId, user.getUserId())){
            throw new IllegalArgumentException("You are not registered for this event");
        }

        String hackathonName = hackRepo.findById(event.getHackathon()).map(Hackathon::getName).orElse(event.getName());
        String partName = (user.getFirstName()+" "+user.getLastName());


    }

    private byte[] render(String partName, String eventName, String hackathonName) throws IOException {
        try(PDDocument doc = new PDDocument()){
            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
            doc.addPage(page);

            PDFont titleFont = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            PDFont cursive = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);
            PDFont body = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bodyBoldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try(PDPageContentStream cs = new PDPageContentStream(doc, page)){
                drawBackground(cs);
                drawBorder(cs);
                float centreX = PAGE_WIDTH/2f;
                float y = PAGE_HEIGHT/2f;

                drawCentered(cs, body, 13, centreX, y, "CERTIFICATE OF PARTICIPATION", GREY, 3.5F);
                y-=55f;
                drawCentered(cs, titleFont, 34, centreX, y, hackathonName, NAVY, 0f);
                y-=55f;
                drawCentered(cs, body, 13, centreX, y, "Tis certifies that", GREY, 0f);
                y-=58f;
                drawCentered(cs, cursive, 40, centreX, y, partName, GOLD, 0f);
                y-=6;
                cs.setStrokingColor(GOLD[0], GOLD[1], GOLD[2]);
                cs.setLineWidth(1.2f);
                float nameWidth = cursive.getStringWidth(partName)/1000*40;
                float lineHalf = Math.max(nameWidth/2f+40, 160);
                cs.moveTo(centreX-lineHalf, y);
                cs.lineTo(centreX-lineHalf, y);
                cs.stroke();
                y-=45;

                String bodyText = "has successfully participated in \""+eventName+"\"";
                drawCentered(cs, body, 13, centreX, y, bodyText, GREY, 0f);
                y-=110;

                String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy"));
                float footerY = 110;
                float leftX = 140;
                float rightX = PAGE_WIDTH-140;

                cs.setStrokingColor(GREY[0], GREY[1], GREY[2]);
                cs.setLineWidth(0.8f);
                cs.moveTo(leftX, footerY+30);
                cs.stroke();
                drawLeft(cs, bodyBoldFont, 11, leftX, footerY+12, dateStr, NAVY);
                drawLeft(cs, body, 9.5f, leftX, footerY-3, "Date Issued", GREY);

                cs.moveTo(rightX-170, footerY+30);
                cs.lineTo(rightX, footerY+30);
                cs.stroke();
                drawRight(cs, bodyBoldFont, 11, rightX, footerY+12, "Hackathon Platform", NAVY);
                drawRight(cs, body, 9.5f, rightX, footerY-3, "Hosted on", GREY);
                drawCentered(cs, bodyBoldFont, 9, centreX, 55, "\u2726 HACKATHON PLATFORM \u2726", GOLD, 2f);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private void drawBackground(PDPageContentStream cs) throws IOException{
        cs.setNonStrokingColor(0.99f, 0.98f, 0.95f);
        cs.addRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        cs.fill();
    }

    private void drawBorder(PDPageContentStream cs) throws IOException{
        float outerMargin = 28;
        float innerMargin = 28;

        cs.setStrokingColor(NAVY[0], NAVY[1], NAVY[2]);
        cs.setLineWidth(3f);
        cs.addRect(outerMargin, outerMargin, PAGE_WIDTH-2*outerMargin, PAGE_HEIGHT-2*outerMargin);
        cs.stroke();
        cs.setStrokingColor(GOLD[0], GOLD[1], GOLD[2]);
        cs.setLineWidth(1f);
        cs.addRect(innerMargin, innerMargin, PAGE_WIDTH-2*innerMargin, PAGE_HEIGHT-2*innerMargin);
        cs.stroke();
    }

    private void drawCentered(PDPageContentStream cs, PDFont font, float size, float centreX, float y, String text, float[] colour, float spacing) throws IOException{
        float width = font.getStringWidth(text)/1000*size;
        if(spacing>0){
            width+=spacing*(text.length()-1);
        }

        float x = centreX-width/2f;
        cs.beginText();
        cs.setStrokingColor(colour[0], colour[1], colour[2]);
        cs.setFont(font, size);
        if(spacing>0){
            cs.setCharacterSpacing((spacing));
        }
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private void drawLeft(PDPageContentStream cs, PDFont font, float size, float x, float y, String text, float[] colour) throws IOException{
        cs.beginText();
        cs.setNonStrokingColor(colour[0], colour[1], colour[2]);
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private void drawRight(PDPageContentStream cs, PDFont font, float size, float x, float y, String text, float[] colour) throws IOException{
        float width = font.getStringWidth(text)/1000*size;
        cs.beginText();
        cs.setNonStrokingColor(colour[0], colour[1], colour[2]);
        cs.setFont(font, size);
        cs.newLineAtOffset(x-width/2, y);
        cs.showText(text);
        cs.endText();
    }
}