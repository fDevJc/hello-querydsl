package hello.querydsl.controller;

import hello.querydsl.dto.MemberSearchCondition;
import hello.querydsl.dto.MemberTeamDto;
import hello.querydsl.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class MemberController {
    private final MemberJpaRepository memberJpaRepository;

    //http://localhost:8080/v1/members?teamName=teamA&ageGoe=30
    @GetMapping("/v1/members")
    public List<MemberTeamDto> members(MemberSearchCondition condition) {
        return memberJpaRepository.search(condition);
    }
}
