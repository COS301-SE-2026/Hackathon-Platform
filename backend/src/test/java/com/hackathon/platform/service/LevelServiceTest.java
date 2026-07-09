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

    private LevelRequest validRequest(){
        LevelRequest req= new LevelRequest();
        req.setName("idk");
        req.setLevelNumber((short) 1);
        req.setDescription("something here");
        return req;
    }

    @Test
    void createLevel_saveAndReturnLevel_whnValid(){
        UUID hackathonId =UUID.randomUUID();
        LevelRequest levelReq=validRequest();

        when(hackathonRepository.existsById(hackathonId)).thenReturn(true);
        when(levelRepository.existsByHackathonIdAndLevelNumber(hackathonId, (short)1)).thenReturn(false);
        when(levelRepository.save(any(Level.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Level res = levelService.createLevel(hackathonId, levelReq);
        assertThat(res.getName()).isEqualTo("idk");
        assertThat(res.getLevelNumber()).isEqualTo((short)1);
        assertThat(res.getHackathonId()).isEqualTo(hackathonId);
        verify(levelRepository, times(1)).save(any(Level.class));
    }

    @Test
    void createLevel_throws_whenRequestNull(){
        UUID hackathond= UUID.randomUUID();
        assertThatThrownBy(()->levelService.createLevel(hackathond, null)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("null");
    }

    @Test
    void createLevel_throws_whenNameBlank(){
        UUID hackathonId = UUID.randomUUID();
        LevelRequest req= validRequest();
        req.setName(" ");
        assertThatThrownBy(()-> levelService.createLevel(hackathonId, req)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name is required");
    }

    @Test
    void createLevel_throws_whenLevelNumberNull(){
        UUID hackathonId= UUID.randomUUID();
        LevelRequest req= validRequest();
        assertThatThrownBy(() -> levelService.createLevel(hackathonId, req)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Level number");;
    }

    @Test
    void createLevel_throws_whenLevelLowerZero(){
        UUID hackathonId=UUID.randomUUID();
        LevelRequest req = validRequest();
        req.setLevelNumber((short) 0);
        assertThatThrownBy(() -> levelService.createLevel(hackathonId, req)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("grater than 0");
    }

    @Test
    void createLevel_throws_whenHackathonDoesntExist(){
        UUID hackathonId = UUID.randomUUID();
        LevelRequest req = validRequest();

        when(hackathonRepository.existsById(hackathonId)).thenReturn(false);
        assertThatThrownBy(() -> levelService.createLevel(hackathonId, req)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Hackathon doesnt exist");
        verify(levelRepository, never()).save(any());
    }

    @Test
    void createLevel_throws_whenLevelNumAlreUsed(){
        UUID hackathonId = UUID.randomUUID();
        LevelRequest req = validRequest();
        when(hackathonRepository.existsById(hackathonId)).thenReturn(true);
        when(levelRepository.existsByHackathonIdAndLevelNumber(hackathonId, (short) 1)).thenReturn(true);
        assertThatThrownBy(() -> levelService.createLevel(hackathonId, req)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("already exist");
        verify(levelRepository, never()).save(any());
    }
}