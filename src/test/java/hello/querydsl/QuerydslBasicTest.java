package hello.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import hello.querydsl.entity.Member;
import hello.querydsl.entity.QMember;
import hello.querydsl.entity.QTeam;
import hello.querydsl.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static hello.querydsl.entity.QMember.*;
import static hello.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.*;

@Transactional
@SpringBootTest
public class QuerydslBasicTest {
    @PersistenceContext
    EntityManager em;

    JPAQueryFactory query;

    @BeforeEach
    public void beforeEach() {
        //given
        query = new JPAQueryFactory(em);
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
    }

    @Test
    void startJPQL() {
        //memberA을 찾아라
        Member memberA = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "memberA")
                .getSingleResult();

        assertThat(memberA.getUsername()).isEqualTo("memberA");
    }
    @Test
    void startQuerydsl() {
        //memberA을 찾아라
//        QMember m = new QMember("m");
        QMember m = member;

        Member memberA = query
                .select(m)
                .from(m)
                .where(m.username.eq("memberA"))
                .fetchOne();

        assertThat(memberA.getUsername()).isEqualTo("memberA");
    }

    @Test
    void startQuerydsl1() {
        //memberA을 찾아라
        Member memberA = query
                .select(member)
                .from(member)
                .where(member.username.eq("memberA"))
                .fetchOne();
        assertThat(memberA.getUsername()).isEqualTo("memberA");
    }
    @Test
    void querydslSearch() {
        Member memberA = query
                .selectFrom(member)
//                .where(member.username.eq("memberA").and(member.age.eq(10)))
                .where(
                        member.username.eq("memberA"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(memberA.getUsername()).isEqualTo("memberA");
    }
    @Test
    void querydslResultSearch() {

    }

    @Test
    void querydslSort() {
        List<Member> members = query
                .selectFrom(member)
                .orderBy(member.age.desc())
                .fetch();

        for (Member member : members) {
            System.out.println("member = " + member);
        }
    }

    @Test
    void querydslPaging() {
        List<Member> members = query
                .selectFrom(member)
                .orderBy(member.age.desc())
                .offset(1)
                .limit(1)
                .fetch();

        for (Member member : members) {
            System.out.println("member = " + member);
        }
    }

    @Test
    void querydslGroup() {
        List<Tuple> fetch = query
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.max(),
                        member.age.min(),
                        member.age.avg()
                )
                .from(member)
                .fetch();
        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void querydslGroupBy() {
        //팀의 이름과 각 팀의 평균 연령을 구해라
        List<Tuple> fetch = query
                .select(team.name,
                        member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void querydslJoin() {
        //팀 A에 소속된 모든 회원
        List<Member> members = query
                .select(member)
                .from(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        for (Member member1 : members) {
            System.out.println("member1 = " + member1);
        }
        assertThat(members)
                .extracting("username")
                .containsExactly("memberA", "memberB");
    }

    @Test
    void querydslThetaJoin() {
        //회원의 이름과 팀이름이 같은 회원 조회
        em.persist(new Member("teamA", 10, null));
        em.persist(new Member("teamB", 10, null));

        List<Member> fetch = query
                .select(member)
                .from(member,team)
                .where(member.username.eq(team.name))
                .fetch();

        for (Member fetch1 : fetch) {
            System.out.println("fetch1 = " + fetch1);
        }
    }
    
    @Test
    void querydslJoinOn() {
        //회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
//        List<Tuple> teamA = query
//                .select(member, team)
//                .from(member)
//                .leftJoin(member.team, team)
//                .where(team.name.eq("teamA"))
//                .fetch();
        List<Tuple> teamA = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : teamA) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void querydslJoinOn_NoRelation() {
        //회원의 이름과 팀이름이 같은 회원 조회
        em.persist(new Member("teamA", 10, null));
        em.persist(new Member("teamB", 10, null));

        List<Tuple> fetch = query
                .select(member, team)
                .from(member)
                .join(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void querydslFetchJoin_No() {
        em.flush();
        em.clear();

        Member memberA = query
                .select(member)
                .from(member)
                .join(member.team, team)
                .where(member.username.eq("memberA"))
                .fetchOne();

        System.out.println("memberA = " + memberA);

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(memberA.getTeam());  //영속성컨텍스트에 들어와있는지 아닌지 확인가능
        System.out.println("loaded = " + loaded);

        assertThat(loaded).as("페치조인 미적용")
                .isFalse();
    }

    @Test
    void querydslFetchJoin() {
        em.flush();
        em.clear();

        Member memberA = query
                .select(member)
                .from(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("memberA"))
                .fetchOne();

        System.out.println("memberA = " + memberA);

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(memberA.getTeam());  //영속성컨텍스트에 들어와있는지 아닌지 확인가능
        System.out.println("loaded = " + loaded);

        assertThat(loaded).as("페치조인 적용")
                .isTrue();
    }

    @Test
    void querySubQuery() {
        //나이가 가장 많은 회원을 조회
        QMember memberSub = new QMember("memberSub");

        List<Member> fetch = query
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        for (Member fetch1 : fetch) {
            System.out.println("fetch1 = " + fetch1);
        }
    }

    @Test
    void querySubQuery1() {
        //나이가 평균이상인 회원
        QMember memberSub = new QMember("memberSub");

        List<Member> fetch = query
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        for (Member fetch1 : fetch) {
            System.out.println("fetch1 = " + fetch1);
        }
    }

    @Test
    void querySubQuery2() {
        //나이가 10, 20인 in 절
        QMember memberSub = new QMember("memberSub");

        List<Member> fetch = query
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.loe(20))
                ))
                .fetch();

        for (Member fetch1 : fetch) {
            System.out.println("fetch1 = " + fetch1);
        }
    }

    @Test
    void querydslSelectSubQuery() {
        QMember memberSub = new QMember("memberSub");
        List<Tuple> fetch = query
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        //jpa , jpql 서브쿼리의 한계점으로 from 절에서 서브쿼리(인라인뷰)는 지원하지 않는다.
        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void querydslCase() {
        List<String> fetch = query
                .select(
                        member.age
                                .when(10).then("열살")
                                .when(20).then("스무살")
                                .when(30).then("서른살")
                                .otherwise("그외")
                )
                .from(member)
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }
    
    @Test
    void querydslCase2() {
        List<String> fetch = query
                .select(
                        new CaseBuilder()
                                .when(member.age.between(0, 20)).then("0~20")
                                .when(member.age.between(21,30)).then("21~30")
                                .otherwise("기타")
                )
                .from(member)
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }
    
    @Test
    void querydslConstant() {
        List<Tuple> a = query
                .select(member.username,
                        Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : a) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void querydslConcat() {
        List<String> fetch = query
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }
}
