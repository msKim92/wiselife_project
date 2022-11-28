package be.wiselife.challenge.controller;

import be.wiselife.challenge.dto.ChallengeDto;
import be.wiselife.challenge.entity.Challenge;
import be.wiselife.challenge.mapper.ChallengeMapper;
import be.wiselife.challenge.service.ChallengeService;
import be.wiselife.challengereview.mapper.ChallengeReviewMapper;
import be.wiselife.challengetalk.mapper.ChallengeTalkMapper;
import be.wiselife.dto.MultiResponseDto;
import be.wiselife.dto.SingleResponseDto;
import be.wiselife.image.service.ImageService;
import be.wiselife.member.entity.Member;
import be.wiselife.member.service.MemberService;
import be.wiselife.memberchallenge.service.MemberChallengeService;
import org.hibernate.validator.constraints.Range;
import be.wiselife.memberchallenge.repository.MemberChallengeRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/challenges")
@Validated
public class ChallengeController {
    private final MemberService memberService;
    private final ChallengeService challengeService;
    private final ChallengeTalkMapper challengeTalkMapper;
    private final ChallengeMapper challengeMapper;
    private final ChallengeReviewMapper challengeReviewMapper;
    private final MemberChallengeRepository memberChallengeRepository;
    private final MemberChallengeService memberChallengeService;
    private final ImageService imageService;

    public ChallengeController(MemberService memberService, ChallengeService challengeService, ChallengeTalkMapper challengeTalkMapper, ChallengeMapper challengeMapper, ChallengeReviewMapper challengeReviewMapper, MemberChallengeRepository memberChallengeRepository, MemberChallengeService memberChallengeService, ImageService imageService) {
        this.memberService = memberService;
        this.challengeService = challengeService;
        this.challengeTalkMapper = challengeTalkMapper;
        this.challengeMapper = challengeMapper;
        this.challengeReviewMapper = challengeReviewMapper;
        this.memberChallengeRepository = memberChallengeRepository;
        this.memberChallengeService = memberChallengeService;
        this.imageService = imageService;
    }

    /**
     * 챌린지 생성
     *
     * @param challengePostDto 생성할 챌린지 관련 정보
     * @param request          챌린지 생성하려는 멤버의 token 값 받기 위해 필요
     * @param exampleImage     예시사진들
     * @return
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity postChallenge(@Valid @RequestPart(value = "post") ChallengeDto.Post challengePostDto,
                                        @RequestPart(value = "example", required = false) List<MultipartFile> exampleImage,
                                        @RequestPart(value = "rep", required = false) MultipartFile repImage,
                                        HttpServletRequest request) throws IOException {

        Challenge challenge = challengeMapper.challengePostDtoToChallenge(challengePostDto);
        challenge = challengeService.createChallenge(challenge, memberService.getLoginMember(request), repImage, exampleImage);

        return new ResponseEntity<>(
                new SingleResponseDto<>(challengeMapper.challengeToChallengeSimpleResponseDto(challenge, challengeReviewMapper))
                , HttpStatus.CREATED);
    }

    /**
     * 챌린지 수정
     *
     * @param challengeId       CHALLENGE 테이블 PK
     * @param challengePatchDto 수정할 챌린지 관련 정보
     * @param request           챌린지 수정하려는 멤버의 token 값 받기 위해 필요
     * @return
     */
    @PatchMapping(value = "/{challenge-id}", consumes = {"multipart/form-data"})
    public ResponseEntity patchChallenge(@PathVariable("challenge-id") @Positive Long challengeId,
                                         @Valid @RequestPart(value = "patch") ChallengeDto.Patch challengePatchDto,
                                         @RequestPart(value = "example", required = false) List<MultipartFile> exampleImage,
                                         @RequestPart(value = "rep", required = false) MultipartFile repImage,
                                         HttpServletRequest request) throws IOException {

        Challenge challenge = challengeMapper.challengePatchDtoToChallenge(challengePatchDto);

        challenge = challengeService.updateChallenge(challenge, memberService.getLoginMember(request), challengeId, exampleImage, repImage);

        return new ResponseEntity<>(
                new SingleResponseDto<>(challengeMapper.challengeToChallengeSimpleResponseDto(challenge, challengeReviewMapper))
                , HttpStatus.OK);
    }

