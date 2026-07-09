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

    @Test
    void getLevel_returnOrderLevels(){
        UUID hackathonId = UUID.randomUUID();
        Level l1 = new Level(hackathonId, "ez clapz", (short)1);
        Level l2 = new Level(hackathonId, "dis 1 hard", (short)2);
        when(hackathonRepository.existsById(hackathonId)).thenReturn(true);
        when(levelRepository.findByHackathonIdOrderByLevelNumberAsc(hackathonId)).thenReturn(List.of(l1, l2));
        List<Level> res = levelService.getLevelByHackathonId(hackathonId);
        assertThat(res).containsExactly(l1,l2);
    }

    @Test
    void getLevelByHackathon_throws_whenHackathonDoesntExist() {
        UUID hackathonId = UUID.randomUUID();
        when(hackathonRepository.existsById(hackathonId)).thenReturn(false);
        assertThatThrownBy(() -> levelService.getLevelByHackathonId(hackathonId)).isInstanceOf(RuntimeException.class).hasMessageContaining("not found");
    }

    @Test
    void getLevelById_returnLevel_whenFound(){
        Level lvl = new Level(UUID.randomUUID(), "ez", (short)1);
        lvl.setId((short)5);
        when(levelRepository.findById((short)5)).thenReturn(Optional.of(lvl));
        Level res = levelService.getLevelById((short)5);
        assertThat(res.getName()).isEqualTo("ez");
    }

    @Test
    void getLevelById_throw_whenCantFind(){
        when(levelRepository.findById((short)6767)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> levelService.getLevelById((short)6767)).isInstanceOf(RuntimeException.class).hasMessageContaining("not found");
    }

    @Test
    void updateLevel_updateNameAndDescr_whenLvlNumUnchanged(){
        UUID hackathonId = UUID.randomUUID();
        Level ex = new Level(hackathonId, "old", (short)1);
        ex.setId((short)1);
        ex.setDescription("old descr");
        LevelRequest req = new LevelRequest();
        req.setName("new");
        req.setLevelNumber((short)1);
        req.setDescription("new scr");
        when(levelRepository.findById((short)1)).thenReturn(Optional.of(ex));
        when(levelRepository.save(any(Level.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Level res = levelService.updateLevel((short)1, req);
        assertThat(res.getName()).isEqualTo("new");
        assertThat(res.getDescription()).isEqualTo("new scr");
        verify(levelRepository, never()).existsByHackathonIdAndLevelNumber(eq(hackathonId), any(Short.class));
    }

    @Test
    void updateLevel_changeLvlNum_NumFree(){
        UUID hackathonId = UUID.randomUUID();
        Level ex = new Level(hackathonId, "me", (short)1);
        ex.setId((short)1);
        LevelRequest req = new LevelRequest();
        req.setLevelNumber((short)2);
        when(levelRepository.findById((short)1)).thenReturn(Optional.of(ex));
        when(levelRepository.existsByHackathonIdAndLevelNumber(hackathonId, (short) 2)).thenReturn(false);
        when(levelRepository.save(any(Level.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Level res = levelService.updateLevel((short) 1,req);
        assertThat(res.getLevelNumber()).isEqualTo((short)2 );
    }

    @Test
    void updateLevel_throws_whenNewLvlNumTaken(){
        UUID hackathonId = UUID.randomUUID();
        Level ex = new Level(hackathonId, "name", (short) 1);
        ex.setId((short) 1);

        LevelRequest rq = new LevelRequest();
        rq.setLevelNumber((short)2);
        when(levelRepository.findById((short) 1)).thenReturn(Optional.of(ex));
        when(levelRepository.existsByHackathonIdAndLevelNumber(hackathonId, (short)2)).thenReturn(true);
        assertThatThrownBy(() -> levelService.updateLevel((short) 1, rq)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("already exists");
        verify(levelRepository, never()).save(any());
    }

    @Test
    void updateLevel_throw_whenLevelNotFound(){
        LevelRequest req = validRequest();
        when(levelRepository.findById((short) 1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> levelService.updateLevel((short) 1, req)).isInstanceOf(RuntimeException.class).hasMessageContaining("not found");
    }
}