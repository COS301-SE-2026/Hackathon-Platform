package com.hackathon.platform.service;

import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.HackathonRepository;
import com.hackathon.platform.repository.EventRegistrationRepository;

@Service
public class CertificateService {
    private static final float PAGE_WIDTH = PDRectangle.A4.getHeight();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getWidth();

    private static final float[] NAVY = {0.09f, 0.13f, 0.24f};
    private static final float[] GOLD = {0.72f, 0.58f, 0.20f};
    private static final float[] grey = {0.35f, 0.38f, 0.45f};

    private final EventRepository eventRepo;
    private final HackathonRepository hackRepo;
    private final EventRegistrationRepository eventRegRepo;

    public CertificateService(EventRepository eventRepo, HackathonRepository hackRepo, EventRegistrationRepository eventRegRepo){
        this.eventRepo = eventRepo;
        this.hackRepo = hackRepo;
        this.eventRegRepo = eventRegRepo;
    }

}