    /**
     * MemberChallenge 생성
     * 멤버가 챌린지 참가하면 만들어지는 MEMBER와 CHALLENGE의 중간테이블
     *
     * @param challengeId CHALLENGE 테이블 PK
     * @param request
     * @return
     */
    @PostMapping("/participate/{challengeId}")
    public ResponseEntity postMemberAndChallenge(@PathVariable("challengeId") @Positive Long challengeId,
                                                 HttpServletRequest request) {

        Challenge challengeFromRepository = challengeService.findChallengeById(challengeId);

        Challenge challenge = challengeService.participateChallenge(challengeFromRepository, memberService.getLoginMember(request));
        return new ResponseEntity<>(
                new SingleResponseDto<>(challengeMapper.
                        challengeToChallengeDetailResponseDto(challenge, challengeTalkMapper, memberService, challengeReviewMapper, memberService.getLoginMember(request), memberChallengeService))
                , HttpStatus.CREATED);
    }

    /**
     * 작성자 : 유현
     * 수정자 : 민섭
     * 인증사진 등록
     *
     * @param challengeId   기존 Dto 삭제후, id를 받아오는걸로 변경
     * @param multipartFile 인증할 사진 받아오기
     * @param request       로그인한 사람의 이메일 정보를 가져오기위한 인자값
     *                                                                   TODO :
     *                                                                    챌린지 참여인원인지 판단하는 로직 추가
     *                                                                    응답값을 "/challenges/{challenge-id}으로 리다이렉션되게 개선 필요
     *                멘토님 조언
     *                서버에서도 인증사진을 등록하는 것에 대한 유효성 검사가 필요하다.
     *                사진을 동시에 등록하는 것에 대한 대책 마련
     *                사진을 줄이는 리사이징에 대한 고민

     */
    @PatchMapping(value = "/cert/{challenge-id}", consumes = {"multipart/form-data"})
    public ResponseEntity patchMemberCertification(@Valid @PathVariable("challenge-id") @Positive Long challengeId,
                                                   @RequestPart(value = "cert", required = false) MultipartFile multipartFile,
                                                   HttpServletRequest request) throws IOException {
        //메서드의 호출을 줄인다.AOP, ArgumentResolver 고민 횡단관심사로 빼서 역할 위임하기/접근권한을 두기(멘토님 조언)
        Challenge challenge = challengeService.updateCertImage(challengeId, memberService.getLoginMember(request), multipartFile);

        return new ResponseEntity<>(
                new SingleResponseDto<>(challengeMapper.challengeToChallengeDetailResponseDto(challenge, challengeTalkMapper, memberService, challengeReviewMapper, memberService.getLoginMember(request), memberChallengeService))
                , HttpStatus.CREATED);
    }

    /**
     * 작성자 : 유현
     * 챌린지 상세페이지 조회(팀원들하고 상의해야하는 부분)
     * 로그인 된 유저가 아닐시 인증사진은 안나오게 simpleResponse로 응답을 준다.
     * 로그인 된 유저면 자신이 인증한 사진만 볼 수 있게 detailResponse를 응답해 준다.
     *    TODO
     *     1) 만약 유저가 해당 챌린지 참여중이라면 별도로 유저의 해당 챌린지 성공률도 표시함
     *     2) 챌린지 참여중인 유저들의 평균 챌린지 성공률
     *     3) 동일한 사용자의 조회수 중복 증가 방지 기능
     */
    @GetMapping("/{challenge-id}")
    public ResponseEntity getChallenge(@PathVariable("challenge-id") @Positive Long challengeId,
                                       HttpServletRequest request) {
        Challenge challenge = challengeService.findChallengeById(challengeId); // 찾아왔는데 왜 패스값이 없냐? DB상에는있는데...
        challenge = challengeService.updateViewCount(challenge);

        if (request.getHeader("Authorization") == null ||
                memberChallengeService.findMemberChallengeByMemberAndChallenge(challenge, memberService.getLoginMember(request)) == null) {

            ChallengeDto.SimpleResponse challengeResponseDto
                    = challengeMapper.challengeToChallengeSimpleResponseDto(challenge, challengeReviewMapper);

            return new ResponseEntity<>(
                    new SingleResponseDto<>(challengeResponseDto), HttpStatus.OK);
        } else {

            ChallengeDto.DetailResponse challengeResponseDto
                    = challengeMapper.challengeToChallengeDetailResponseDto(challenge, challengeTalkMapper, memberService, challengeReviewMapper, memberService.getLoginMember(request), memberChallengeService);

            return new ResponseEntity<>(
                    new SingleResponseDto<>(challengeResponseDto), HttpStatus.OK);
        }
    }

