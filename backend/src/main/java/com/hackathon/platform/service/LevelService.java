package com.hackathon.platform.service;

import com.hackathon.platform.dto.LevelRequest;
import com.hackathon.platform.model.Level;
import com.hackathon.platform.repository.LevelRepository;
import com.hackathon.platform.repository.HackathonRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LevelService {
    private final LevelRepository levelRepository;
    private final HackathonRepository hackathonRepository;

    public LevelService(LevelRepository levelRepository, HackathonRepository hackathonRepository){
        this.levelRepository = levelRepository;
        this.hackathonRepository = hackathonRepository;
    }

    public Level createLevel(UUID hackathonId, LevelRequest req) {
        if (req==null){
            throw new IllegalArgumentException("level request is null");
        }
        if( req.getName()==null || req.getName().isBlank()){
            throw new IllegalArgumentException("level name is blank");
        }

        if(req.getLevelNumber()==null || req.getLevelNumber()<=0){
            throw new IllegalArgumentException("invalid Level num");
        }
        if(!hackathonRepository.existsById(hackathonId)){
            throw new IllegalArgumentException("hackathon is not found");
        }

        if(levelRepository.existsByHackathonIdAndLevelNumber(hackathonId, req.getLevelNumber())) {
            throw new IllegalArgumentException("level number "+req.getLevelNumber()+" already exists");
        }

        Level level = new Level(hackathonId, req.getName(), req.getLevelNumber());
        level.setDescription(req.getDescription());

        return levelRepository.save(level);
    }

    public List<Level> getLevelByHackathonId(UUID id){
        if(!hackathonRepository.existsById(id)){
            throw new IllegalArgumentException("hackathon not found");
        }
        return levelRepository.findByHackathonIdOrderByLevelNumberAsc(id);
    }

    public Level getLevelById(short id){
        return levelRepository.findById(id).orElseThrow(() -> new RuntimeException("Lvele not found"));
    }
}