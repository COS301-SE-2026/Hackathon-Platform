// package com.hackathon.platform.controller;

// import static org.hamcrest.Matchers.hasSize;
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.hackathon.platform.dto.LevelRequest;
// import com.hackathon.platform.model.Hackathon;
// import com.hackathon.platform.model.Level;
// import com.hackathon.platform.model.Role;
// import com.hackathon.platform.model.User;
// import com.hackathon.platform.repository.HackathonRepository;
// import com.hackathon.platform.repository.RoleRepository;
// import com.hackathon.platform.repository.UserRepository;
// import java.util.List;
// import java.util.UUID;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.http.MediaType;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.test.web.servlet.MvcResult;
// import org.springframework.transaction.annotation.Transactional;

// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
// @AutoConfigureMockMvc
// @Transactional
// class LevelControllerTest {
//   @Autowired private MockMvc mockMvc;
//   @Autowired private ObjectMapper objectMapper;
//   @Autowired private UserRepository usrRep;
//   @Autowired private RoleRepository roleRep;
//   @Autowired private HackathonRepository hackrep;
//   private UsernamePasswordAuthenticationToken admin;
//   private UsernamePasswordAuthenticationToken part;
//   private UUID hackId;

//   @BeforeEach
//   void setUp() {
//     Role adR = Role.builder().roleId(1).name("ADMIN").build();
//     roleRep.saveAndFlush(adR);

//     Role pR = Role.builder().roleId(2).name("PARTICIPANT").build();
//     roleRep.saveAndFlush(pR);

//     User ad =
//         User.builder()
//             .userId(UUID.randomUUID())
//             .firstName("ShahRukh")
//             .lastName("Khan")
//             .email("srk@gmail.com")
//             .passwordHash("$2a$12$hashedpassword")
//             .role(adR)
//             .status("ACTIVE")
//             .build();
//     User svdAdmin = usrRep.saveAndFlush(ad);

//     User partici =
//         User.builder()
//             .userId(UUID.randomUUID())
//             .firstName("Virat")
//             .lastName("Kohli")
//             .email("vk@gmail.com")
//             .passwordHash("$2a$12$hashedpassword")
//             .role(pR)
//             .status("ACTIVE")
//             .build();
//     User svdP = usrRep.saveAndFlush(partici);

//     admin =
//         new UsernamePasswordAuthenticationToken(
//             svdAdmin, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
//     part =
//         new UsernamePasswordAuthenticationToken(
//             svdP, null, List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));

//     Hackathon hackathon = new Hackathon();
//     hackathon.setName("lvlController test");
//     Hackathon svdHackathon = hackrep.saveAndFlush(hackathon);
//     hackId = svdHackathon.getHackathonId();
//   }

//   private LevelRequest levelRequest(String name, int levelNum) {
//     LevelRequest req = new LevelRequest();
//     req.setName(name);
//     req.setLevelNumber((short) levelNum);
//     req.setDescription("descr");
//     return req;
//   }

//   @Test
//   void createLevel_asAdmin_return200() throws Exception {
//     mockMvc
//         .perform(
//             post("/api/hackathons/{hackathonId}/levels", hackId)
//                 .with(authentication(admin))
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(objectMapper.writeValueAsString(levelRequest("num1", 1))))
//         .andExpect(status().isOk())
//         .andExpect(jsonPath("$.name").value("num1"))
//         .andExpect(jsonPath("$.name").value("num1"));
//   }

//   @Test
//   void createLevel_asPart_return403() throws Exception {
//     mockMvc
//         .perform(
//             post("/api/hackathons/{hackathonId}/levels", hackId)
//                 .with(authentication(part))
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(objectMapper.writeValueAsString(levelRequest("num1", 1))))
//         .andExpect(status().isForbidden());
//   }

