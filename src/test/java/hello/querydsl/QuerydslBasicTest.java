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
        //memberA??? ?????????
        Member memberA = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "memberA")
                .getSingleResult();

        assertThat(memberA.getUsername()).isEqualTo("memberA");
    }
    @Test
    void startQuerydsl() {
        //memberA??? ?????????
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
        //memberA??? ?????????
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
        //?????? ????????? ??? ?????? ?????? ????????? ?????????
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
        //??? A??? ????????? ?????? ??????
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
        //????????? ????????? ???????????? ?????? ?????? ??????
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
        //????????? ?????? ???????????????, ??? ????????? teamA??? ?????? ??????, ????????? ?????? ??????
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
        //????????? ????????? ???????????? ?????? ?????? ??????
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

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(memberA.getTeam());  //???????????????????????? ?????????????????? ????????? ????????????
        System.out.println("loaded = " + loaded);

        assertThat(loaded).as("???????????? ?????????")
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

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(memberA.getTeam());  //???????????????????????? ?????????????????? ????????? ????????????
        System.out.println("loaded = " + loaded);

        assertThat(loaded).as("???????????? ??????")
                .isTrue();
    }

    @Test
    void querySubQuery() {
        //????????? ?????? ?????? ????????? ??????
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
        //????????? ??????????????? ??????
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
        //????????? 10, 20??? in ???
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

        //jpa , jpql ??????????????? ??????????????? from ????????? ????????????(????????????)??? ???????????? ?????????.
        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void querydslCase() {
        List<String> fetch = query
                .select(
                        member.age
                                .when(10).then("??????")
                                .when(20).then("?????????")
                                .when(30).then("?????????")
                                .otherwise("??????")
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
                                .otherwise("??????")
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
        //getter, setter ????????? ???
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
        //getter, setter ????????? ???
        List<UserDto> fetch = query
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), //DTO??? ???????????? ???????????? alias??? ?????? ??????????????????.
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
                        member.username.as("name"), //DTO??? ???????????? ???????????? alias??? ?????? ??????????????????.
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
        //getter, setter ????????? ???
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
        //getter, setter ????????? ???
        List<UserDto> fetch = query
                .select(Projections.constructor(UserDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (UserDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
        ??????:
            ????????????
            ?????????????????? ????????? ?????? ????????? ??????.
        ??????:
            dto -> querydsl??? ???????????? ??????
            q????????? ??????
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
        ???????????? ????????? ????????? ????????? ????????? ????????? ??? ????????? ??? ??????.
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
    //??????
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
                .set(member.username, "?????????")
                .where(member.age.lt(28))
                .execute();

        System.out.println("count = " + count);

        /*
            flush, clear ????????? ???????????????????????? ???????????????
            ????????????????????? ???????????? ???????????????
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
        ?????? function??? ???????????? dialect??? ?????? ??????????????????.
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


