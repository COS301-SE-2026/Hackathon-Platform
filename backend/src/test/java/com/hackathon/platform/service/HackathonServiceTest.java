package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.hackathon.platform.dto.HackathonRequest;
import com.hackathon.platform.model.Hackathon;
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
class HackathonServiceTest {
    @Mock private HackathonRepository hackathonRepository;
    private HackathonService hackathonService;

    @BeforeEach
    void setUp(){
        hackathonService = new HackathonService(hackathonRepository);
    }

    @Test
    void createHackathon_savesAndReturnsHackathon_whenValid() {
        HackathonRequest req = new HackathonRequest();
        req.setName("Challenge 2026");
        req.setDescription("test hackathon");

        when(hackathonRepository.save(any(Hackathon.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Hackathon result = hackathonService.createHackathon(req);
        assertThat(result.getName()).isEqualTo("Challenge 2026");
        assertThat(result.getDescription()).isEqualTo("test hackathon");
        verify(hackathonRepository, times(1)).save(any(Hackathon.class));
    }
}