//   @Test
//   void getLevelForHackathon_returnOrderLevels() throws Exception {
//     mockMvc.perform(
//         post("/api/hackathons/{hackathonId}/levels", hackId)
//             .with(authentication(admin))
//             .contentType(MediaType.APPLICATION_JSON)
//             .content(objectMapper.writeValueAsString(levelRequest("num2", 2))));
//     mockMvc.perform(
//         post("/api/hackathons/{hackathonId}/levels", hackId)
//             .with(authentication(admin))
//             .contentType(MediaType.APPLICATION_JSON)
//             .content(objectMapper.writeValueAsString(levelRequest("num1", 1))));
//     mockMvc
//         .perform(get("/api/hackathons/{hackathonId}/levels", hackId).with(authentication(part)))
//         .andExpect(status().isOk())
//         .andExpect(jsonPath("$", hasSize(2)))
//         .andExpect(jsonPath("$[0].levelNumber").value(1))
//         .andExpect(jsonPath("$[1].levelNumber").value(2));
//   }

//   @Test
//   void updateLevel_asAdmin_return200() throws Exception {
//     MvcResult createRes =
//         mockMvc
//             .perform(
//                 post("/api/hackathons/{hackathonId}/levels", hackId)
//                     .with(authentication(admin))
//                     .contentType(MediaType.APPLICATION_JSON)
//                     .content(objectMapper.writeValueAsString(levelRequest("old", 1))))
//             .andExpect(status().isOk())
//             .andReturn();
//     Level created =
//         objectMapper.readValue(createRes.getResponse().getContentAsString(), Level.class);
//     LevelRequest updateReq = new LevelRequest();
//     updateReq.setName("new");
//     updateReq.setLevelNumber((short) 1);
//     updateReq.setDescription("new descr");
//     mockMvc
//         .perform(
//             put("/api/levels/{levelId}", created.getId())
//                 .with(authentication(admin))
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(objectMapper.writeValueAsString(updateReq)))
//         .andExpect(status().isOk())
//         .andExpect(jsonPath("$.name").value("new"));
//   }

//   @Test
//   void updateLevel_Participant_return403() throws Exception {
//     MvcResult createRes =
//         mockMvc
//             .perform(
//                 post("/api/hackathons/{hackathonId}/levels", hackId)
//                     .with(authentication(admin))
//                     .contentType(MediaType.APPLICATION_JSON)
//                     .content(objectMapper.writeValueAsString(levelRequest("lvl1", 1))))
//             .andExpect(status().isOk())
//             .andReturn();
//     Level creat = objectMapper.readValue(createRes.getResponse().getContentAsString(), Level.class);
//     mockMvc
//         .perform(
//             put("/api/levels/{levelId}", creat.getId())
//                 .with(authentication(part))
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(objectMapper.writeValueAsString(levelRequest("im tired of testing", 1))))
//         .andExpect(status().isForbidden());
//   }

//   @Test
//   void deleteLevel_asAdmin_return204() throws Exception {
//     MvcResult createRes =
//         mockMvc
//             .perform(
//                 post("/api/hackathons/{hackathonId}/levels", hackId)
//                     .with(authentication(admin))
//                     .contentType(MediaType.APPLICATION_JSON)
//                     .content(objectMapper.writeValueAsString(levelRequest("delte test", 1))))
//             .andExpect(status().isOk())
//             .andReturn();
//     Level newOne =
//         objectMapper.readValue(createRes.getResponse().getContentAsString(), Level.class);
//     mockMvc
//         .perform(delete("/api/levels/{levelId}", newOne.getId()).with(authentication(admin)))
//         .andExpect(status().isNoContent());
//   }

//   @Test
//   void delLvl_periticpant_return403() throws Exception {
//     MvcResult createRes =
//         mockMvc
//             .perform(
//                 post("/api/hackathons/{hackathonId}/levels", hackId)
//                     .with(authentication(admin))
//                     .contentType(MediaType.APPLICATION_JSON)
//                     .content(objectMapper.writeValueAsString(levelRequest("test", 1))))
//             .andExpect(status().isOk())
//             .andReturn();
//     Level newLvl =
//         objectMapper.readValue(createRes.getResponse().getContentAsString(), Level.class);
//     mockMvc
//         .perform(delete("/api/levels/{levelId}", newLvl.getId()).with(authentication(part)))
//         .andExpect(status().isForbidden());
//   }
// }
