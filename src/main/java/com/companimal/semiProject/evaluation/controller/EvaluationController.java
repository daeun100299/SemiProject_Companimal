package com.companimal.semiProject.evaluation.controller;

import com.companimal.semiProject.evaluation.model.dto.CalculationListDTO;
import com.companimal.semiProject.evaluation.model.dto.CreatorBusinessDTO;
import com.companimal.semiProject.evaluation.model.dto.CreatorEvaluationDetailDTO;
import com.companimal.semiProject.evaluation.model.dto.ProjectEvaluationDTO;
import com.companimal.semiProject.evaluation.model.service.CreatorEvaluationService;
import com.companimal.semiProject.evaluation.model.service.EvaluationService;
import com.companimal.semiProject.project.model.dto.CreatorInfoDTO;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final CreatorEvaluationService creatorEvaluationService;

    public EvaluationController(CreatorEvaluationService creatorEvaluationService, EvaluationService evaluationService) {
        this.creatorEvaluationService = creatorEvaluationService;
        this.evaluationService = evaluationService;
    }

    @GetMapping("/evacalculationlist")
    public String selectEvaCalculationList(Model model) {
        System.out.println("후원금 최종 정산 심사");

        List<CalculationListDTO> calculationList = evaluationService.selectEvaCalculationList();
        System.out.println("🎈🎈🎈🎈🎈🎈🎈🎈🎈" + calculationList.toString());

        model.addAttribute("calculationList", calculationList);

        return "contents/evaluation/evacalculationlist";
    }


    @GetMapping("/creatorEvaluationRegist")
    public String creatorEvaluationRegist() {
        return "/contents/evaluation/creatorEvaluationRegist";
    }

    @PostMapping("/creatorEvaluationRegist")
    public String creatorEvaluationRegist(@RequestParam MultipartFile creatorProductPlan
            , @RequestParam MultipartFile creatorProductPortfolio
            , @RequestParam MultipartFile creatorImg
            , @ModelAttribute CreatorInfoDTO creatorInfoDTO
            , Authentication authentication) throws IOException {

        // 현재 로그인 중인 아이디 추출 (심사 등록을 한 크리에이터)
        String creatorId = authentication.getName();

        // 크리에이터 심사 파일과 사업자 정보 삭제
        creatorEvaluationService.deleteCreFileAndBusinessInfo(creatorId);

        // 해당 아이디로 이미 등록된 크리에이터 정보가 있을 시 업데이트, 없으면 인서트
        if (creatorEvaluationService.selectCreatorInfo(creatorId)) {
            creatorEvaluationService.updateCreatorInfo(creatorProductPlan, creatorProductPortfolio, creatorImg, creatorInfoDTO, creatorId);
            System.out.println("true");
        } else {
            creatorEvaluationService.insertCreatorInfo(creatorProductPlan, creatorProductPortfolio, creatorImg, creatorInfoDTO, creatorId);
            System.out.println("false");
        }
        return "/main";
    }

    @GetMapping("/creatorBusinessEvaluationRegist")
    public String creatorBusinessEvaluationRegist() {
        return "/contents/evaluation/creatorBusinessEvaluationRegist";
    }

    @PostMapping("/creatorBusinessEvaluationRegist")
    public String creatorBusinessEvaluationRegist(@RequestParam("creatorProductPlan") MultipartFile creatorProductPlan
            , @RequestParam("creatorProductPortfolio") MultipartFile creatorProductPortfolio
            , @RequestParam("creatorImg") MultipartFile creatorImg
            , @RequestParam("businessRegistration") MultipartFile businessRegistration
            , @RequestParam("busiDateStr") String busiDateStr
            , @ModelAttribute CreatorInfoDTO creatorInfoDTO
            , @ModelAttribute CreatorBusinessDTO creatorBusinessDTO
            , Authentication authentication) throws IOException {

        // 현재 로그인 중인 아이디 추출 (심사 등록을 한 크리에이터)
        String creatorId = authentication.getName();

        // 크리에이터 심사 파일과 사업자 정보 삭제
        creatorEvaluationService.deleteCreFileAndBusinessInfo(creatorId);

        // 해당 아이디로 이미 등록된 크리에이터 정보가 있을 시 업데이트, 없으면 인서트
        // 사업자 크리에이터는 개인으로 등록할 때 받는 정보에 추가 정보만 받아서 따로 DB에 저장하기 때문에
        // 개인 등록 절차와 동일하게 진행하고 별도로 사업자 등록 시 추가로 받은 정보만 DB에 저장하는 과정을 추가
        if (creatorEvaluationService.selectCreatorInfo(creatorId)) {
            creatorEvaluationService.updateCreatorInfo(creatorProductPlan, creatorProductPortfolio, creatorImg, creatorInfoDTO, creatorId);
        } else {
            creatorEvaluationService.insertCreatorInfo(creatorProductPlan, creatorProductPortfolio, creatorImg, creatorInfoDTO, creatorId);
        }

        // String으로 넘어온 설립일 값을 Date 타입으로 변환해서 dto에 삽입 후 DB에 사업자 정보 저장
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date busiDate = simpleDateFormat.parse(busiDateStr);
            creatorBusinessDTO.setBusiDate(busiDate);
            System.out.println(creatorBusinessDTO);
            creatorEvaluationService.insertCreatorBusiness(businessRegistration, creatorBusinessDTO, creatorId);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return "/main";
    }

    @GetMapping("/manager/creatorEvaluationList")
    public ModelAndView creatorEvaluationList(ModelAndView modelAndView) {

        modelAndView.addObject("CreatorEvaluationList", creatorEvaluationService.selectCreatorEvaluationList());

        modelAndView.setViewName("/contents/evaluation/manager/creatorEvaluationList");


        return modelAndView;
    }

    @ResponseBody
    @PostMapping("/updateCalAppDate")
    public String updateCalAppDate(@RequestParam("proCode") int proCode) {

        int result = evaluationService.updateCalAppDate(proCode);

        if (result > 0) {
            System.out.println("후원금 최종 정산 승인 완료");
        } else {
            System.out.println("후원금 최종 정산 승인 실패");
        }

        return "/contents/evaluation/evacalculationlist";
    }


    @GetMapping("/manager/creatorEvaluationDetail/{evaNum}")
    public ModelAndView creatorEvaluationDetail(@PathVariable int evaNum, ModelAndView modelAndView) {

        CreatorEvaluationDetailDTO creatorEvaluationDetailDTO = creatorEvaluationService.selectCreatorEvaluationDetail(evaNum);

        String creatorType = creatorEvaluationDetailDTO.getCreatorType();

        modelAndView.addObject("CreatorEvaluationDetailDTO", creatorEvaluationDetailDTO);

        if (creatorType.equals("개인")) {
            modelAndView.setViewName("/contents/evaluation/manager/creatorEvaluationDetail");
        } else {
            modelAndView.setViewName("/contents/evaluation/manager/creatorBusinessEvaluationDetail");
        }

        return modelAndView;
    }

    @GetMapping("/manager/accept")
    public String creatorAccept(@RequestParam("evaNum") int evaNum) {

        // 심사 번호로 멤버 아이디 조회
        String memId = creatorEvaluationService.selectCreatorId(evaNum);

        // 심사가 승인 됐으니 해당 멤버의 권한을 SUPPORTER에서 CREATOR로 업데이트
        String memberRole = "CREATOR";
        creatorEvaluationService.updateCreatorRole(memId, memberRole);

        // 심사 상태 승인으로 변경
        Map<String, Object> map = new HashMap<>();
        map.put("evaNum", evaNum);
        map.put("evaSituation", "승인");
        creatorEvaluationService.updateEvaSituation(map);

        return "/contents/evaluation/manager/creatorEvaluationList";
    }

    @GetMapping("/manager/return")
    public String creatorReturn(@RequestParam("evaNum") int evaNum, @RequestParam("reaRejection") String reaRejection) {

        // 심사 번호로 멤버 아이디 조회
        String memId = creatorEvaluationService.selectCreatorId(evaNum);

        // 해당 심사의 반려 사유와 심사 상태 업데이트
        Map<String, Object> map = new HashMap<>();
        map.put("evaNum", evaNum);
        map.put("reaRejection", reaRejection);
        map.put("evaSituation", "반려");
        creatorEvaluationService.updateReaRejection(map);

//        creatorEvaluationService.deleteCreatorEvaluation(evaNum, reaRejection, memId);

        return  "/contents/evaluation/manager/creatorEvaluationList";
    }

    @GetMapping("/evaluationProcessAfter")
    public String EvaluationProcessAfter() {
        return "/contents/evaluation/manager/creatorEvaluationList";
    }

    @GetMapping("/projectEvaluationList")
    public String projectEvaluationList(Model model) {

        List<ProjectEvaluationDTO> selectAllProjectEvaList = evaluationService.selectAllProjectEva();

        model.addAttribute("selectAllProjectEvaList", selectAllProjectEvaList);

        System.out.println(selectAllProjectEvaList);

        return "/contents/evaluation/projectEvaluationList";
    }

    @GetMapping("/projectEvaluationDetail")
    public String projectEvaluationDetail() {
        return "/contents/evaluation/projectEvaluationDetail";
    }

}

