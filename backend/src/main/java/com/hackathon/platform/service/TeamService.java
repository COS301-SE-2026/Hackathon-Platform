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

    @Transactional
    public void approveOrRejectJoinRequest(UUID teamId, UUID userIdToApprove, UUID currentUserId, boolean approve) {
    
        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new RuntimeException("Current user not in team"));
        if (!"APPROVED".equals(currentMember.getStatus())) {
            throw new RuntimeException("Only approved team members can approve/reject requests");
        }

    
        TeamMember pendingRequest = teamMemberRepository.findByTeamIdAndUserId(teamId, userIdToApprove)
            .orElseThrow(() -> new RuntimeException("Join request not found"));
        if (!"PENDING".equals(pendingRequest.getStatus())) {
            throw new RuntimeException("Request already processed");
        }

    
        if (approve) {
            long currentSize = teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED");
            int limit = 4; 
            if (currentSize >= limit) {
                throw new RuntimeException("Team is full");
            }
            pendingRequest.setStatus("APPROVED");
        } else {
            pendingRequest.setStatus("REJECTED");
        }
        teamMemberRepository.save(pendingRequest);
}

    @Transactional
    public void leaveTeam(UUID teamId, UUID currentUserId) {
    
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new RuntimeException("User not in team"));

        
        if ("APPROVED".equals(membership.getStatus())) {
            membership.setStatus("LEFT");
        } else if ("PENDING".equals(membership.getStatus())) {
            
            teamMemberRepository.delete(membership);
            return; 
        } else {
            throw new RuntimeException("Cannot leave with current status: " + membership.getStatus());
        }

        teamMemberRepository.save(membership);

        
        long approvedCount = teamMemberRepository.countByTeamIdAndStatus(teamId, "APPROVED");
        if (approvedCount == 0) {
            Team team = teamRepository.findById(teamId).orElseThrow();
            team.setStatus("INACTIVE");
            teamRepository.save(team);
        }
}

}