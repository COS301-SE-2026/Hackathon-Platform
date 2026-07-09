package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.hackathon.platform.dto.LevelRequest;
import com.hackathon.platform.model.Level;
import com.hackathon.platform.repository.LevelRepository;
import com.hackathon.platform.repository.HackathonRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LevelServiceTest{
    @Mock private LevelRepository levelRepository;
    @Mock private HackathonRepository hackathonRepository;
    private LevelService levelService;

    @BeforeEach
    void setUp(){
        levelService = new LevelService(levelRepository, hackathonRepository);
    }
}