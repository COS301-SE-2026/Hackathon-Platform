package com.hackathon.platform.service;

import com.hackathon.platform.certificate.CertificateGenerator;
import com.hackathon.platform.certificate.CertificateRecipient;
import com.hackathon.platform.certificate.CertificateRecipientResolver;
import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.dto.CertificateTemplateRequest;
import com.hackathon.platform.dto.CertificateVerificationResponse;
import com.hackathon.platform.model.*;
import com.hackathon.platform.repository.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hackathon.platform.storage.BlobPath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CertificateService {
  private static final float PAGE_WIDTH = PDRectangle.A4.getHeight();
  private static final float PAGE_HEIGHT = PDRectangle.A4.getWidth();

  private static final float[] NAVY = {0.09f, 0.13f, 0.24f};
  private static final float[] GOLD = {0.72f, 0.58f, 0.20f};
  private static final float[] GREY = {0.35f, 0.38f, 0.45f};

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String VERIFICATION_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private final String VERIFICATION_BASE_URL = "https://hackathonplatform.co.za/verify/";

  private final EventRepository eventRepo;
  private final HackathonRepository hackRepo;
  private final EventRegistrationRepository eventRegRepo;
  private final StorageService storageService;
  private final TeamRepository teamRepository;
  private AzureBlobConfig blob;
  private final CertificateTemplateRepository templateRepo;
  private final CertificateGenerationRunRepository runRepo;
  private final CertificateIssuedRepository issuedRepo;
  private final CertificateRecipientResolver recipientResolver;
  private final CertificateGenerator certificateGenerator;

  public CertificateService(
          EventRepository eventRepo,
          HackathonRepository hackRepo,
          EventRegistrationRepository eventRegRepo,
          StorageService storageService,
          AzureBlobConfig blobConfig, CertificateTemplateRepository templateRepo, CertificateGenerationRunRepository runRepo, CertificateIssuedRepository issuedRepo, CertificateRecipientResolver recipientResolver, CertificateGenerator certificateGenerator, TeamRepository teamRepository) {
    this.eventRepo = eventRepo;
    this.hackRepo = hackRepo;
    this.eventRegRepo = eventRegRepo;
    this.storageService = storageService;
    this.blob = blobConfig;
    this.templateRepo = templateRepo;
    this.runRepo = runRepo;
    this.issuedRepo = issuedRepo;
    this.recipientResolver = recipientResolver;
    this.certificateGenerator = certificateGenerator;
    this.teamRepository = teamRepository;
  }

  public byte[] genCertificate(UUID eventId, User user) {
    Event event =
        eventRepo
            .findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found"));

    if (!eventRegRepo.existsByEventIdAndUserId(eventId, user.getUserId())) {
      throw new IllegalArgumentException("You are not registered for this event");
    }

    String hackathonName =
        hackRepo.findById(event.getHackathon()).map(Hackathon::getName).orElse(event.getName());
    String partName = (user.getFirstName() + " " + user.getLastName());

    try {
      return render(partName, event, hackathonName);
    } catch (IOException e) {
      throw new RuntimeException("Failed to make the certificate");
    }
  }

  private byte[] render(String partName, Event event, String hackathonName) throws IOException {
    try (PDDocument doc = new PDDocument()) {
      PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
      doc.addPage(page);

      PDFont titleFont = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
      PDFont cursive = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);
      PDFont body = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      PDFont bodyBoldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

      try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
        drawBackground(cs);
        drawBorder(cs);
        float centreX = PAGE_WIDTH / 2f;
        float y = PAGE_HEIGHT - 130;

        PDImageXObject logo = loadLogoImage(doc, event);
        if (logo != null) {
          float logoSize = 70f;
          float logoX = centreX - logoSize / 2f;
          float logoY = y - logoSize + 20f;
          cs.drawImage(logo, logoX, logoY, logoSize, logoSize);
          y -= (logoSize + 20f);
        }

        drawCentered(cs, body, 13, centreX, y, "CERTIFICATE OF PARTICIPATION", GREY, 3.5F);
        y -= 55f;
        drawCentered(cs, titleFont, 34, centreX, y, hackathonName, NAVY, 0f);
        y -= 55f;
        drawCentered(cs, body, 13, centreX, y, "This certifies that", GREY, 0f);
        y -= 58f;
        drawCentered(cs, cursive, 40, centreX, y, partName, GOLD, 0f);
        y -= 6;
        cs.setStrokingColor(GOLD[0], GOLD[1], GOLD[2]);
        cs.setLineWidth(1.2f);
        float nameWidth = cursive.getStringWidth(partName) / 1000 * 40;
        float lineHalf = Math.max(nameWidth / 2f + 40, 160);
        cs.moveTo(centreX - lineHalf, y);
        cs.lineTo(centreX + lineHalf, y);
        cs.stroke();
        y -= 45;

        String bodyText = "has successfully participated in \"" + event.getName() + "\"";
        drawCentered(cs, body, 13, centreX, y, bodyText, GREY, 0f);
        y -= 110;

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy"));
        float footerY = 110;
        float leftX = 140;
        float rightX = PAGE_WIDTH - 140;

        cs.setStrokingColor(GREY[0], GREY[1], GREY[2]);
        cs.setLineWidth(0.8f);
        cs.moveTo(leftX, footerY + 30);
        cs.lineTo(leftX + 170, footerY + 30);
        cs.stroke();
        drawLeft(cs, bodyBoldFont, 11, leftX, footerY + 12, dateStr, NAVY);
        drawLeft(cs, body, 9.5f, leftX, footerY - 3, "Date Issued", GREY);

        cs.moveTo(rightX - 170, footerY + 30);
        cs.lineTo(rightX, footerY + 30);
        cs.stroke();
        drawRight(cs, bodyBoldFont, 11, rightX, footerY + 12, "Hackathon Platform", NAVY);
        drawRight(cs, body, 9.5f, rightX, footerY - 3, "Hosted on", GREY);
        drawCentered(cs, bodyBoldFont, 9, centreX, 55, "HACKATHON PLATFORM", GOLD, 2f);
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      doc.save(out);
      return out.toByteArray();
    }
  }

  private void drawBackground(PDPageContentStream cs) throws IOException {
    cs.setNonStrokingColor(0.99f, 0.98f, 0.95f);
    cs.addRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
    cs.fill();
  }

  private void drawBorder(PDPageContentStream cs) throws IOException {
    float outerMargin = 28;
    float innerMargin = 40;

    cs.setStrokingColor(NAVY[0], NAVY[1], NAVY[2]);
    cs.setLineWidth(3f);
    cs.addRect(
        outerMargin, outerMargin, PAGE_WIDTH - 2 * outerMargin, PAGE_HEIGHT - 2 * outerMargin);
    cs.stroke();
    cs.setStrokingColor(GOLD[0], GOLD[1], GOLD[2]);
    cs.setLineWidth(1f);
    cs.addRect(
        innerMargin, innerMargin, PAGE_WIDTH - 2 * innerMargin, PAGE_HEIGHT - 2 * innerMargin);
    cs.stroke();
  }

  private void drawCentered(
      PDPageContentStream cs,
      PDFont font,
      float size,
      float centreX,
      float y,
      String text,
      float[] colour,
      float spacing)
      throws IOException {
    float width = font.getStringWidth(text) / 1000 * size;
    if (spacing > 0) {
      width += spacing * (text.length() - 1);
    }

    float x = centreX - width / 2f;
    cs.beginText();
    cs.setNonStrokingColor(colour[0], colour[1], colour[2]);
    cs.setFont(font, size);
    if (spacing > 0) {
      cs.setCharacterSpacing((spacing));
    }
    cs.newLineAtOffset(x, y);
    cs.showText(text);
    cs.endText();
  }

  private void drawLeft(
      PDPageContentStream cs,
      PDFont font,
      float size,
      float x,
      float y,
      String text,
      float[] colour)
      throws IOException {
    cs.beginText();
    cs.setNonStrokingColor(colour[0], colour[1], colour[2]);
    cs.setFont(font, size);
    cs.newLineAtOffset(x, y);
    cs.showText(text);
    cs.endText();
  }

  private void drawRight(
      PDPageContentStream cs,
      PDFont font,
      float size,
      float x,
      float y,
      String text,
      float[] colour)
      throws IOException {
    float width = font.getStringWidth(text) / 1000 * size;
    cs.beginText();
    cs.setNonStrokingColor(colour[0], colour[1], colour[2]);
    cs.setFont(font, size);
    cs.newLineAtOffset(x - width, y);
    cs.showText(text);
    cs.endText();
  }

  private PDImageXObject loadLogoImage(PDDocument doc, Event event) {
    String logoKey = event.getLogoStorageKey();
    if (logoKey == null || logoKey.isBlank()) {
      return null;
    }
    try (InputStream in = storageService.download(blob.getEventResourcesContainer(), logoKey)) {
      byte[] bytes = in.readAllBytes();
      return PDImageXObject.createFromByteArray(doc, bytes, logoKey);
    } catch (Exception e) {
      return null;
    }
  }

  @Transactional
  public CertificateTemplate createTemplate(CertificateTemplateRequest req, UUID createdByUserId){
    CertificateTemplate template = new CertificateTemplate();
    template.setName(req.getName());
    template.setEventId(req.getEventId());
    template.setHackathonId(req.getHackathonId());
    template.setLayout(req.getLayout());
    template.setCreatedByUserId(createdByUserId);
    return templateRepo.save(template);
  }

  @Transactional
  public CertificateTemplate updateTemplate(UUID templateId, CertificateTemplateRequest req){
    CertificateTemplate template = getTemplate(templateId);
    template.setName(req.getName());
    template.setLayout(req.getLayout());
    template.setUpdatedAt(OffsetDateTime.now());
    return templateRepo.save(template);
  }

  @Transactional
  public String uploadBackground(UUID templateId, MultipartFile file){
    CertificateTemplate template = getTemplate(templateId);
    String storageKey = BlobPath.certificateBackground(templateId.toString(), file.getOriginalFilename());
    storageService.upload(blob.getEventResourcesContainer(), storageKey, file);
    template.setBackgroundStorageKey(storageKey);
    template.setUpdatedAt(OffsetDateTime.now());
    templateRepo.save(template);
    return storageKey;
  }

  @Transactional(readOnly = true)
  public String uploadTemplateAsset(UUID templateId, MultipartFile file){
    getTemplate(templateId);
    String storageKey = BlobPath.certificateAsset(templateId.toString(), file.getOriginalFilename());
    storageService.upload(blob.getEventResourcesContainer(), storageKey, file);
    return storageKey;
  }

  public String resolveAssetUrl(String storageKey){
    return storageService.generatePresignedUrl(blob.getEventResourcesContainer(), storageKey, 60);
  }

  public Map<String, String> resolveTemplateAssetUrls(CertificateTemplate template){
    Map<String, String> urls = new HashMap<>();
    for(var el : template.getLayout().getElements()){
      if("IMAGE".equals(el.getType()) && el.getImageStorageKey() != null){
        urls.computeIfAbsent(el.getImageStorageKey(), this::resolveAssetUrl);
      }
    }
    return urls;
  }

  @Transactional(readOnly = true)
  public CertificateTemplate getTemplate(UUID templateId){
    return templateRepo.findById(templateId).orElseThrow(() -> new IllegalArgumentException("Certificate template not found"));
  }

  @Transactional(readOnly = true)
  public List<CertificateTemplate> getTemplatesForEvent(UUID eventId, UUID hackathonId){
    List<CertificateTemplate> templates = templateRepo.findByEventId(eventId);
    if(hackathonId != null){
      templates.addAll(templateRepo.findByHackathonIdAndEventIdIsNull(hackathonId));
    }
    return templates;
  }

  @Transactional
  public void deleteTemplate(UUID templateId){
    templateRepo.deleteById(templateId);
  }

  public String resolveBackgroundUrl(CertificateTemplate template){
    if(template.getBackgroundStorageKey() == null){
      return null;
    }
    return storageService.generatePresignedUrl(blob.getEventResourcesContainer(), template.getBackgroundStorageKey(), 60);
  }

  @Transactional
  public CertificateGenerationRun startGeneration(UUID eventId, UUID templateId, String scope, Integer topN, UUID requestedByUserId){
    if(!eventRepo.existsById(eventId)){
      throw new IllegalArgumentException("Event not found");
    }
    getTemplate(templateId);
    CertificateGenerationRun run = new CertificateGenerationRun();
    run.setEventId(eventId);
    run.setTemplateId(templateId);
    run.setScope(scope == null ? "ALL_PARTICIPANTS" : scope);
    run.setTopN(topN);
    run.setStatus("PENDING");
    run.setRequestedByUserId(requestedByUserId);
    run = runRepo.save(run);
    runGeneration(run.getRunId());
    return run;
  }

  @Async
  @Transactional
  public void runGeneration(UUID runId) {
    CertificateGenerationRun run = runRepo.findById(runId).orElse(null);
    if(run == null){
      return;
    }
    try{
      run.setStatus("RUNNING");
      runRepo.save(run);
      Event event = eventRepo.findById(run.getEventId()).orElseThrow(() -> new IllegalArgumentException("Event not found"));
      CertificateTemplate template = getTemplate(run.getTemplateId());
      byte[] backgroundBytes = null;
      if(template.getBackgroundStorageKey() != null){
        backgroundBytes = storageService.download(blob.getEventResourcesContainer(), template.getBackgroundStorageKey()).readAllBytes();
      }
      Map<String, byte[]> imageAssetBytes = new HashMap<>();
      for(var el : template.getLayout().getElements()){
        if("IMAGE".equals(el.getType()) && el.getImageStorageKey() != null){
          imageAssetBytes.computeIfAbsent(el.getImageStorageKey(), key -> {
            try {
              return storageService.download(blob.getEventResourcesContainer(), key).readAllBytes();
            } catch (IOException e) {
              return null;
            }
          });
        }
      }

      List<CertificateRecipient> recipients = recipientResolver.resolve(event, run.getScope(), run.getTopN());
      run.setTotalCount(recipients.size());
      runRepo.save(run);

      int completed = 0;
      for (CertificateRecipient recipient : recipients){
        issueOne(run, template, backgroundBytes, imageAssetBytes, recipient, event);
        completed++;
        run.setCompletedCount(completed);
        runRepo.save(run);
      }

      run.setStatus("COMPLETED");
      run.setCompletedAt(OffsetDateTime.now());
      runRepo.save(run);
    } catch (Exception e) {
      run.setStatus("FAILED");
      run.setErrorMessage(e.getMessage());
      run.setCompletedAt(OffsetDateTime.now());
      runRepo.save(run);
    }
  }

  private void issueOne(CertificateGenerationRun run, CertificateTemplate template, byte[] backgroundBytes, Map<String, byte[]> imageAssetBytes, CertificateRecipient recipient, Event event) throws Exception {
    UUID certificateId = UUID.randomUUID();
    String verificationCode = generateVerificationCode();
    String verificationUrl = VERIFICATION_BASE_URL + verificationCode;

    byte[] pdfBytes = certificateGenerator.generate(template.getLayout(), backgroundBytes, imageAssetBytes, recipient.getFieldValues(), verificationUrl, recipient.getCertificateType());
    String storageKey = BlobPath.certificatePdf(event.getEventId().toString(), run.getRunId().toString(), certificateId.toString());
    storageService.uploadBytes(blob.getEventResourcesContainer(), storageKey, pdfBytes, "application/pdf");

    CertificateIssued issued = new CertificateIssued();
    issued.setCertificateId(certificateId);
    issued.setRunId((run.getRunId()));
    issued.setTemplateId(template.getTemplateId());
    issued.setEventId(event.getEventId());
    issued.setTeamId(recipient.getTeamId());
    issued.setUserId(recipient.getUserId());
    issued.setCertificateType(recipient.getCertificateType());
    issued.setRecipientName(recipient.getRecipientName());
    issued.setRankAtIssue(recipient.getRank());
    issued.setStorageKey(storageKey);
    issued.setVerificationCode(verificationCode);
    issuedRepo.save(issued);
  }

  private String generateVerificationCode(){
    String code;
    do{
      StringBuilder sb = new StringBuilder(10);
      for (int i=0; i<10;i++){
        sb.append(VERIFICATION_ALPHABET.chatAt(RANDOM.nextInt(VERIFICATION_ALPHABET.length())));
      }
      code = sb.toString();
    } while (issuedRepo.existsByVerificationCode(code));
    return code;
  }

  @Transactional(readOnly = true)
  public CertificateGenerationRun getRun(UUID runId){
    return runRepo.findById(runId).orElseThrow(() -> new IllegalArgumentException("Generation run not found"));
  }

  @Transactional(readOnly = true)
  public List<CertificateGenerationRun> getRunsForEvent(UUID eventId){
    return runRepo.findByEventIdOrderByRequestedAtDesc(eventId);
  }

  @Transactional(readOnly = true)
  public List<CertificateIssued> getIssuedForUser(UUID userId, List<UUID> teamIds){
    return issuedRepo.findByUserIdOrTeamIdInOrderByIssuedAtDesc(userId, teamIds);
  }

  @Transactional(readOnly = true)
  public CertificateIssued getIssued(UUID certificateId){
    return issuedRepo.findById(certificateId).orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
  }

  public String resolveDownloadUrl(CertificateIssued cert){
    return storageService.generatePresignedUrl(blob.getEventResourcesContainer(), cert.getStorageKey(), 60, "certificate-"+cert.getRecipientName().replace(" ", "-")+".pdf");
  }

  @Transactional(readOnly = true)
  public CertificateVerificationResponse verify(String verificationCode){
    return issuedRepo.findByVerificationCode(verificationCode).map(
            cert -> {
              Event event = eventRepo.findById(cert.getEventId()).orElse(null);
              return new CertificateVerificationResponse(true, cert.getRecipientName(), event == null ? "Unknown event" : event.getName(), cert.getCertificateType(), cert.getRankAtIssue(), cert.getIssuedAt());
            }).orElse(CertificateVerificationResponse.invalid());
  }
}
