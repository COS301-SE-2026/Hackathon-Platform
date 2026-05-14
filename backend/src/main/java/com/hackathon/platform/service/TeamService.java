package com.hackathon.platform.service;

import com.hackathon.platform.dto.CreateTeamRequest;
import com.hackathon.platform.dto.TeamResponse;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.model.TeamMember;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.repository.TeamMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

   
    public TeamService(TeamRepository teamRepository, TeamMemberRepository teamMemberRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request, UUID currentUserId) {
        
        Team team = new Team();
        team.setTeamName(request.getTeamName());
        team.setEventId(request.getEventId());
        team.setCreatedByUserId(currentUserId);
        team.setStatus("ACTIVE");
        Team savedTeam = teamRepository.save(team);

       
        TeamMember member = new TeamMember();
        member.setTeamId(savedTeam.getTeamId());
        member.setUserId(currentUserId);
        member.setStatus("APPROVED");
        teamMemberRepository.save(member);

        
        TeamResponse response = new TeamResponse();
        response.setTeamId(savedTeam.getTeamId());
        response.setTeamName(savedTeam.getTeamName());
        response.setEventId(savedTeam.getEventId());
        response.setCreatedByUserId(savedTeam.getCreatedByUserId());
        response.setCreatedAt(savedTeam.getCreatedAt());
        response.setStatus(savedTeam.getStatus());
        return response;
    }


    @Transactional
    public void requestToJoinTeam(UUID teamId, UUID currentUserId) {

        Team team = teamRepository.findById(teamId)
        .orElseThrow(() -> new RuntimeException("Team not found"));

        if (teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId).isPresent()) {
        throw new RuntimeException("Already requested or member");
        }

        long approvedCount = teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED");
        int limit = 4; 
        if (approvedCount >= limit) {
            throw new RuntimeException("Team is full");
        }

        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(currentUserId);
        member.setStatus("PENDING");
        teamMemberRepository.save(member);
}


}