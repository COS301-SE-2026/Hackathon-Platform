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
import static org.mockito.Mockito.times;

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

    @Test
    void createHackathon_throws_whenNameIsNull(){
        HackathonRequest req = new HackathonRequest();
        req.setName(null);
        assertThatThrownBy(() -> hackathonService.createHackathon(req)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name is required");
        verifyNoInteractions(hackathonRepository);
    }

    @Test
    void createHackathon_throws_whenNameIsBlank(){
        HackathonRequest req = new HackathonRequest();
        req.setName(" ");
        assertThatThrownBy(() -> hackathonService.createHackathon(req)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name is required");
        verifyNoInteractions(hackathonRepository);
    }

    @Test
    void getAllHackathons_returnsAllfromRepository(){
        Hackathon h1 = new Hackathon();
        h1.setName("h1");
        Hackathon h2 = new Hackathon();
        h2.setName("h2");
        when(hackathonRepository.findAll()).thenReturn(List.of(h1, h2));
        List<Hackathon> result = hackathonService.getAllHackathons();
        assertThat(result).hasSize(2).containsExactly(h1, h2);
    }

    @Test
    void getHackathonById_returnHackathon_whenFound(){
        UUID id = UUID.randomUUID();
        Hackathon hackathon = new Hackathon();
        hackathon.setHackathonId(id);
        hackathon.setName("pls find hackathon");
        when (hackathonRepository.findById(id)).thenReturn(Optional.of(hackathon));
        Hackathon result = hackathonService.getHackathonById(id);
        assertThat(result.getName()).isEqualTo("pls find hackathon");
    }

    @Test
    void getHackathonById_throws_whenNotFound(){
        UUID id= UUID.randomUUID();
        when( hackathonRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> hackathonService.getHackathonById(id)).isInstanceOf(RuntimeException.class).hasMessageContaining("not found");
    }
}