package com.hackathon.platform.service;

import com.hackathon.platform.dto.HackathonRequest;
import com.hackathon.platform.model.Hackathon;
import com.hackathon.platform.repository.HackathonRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class HackathonService {
    private final HackathonRepository hackathonRepository;

    public HackathonService(HackathonRepository hackathonRepository) {
        this.hackathonRepository = hackathonRepository;
    }

    public Hackathon createHackathon(HackathonRequest req){
        if(req == null){
            throw new IllegalArgumentException("Hackathon req cant be null");
        }

        if (req.getName() == null || req.getName().isBlank()){
            throw new IllegalArgumentException("Hackathon name isnt entered");
        }

        Hackathon hackathon = new Hackathon();
        hackathon.setName(req.getName());
        hackathon.setDescription(req.getDescription());

        return hackathonRepository.save(hackathon);
    }
}