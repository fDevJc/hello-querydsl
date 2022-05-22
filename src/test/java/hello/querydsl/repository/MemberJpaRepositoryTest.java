package hello.querydsl.repository;

import hello.querydsl.dto.MemberSearchCondition;
import hello.querydsl.dto.MemberTeamDto;
import hello.querydsl.entity.Member;
import hello.querydsl.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
class MemberJpaRepositoryTest {
    @PersistenceContext
    EntityManager em;
    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    void basicTest() {
        Member member = new Member("memberA", 10, null);
        memberJpaRepository.save(member);

        Member foundMember = memberJpaRepository.findById(member.getId()).get();

        assertThat(foundMember).isEqualTo(member);
    }

    @Test
    void basicQuerydslTest() {
        Member member = new Member("memberA", 10, null);
        memberJpaRepository.save(member);

        Member foundMember = memberJpaRepository.findById(member.getId()).get();

        assertThat(foundMember).isEqualTo(member);

        List<Member> members = memberJpaRepository.findByUsername_querydsl("memberA");
        assertThat(members).containsExactly(member);
        assertThat(members).extracting("username").containsExactly("memberA");
    }

    @Test
    void searchTest() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member memberA = new Member("memberA", 10, teamA);
        Member memberB = new Member("memberB", 20, teamA);
        Member memberC = new Member("memberC", 30, teamB);
        Member memberD = new Member("memberD", 40, teamB);
        em.persist(memberA);
        em.persist(memberB);
        em.persist(memberC);
        em.persist(memberD);

        MemberSearchCondition condition = new MemberSearchCondition();
//        condition.setUsername("memberA");
//        condition.setTeamName("teamA");
        condition.setAgeGoe(9);
        condition.setAgeLoe(30);

        List<MemberTeamDto> memberTeamDtos = memberJpaRepository.searchByBuilder(condition);

        for (MemberTeamDto memberTeamDto : memberTeamDtos) {
            System.out.println("memberTeamDto = " + memberTeamDto);
        }
    }

    @Test
    void searchTest2() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member memberA = new Member("memberA", 10, teamA);
        Member memberB = new Member("memberB", 20, teamA);
        Member memberC = new Member("memberC", 30, teamB);
        Member memberD = new Member("memberD", 40, teamB);
        em.persist(memberA);
        em.persist(memberB);
        em.persist(memberC);
        em.persist(memberD);

        MemberSearchCondition condition = new MemberSearchCondition();
//        condition.setUsername("memberA");
//        condition.setTeamName("teamA");
        condition.setAgeGoe(9);
        condition.setAgeLoe(30);

        List<MemberTeamDto> memberTeamDtos = memberJpaRepository.search(condition);

        for (MemberTeamDto memberTeamDto : memberTeamDtos) {
            System.out.println("memberTeamDto = " + memberTeamDto);
        }
    }
}