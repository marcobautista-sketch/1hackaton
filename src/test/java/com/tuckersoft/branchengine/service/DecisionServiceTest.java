package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.DecisionRequest;
import com.tuckersoft.branchengine.dto.DecisionResponse;
import com.tuckersoft.branchengine.entity.Playthrough;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import com.tuckersoft.branchengine.repository.UserRepository;
import com.tuckersoft.branchengine.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de DecisionService con Mockito, sin PostgreSQL ni red:
 * todos los repositorios y el publicador de eventos estan mockeados.
 */
@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock
    private DecisionRepository decisionRepository;
    @Mock
    private PlaythroughRepository playthroughRepository;
    @Mock
    private StoryNodeRepository storyNodeRepository;
    @Mock
    private RealityLogRepository realityLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DecisionService decisionService;
    // CurrentUserProvider es una clase concreta: Mockito (bytebuddy) no puede
    // mockearla en JDK 25, asi que se instancia real con su repositorio mockeado.
    private CurrentUserProvider currentUserProvider;

    private User owner;
    private StoryNode sourceNode;
    private StoryNode targetNode;
    private Playthrough playthrough;

    @BeforeEach
    void setUp() {
        currentUserProvider = new CurrentUserProvider(userRepository);
        decisionService = new DecisionService(decisionRepository, playthroughRepository,
                storyNodeRepository, realityLogRepository, currentUserProvider, eventPublisher);

        owner = new User();
        owner.setId(1L);
        owner.setEmail("qa@tuckersoft.test");
        owner.setDisplayName("Ada Lovelace");
        owner.setRole("ROLE_USER");

        sourceNode = new StoryNode();
        sourceNode.setId(10L);
        sourceNode.setNodeCode("NODE-CEREAL");
        sourceNode.setPrimaryBranchCode("NODE-BUS");
        sourceNode.setGlitchBranchCode("NODE-ESPEJO");

        targetNode = new StoryNode();
        targetNode.setId(11L);
        targetNode.setNodeCode("NODE-BUS");

        playthrough = new Playthrough();
        playthrough.setId(100L);
        playthrough.setUser(owner);
        playthrough.setPlayerTag("STEFAN-01");
        playthrough.setStartNodeCode("NODE-CEREAL");
        playthrough.setCurrentNode(sourceNode);
        playthrough.setLucidity(100);
        playthrough.setControlLevel(0);
        playthrough.setStatus("ACTIVA");
        playthrough.setCreatedAt(Instant.now());
        playthrough.setUpdatedAt(Instant.now());
    }

    private DecisionRequest requestFor(String rawInput, String impactLevel) {
        DecisionRequest request = new DecisionRequest();
        request.setPlaythroughId(playthrough.getId());
        request.setRawInput(rawInput);
        request.setImpactLevel(impactLevel);
        return request;
    }

    @Test
    void reglaDePrecedencia_destruyeLaCamara_esRupturaDeCuartaPared() {
        when(playthroughRepository.findById(playthrough.getId())).thenReturn(Optional.of(playthrough));
        when(storyNodeRepository.findByNodeCode("NODE-ESPEJO")).thenReturn(Optional.of(targetNode));

        DecisionResponse response = decisionService.create(
                requestFor("Stefan destruye la camara", "LEVE"), owner, false);

        assertEquals("RUPTURA_CUARTA_PARED", response.getBranchType(),
                "La regla 2 (RUPTURA_CUARTA_PARED) gana sobre la regla 4 (REBELDIA) porque se evalua antes.");
    }

    @Test
    void entradaCorrupta_noModificaLaPartidaYNoPublicaEvento() {
        when(playthroughRepository.findById(playthrough.getId())).thenReturn(Optional.of(playthrough));

        DecisionResponse response = decisionService.create(
                requestFor("%%% 0101 ### @@", "CRITICO"), owner, false);

        assertEquals("ENTRADA_CORRUPTA", response.getBranchType());
        assertEquals(100, response.getLucidity(), "La entrada corrupta no toca lucidity.");
        assertEquals(0, response.getControlLevel(), "La entrada corrupta no toca controlLevel.");
        verify(eventPublisher, never()).publishEvent(any());
        verify(playthroughRepository, never()).save(any());
    }

    @Test
    void impactoCritico_bajaLucidez40YSubeControl45SinPasarseDeLosLimites() {
        when(playthroughRepository.findById(playthrough.getId())).thenReturn(Optional.of(playthrough));
        when(storyNodeRepository.findByNodeCode("NODE-ESPEJO")).thenReturn(Optional.of(targetNode));

        DecisionResponse response = decisionService.create(
                requestFor("Stefan acepta la oferta y sigue el guion previsto.", "CRITICO"), owner, false);

        assertEquals(60, response.getLucidity(), "100 - 40 = 60");
        assertEquals(45, response.getControlLevel(), "0 + 45 = 45");
    }

    @Test
    void controlLevel100TerminaConPacSymbol_aunqueLucidezTambienLleguéACero() {
        playthrough.setLucidity(40);
        playthrough.setControlLevel(90);
        when(playthroughRepository.findById(playthrough.getId())).thenReturn(Optional.of(playthrough));

        DecisionResponse response = decisionService.create(
                requestFor("Stefan acepta la oferta y sigue el guion previsto.", "CRITICO"), owner, false);

        assertEquals(0, response.getLucidity(), "40 - 40 = 0, tambien dispara ENDING_WHITE_BEAR");
        assertEquals(100, response.getControlLevel(), "90 + 45 topado en 100");
        assertEquals("ENDING_PAC_SYMBOL", response.getEndingCode(),
                "controlLevel >= 100 se evalua primero: gana ENDING_PAC_SYMBOL.");
        assertEquals("FINALIZADA", response.getPlaythroughStatus());
        verify(storyNodeRepository, never()).findByNodeCode(any());
    }

    @Test
    void publishEvent_seLlamaUnaVezEnDecisionNormalYCeroVecesEnEntradaCorrupta() {
        when(playthroughRepository.findById(playthrough.getId())).thenReturn(Optional.of(playthrough));
        when(storyNodeRepository.findByNodeCode("NODE-BUS")).thenReturn(Optional.of(targetNode));

        decisionService.create(requestFor("Stefan acepta la oferta de Mohan.", "LEVE"), owner, false);
        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));

        reset(eventPublisher);
        playthrough.setLucidity(100);
        playthrough.setControlLevel(0);
        decisionService.create(requestFor("%%% 0101 ### @@", "LEVE"), owner, false);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