    /**
     * 챌린지 삭제
     *
     * @param challengeId
     * @param request
     * @return TODO: 챌린지가 시작했다면 챌린지 작성자라도 수정 불가능하게
     */
    @DeleteMapping({"/{challenge-id}"})
    public ResponseEntity deleteChallenge(@PathVariable("challenge-id") @Positive Long challengeId,
                                          HttpServletRequest request) {

        challengeService.deleteChallenge(challengeId, memberService.getLoginMember(request));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 카테고리별 전체 챌린지 조회
     *
     * @param categoryId 카테고리에 해당하는 카테고리 id
     * @param sortBy     paging 기준 1.newest(=최신순) 2.popularity(=인기순)
     * @param page       조회하고 싶은 페이지 숫자
     * @param size       한 페이지에 들어갈 챌린지 개수
     * @return 카테고리에 해당하는 챌린지들 list
     */
    @GetMapping("/all/{category-id}")
    public ResponseEntity getAllChallengesInCategory(@PathVariable("category-id") @Range(min = 0L, max = 3L) Long categoryId,
                                                     @RequestParam(value = "sort-by", defaultValue = "popularity") String sortBy,
                                                     @Positive @RequestParam(value = "page", defaultValue = "1") int page,
                                                     @Positive @RequestParam(value = "size", defaultValue = "10") int size) {

        Page<Challenge> pageInfo = challengeService.getAllChallengesInCategory(categoryId, page - 1, size, sortBy);
        List<ChallengeDto.SimpleResponse> challengeResponseDtoList = challengeMapper.challengeListToSimpleResponseDtoList(pageInfo.getContent(), challengeReviewMapper);

        return new ResponseEntity<>(
                new MultiResponseDto<>(challengeResponseDtoList, pageInfo),
                HttpStatus.OK);
    }

    /**
     * 검색 자동완성용 전체 챌린지 제목 조회
     *
     * @return 챌린지 제목 List
     */
    @GetMapping("/titles")
    public ResponseEntity getAllChallengeTitles() {

        List<Challenge> challengeList = challengeService.getAllChallenges();
        List<ChallengeDto.ChallengeTitleResponse> challengeTitleResponseList = challengeMapper.challengeListToChallengeTitleResponseList(challengeList);

        return new ResponseEntity<>(
                new SingleResponseDto<>(challengeTitleResponseList)
                , HttpStatus.OK);
    }

    /**
     * 챌린지 제목을 통한 검색
     *
     * @param searchTitle 검색어
     * @param sortBy      paging 기준 1.newest(=최신순) 2.popularity(=인기순)
     * @param page        조회하고 싶은 페이지 숫자
     * @param size        한 페이지에 들어갈 챌린지 개수
     * @return
     */
    @GetMapping(value = "/search")
    public ResponseEntity searchChallengesByChallengeTitle(@RequestParam("searchTitle") String searchTitle,
                                                           @RequestParam(value = "sort-by", defaultValue = "popularity") String sortBy,
                                                           @Positive @RequestParam(value = "page", defaultValue = "1") int page,
                                                           @Positive @RequestParam(value = "size", defaultValue = "10") int size) {

        Page<Challenge> pageInfo = challengeService.searchChallengesByChallengeTitle(searchTitle, page - 1, size, sortBy);
        List<ChallengeDto.SimpleResponse> challengeResponseDtoList = challengeMapper.challengeListToSimpleResponseDtoList(pageInfo.getContent(), challengeReviewMapper);

        return new ResponseEntity<>(
                new MultiResponseDto<>(challengeResponseDtoList, pageInfo), HttpStatus.OK);
    }


}