package com.emin.roomsplit.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.emin.roomsplit.user.User;
import com.emin.roomsplit.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JoinCodeGenerator joinCodeGenerator;

    @InjectMocks
    private GroupService groupService;

    private User korisnik(long id, String ime) {
        User user = new User(ime.toLowerCase() + "@example.com", "HASH", ime);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Group grupa(long id, String naziv, String kod, User kreator) {
        Group group = new Group(naziv, kod, kreator);
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    @Test
    void kreiranjeGrupeDajeKreatoruUloguOwner() {
        User ana = korisnik(1L, "Ana");
        AtomicReference<GroupMember> spremljeniClan = new AtomicReference<>();
        when(userRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(joinCodeGenerator.generate()).thenReturn("ABCD2345");
        when(groupRepository.existsByJoinCode("ABCD2345")).thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> {
            Group g = inv.getArgument(0);
            ReflectionTestUtils.setField(g, "id", 10L);
            return g;
        });
        when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(inv -> {
            spremljeniClan.set(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(groupMemberRepository.findAllByGroupIdWithUser(10L))
                .thenAnswer(inv -> List.of(spremljeniClan.get()));

        GroupDetailsResponse response = groupService.create(1L, new CreateGroupRequest("  Stan  "));

        assertThat(response.name()).isEqualTo("Stan");
        assertThat(response.joinCode()).isEqualTo("ABCD2345");
        assertThat(spremljeniClan.get().getRole()).isEqualTo(GroupRole.OWNER);
        assertThat(response.members()).singleElement()
                .satisfies(clan -> assertThat(clan.userId()).isEqualTo(1L));
    }

    @Test
    void kodSeGenerisePonovoAkoJeZauzet() {
        User ana = korisnik(1L, "Ana");
        when(userRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(joinCodeGenerator.generate()).thenReturn("ZAUZET22", "SLOBODAN");
        when(groupRepository.existsByJoinCode("ZAUZET22")).thenReturn(true);
        when(groupRepository.existsByJoinCode("SLOBODAN")).thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));
        when(groupMemberRepository.findAllByGroupIdWithUser(any())).thenReturn(List.of());

        GroupDetailsResponse response = groupService.create(1L, new CreateGroupRequest("Stan"));

        assertThat(response.joinCode()).isEqualTo("SLOBODAN");
    }

    @Test
    void pridruzivanjeSaIspravnimKodomDodajeClanaSUlogomMember() {
        User ana = korisnik(1L, "Ana");
        User emir = korisnik(2L, "Emir");
        Group stan = grupa(10L, "Stan", "ABCD2345", ana);
        when(groupRepository.findByJoinCode("ABCD2345")).thenReturn(Optional.of(stan));
        when(groupMemberRepository.existsByIdGroupIdAndIdUserId(10L, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(emir));
        when(groupMemberRepository.saveAndFlush(any(GroupMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(groupMemberRepository.findAllByGroupIdWithUser(10L)).thenReturn(List.of(
                new GroupMember(stan, ana, GroupRole.OWNER),
                new GroupMember(stan, emir, GroupRole.MEMBER)));

        // Kod se unosi ručno, pa se razmaci i mala slova moraju tolerisati
        GroupDetailsResponse response = groupService.join(2L, new JoinGroupRequest(" abcd2345 "));

        ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(GroupRole.MEMBER);
        assertThat(response.members()).hasSize(2);
    }

    @Test
    void pridruzivanjeSNepostojecimKodomBacaGroupNotFound() {
        when(groupRepository.findByJoinCode("NEMA2222")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.join(2L, new JoinGroupRequest("NEMA2222")))
                .isInstanceOf(GroupNotFoundException.class);

        verify(groupMemberRepository, never()).saveAndFlush(any(GroupMember.class));
    }

    @Test
    void pridruzivanjeGrupeUKojojSiVecClanBacaAlreadyMember() {
        Group stan = grupa(10L, "Stan", "ABCD2345", korisnik(1L, "Ana"));
        when(groupRepository.findByJoinCode("ABCD2345")).thenReturn(Optional.of(stan));
        when(groupMemberRepository.existsByIdGroupIdAndIdUserId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> groupService.join(1L, new JoinGroupRequest("ABCD2345")))
                .isInstanceOf(AlreadyGroupMemberException.class);

        verify(groupMemberRepository, never()).saveAndFlush(any(GroupMember.class));
    }

    @Test
    void istovremenoPridruzivanjeKojeUhvatiBazaDajeAlreadyMember() {
        User ana = korisnik(1L, "Ana");
        User emir = korisnik(2L, "Emir");
        Group stan = grupa(10L, "Stan", "ABCD2345", ana);
        when(groupRepository.findByJoinCode("ABCD2345")).thenReturn(Optional.of(stan));
        when(groupMemberRepository.existsByIdGroupIdAndIdUserId(10L, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(emir));
        when(groupMemberRepository.saveAndFlush(any(GroupMember.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> groupService.join(2L, new JoinGroupRequest("ABCD2345")))
                .isInstanceOf(AlreadyGroupMemberException.class);
    }

    @Test
    void detaljiGrupeNeUcitavajuSeAkoKorisnikNijeClan() {
        when(groupMemberRepository.existsByIdGroupIdAndIdUserId(10L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> groupService.get(3L, 10L)).isInstanceOf(GroupNotFoundException.class);

        verify(groupRepository, never()).findById(any());
        verify(groupMemberRepository, never()).findAllByGroupIdWithUser(any());
    }

    @Test
    void clanVidiDetaljeGrupe() {
        User ana = korisnik(1L, "Ana");
        Group stan = grupa(10L, "Stan", "ABCD2345", ana);
        when(groupMemberRepository.existsByIdGroupIdAndIdUserId(10L, 1L)).thenReturn(true);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(stan));
        when(groupMemberRepository.findAllByGroupIdWithUser(10L))
                .thenReturn(List.of(new GroupMember(stan, ana, GroupRole.OWNER)));

        GroupDetailsResponse response = groupService.get(1L, 10L);

        assertThat(response.name()).isEqualTo("Stan");
        assertThat(response.members()).hasSize(1);
    }

    @Test
    void mojeGrupeVracajuNazivIUlogu() {
        User ana = korisnik(1L, "Ana");
        Group stan = grupa(10L, "Stan", "ABCD2345", ana);
        when(groupMemberRepository.findAllByUserIdWithGroup(1L))
                .thenReturn(List.of(new GroupMember(stan, ana, GroupRole.OWNER)));

        List<GroupSummaryResponse> grupe = groupService.myGroups(1L);

        assertThat(grupe).singleElement().satisfies(g -> {
            assertThat(g.id()).isEqualTo(10L);
            assertThat(g.name()).isEqualTo("Stan");
            assertThat(g.role()).isEqualTo(GroupRole.OWNER);
        });
    }
}
