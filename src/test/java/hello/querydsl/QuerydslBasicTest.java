package hello.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import hello.querydsl.dto.MemberDto;
import hello.querydsl.dto.QMemberDto;
import hello.querydsl.dto.UserDto;
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

    @Test
    void simpleProjection() {

        List<String> fetch = query
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }

    }
    @Test
    void tupleProjection() {
        List<Tuple> fetch = query
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
            System.out.println("tuple.get(member.username) = " + tuple.get(member.username));
            System.out.println("tuple.get(member.age) = " + tuple.get(member.age));
        }
    }
    
    @Test
    void findDto_JPQL() {
        List<MemberDto> resultList = em.createQuery(
                        "select " +
                                "new hello.querydsl.dto.MemberDto(m.username, m.age) " +
                                "from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDto_querydsl_setter() {
        List<MemberDto> fetch = query
                .select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDto_querydsl_field() {
        //getter, setter 없어도 됨
        List<MemberDto> fetch = query
                .select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDto_querydsl_field2() {
        //getter, setter 없어도 됨
        List<UserDto> fetch = query
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), //DTO와 변수명이 다를경우 alias를 줘서 맞춰주면된다.
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : fetch) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findDto_querydsl_field3() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> fetch = query
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), //DTO와 변수명이 다를경우 alias를 줘서 맞춰주면된다.
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(memberSub.age.avg().intValue())
                                        .from(memberSub)
                                , "age")))
                .from(member)
                .fetch();

        for (UserDto userDto : fetch) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findDto_querydsl_constructor() {
        //getter, setter 없어도 됨
        List<MemberDto> fetch = query
                .select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDto_querydsl_constructor2() {
        //getter, setter 없어도 됨
        List<UserDto> fetch = query
                .select(Projections.constructor(UserDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (UserDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
        장점:
            간단하다
            컴파일에러로 잡을수 있는 에러가 있다.
        단점:
            dto -> querydsl에 의존하는 문제
            q파일을 생성
     */
    @Test
    void findDto_queryProjection() {
        List<MemberDto> fetch = query
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @Test
    void distinct() {
        List<Member> fetch = query
                .select(member).distinct()
                .from(member)
                .fetch();

        for (Member fetch1 : fetch) {
            System.out.println("fetch1 = " + fetch1);
        }
    }

    @Test
    void dynamicQuery_BooleanBuilder() {
        String usernameParam = "memberA";
        Integer ageParam = 10;

        List<Member> result = searchMember1(null, ageParam);
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return query
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    void dynamicQuery_WhereParam() {
        String usernameParam = "memberA";
        Integer ageParam = 10;

        List<Member> result = searchMember2(null, ageParam);
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    /*
        메서드로 분리를 하였기 때문에 코드를 재활용 및 조립할 수 있다.
     */
    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return query
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        if (usernameCond == null) {
            return null;
        }
        return member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        if (ageCond == null) {
            return null;
        }
        return member.age.eq(ageCond);
    }
    //조립
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    void bulkUpdate() {
        List<Member> fetch = query
                .selectFrom(member)
                .fetch();
        for (Member fetch1 : fetch) {
            System.out.println("fetch1 = " + fetch1);
        }

        long count = query
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        System.out.println("count = " + count);

        /*
            flush, clear 없으면 영속성컨텍스트가 변경이안됨
            벌크연산후에는 이런부분 생각해야됨
         */
        em.flush();
        em.clear();

        List<Member> fetch2 = query
                .selectFrom(member)
                .fetch();
        for (Member member1 : fetch2) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    void bulkAdd() {
        query
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    void bulkDelete() {
        query
                .delete(member)
                .where(member.age.lt(19))
                .execute();
    }


    /*
        다른 function을 넣으려면 dialect에 따로 넣어줘야한다.
     */
    @Test
    void sqlFunction() {
        List<String> fetch = query
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "m"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void sqlFunction2() {
        String param = "MEMBERA";
        List<Member> fetch = query
                .selectFrom(member)
//                .where(member.username.eq(
//                                Expressions.stringTemplate("function('lower','{0}')" ,member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();
        for (Member fetch1 : fetch) {
            System.out.println("fetch1 = " + fetch1);
        }
    }
}


