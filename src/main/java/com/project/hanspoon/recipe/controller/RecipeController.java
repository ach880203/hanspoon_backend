package com.project.hanspoon.recipe.controller;

import com.project.hanspoon.common.response.ApiResponse;
import com.project.hanspoon.common.security.CustomUserDetails;
import com.project.hanspoon.recipe.constant.Category;
import com.project.hanspoon.recipe.dto.MyRecipeReviewDto;
import com.project.hanspoon.recipe.dto.RecipeDetailDto;
import com.project.hanspoon.recipe.dto.RecipeFormDto;
import com.project.hanspoon.recipe.dto.RecipeListDto;
import com.project.hanspoon.recipe.dto.WishDto;
import com.project.hanspoon.recipe.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recipe")
@Log4j2
public class RecipeController {

    private final RecipeService recipeService;

    /**
     * 레시피 생성 API.
     * - multipart/form-data의 recipe(JSON) + recipeImage(대표 이미지) + instructionImages(단계 이미지)를 받는다.
     * - 파일 저장/DB 저장은 모두 서비스 계층에서 처리한다.
     */
    @PostMapping("/new")
    public ResponseEntity<ApiResponse<Void>> createRecipe(
            @Valid @RequestPart("recipe") RecipeFormDto recipeFormDto,
            @RequestPart(value = "recipeImage", required = false) MultipartFile recipeImage,
            @RequestPart(value = "instructionImages", required = false) List<MultipartFile> instructionImages,
            @AuthenticationPrincipal CustomUserDetails UserDetails,
            BindingResult bindingResult) {

        if (recipeImage != null) {
            log.info("대표 이미지 업로드 감지: name={}, size={}", recipeImage.getOriginalFilename(), recipeImage.getSize());
        } else {
            log.info("대표 이미지 없이 레시피 생성 요청");
        }

        // @Valid 검증 실패 시 즉시 400 반환
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("입력값 검증에 실패했습니다."));
        }

        try {
            recipeService.saveRecipe(recipeFormDto, recipeImage, instructionImages, UserDetails);
            return ResponseEntity.ok(ApiResponse.ok("레시피가 등록되었습니다."));
        } catch (Exception e) {
            log.error("레시피 저장 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("레시피 저장 중 오류가 발생했습니다."));
        }
    }

    /**
     * 레시피 상세 조회 API.
     * - 비로그인 사용자도 조회 가능(찜 여부는 false로 내려감).
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<ApiResponse<RecipeDetailDto>> getRecipeDetail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        String email = (customUserDetails != null) ? customUserDetails.getEmail() : null;

        RecipeDetailDto detail = recipeService.getRecipeDtl(id, email);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    /**
     * 레시피 목록 조회 API.
     * - category/keyword/페이지 조건을 받아 목록을 반환한다.
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Page<RecipeListDto>>> getRecipeList(
            @RequestParam(value = "category", required = false) Category category,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "userId", required = false) Long userId,
            @PageableDefault(size = 8, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<RecipeListDto> recipeList = recipeService.getRecipeListDto(keyword, pageable, category, userId);

        return ResponseEntity.ok(ApiResponse.ok(recipeList));
    }

    /**
     * 레시피 수정 화면에 필요한 상세 데이터 조회 API.
     */
    @GetMapping("/edit/{id}")
    public ResponseEntity<ApiResponse<RecipeDetailDto>> getUpdateRecipe(@PathVariable Long id) {
        RecipeDetailDto recipeDetailDto = recipeService.getRecipeDtl(id);
        return ResponseEntity.ok(ApiResponse.ok(recipeDetailDto));
    }

    /**
     * 레시피 수정 API.
     */
    @PostMapping("/edit/{id}")
    public ResponseEntity<ApiResponse<Long>> updateRecipe(@PathVariable Long id,
            @Valid @RequestPart("recipe") RecipeFormDto recipeFormDto,
            @RequestPart(value = "recipeImage", required = false) MultipartFile recipeImage,
            @RequestPart(value = "instructionImages", required = false) List<MultipartFile> instructionImages) {
        recipeFormDto.setId(id);

        Long updateRecipeId = recipeService.updateRecipe(id, recipeFormDto, recipeImage, instructionImages);
        return ResponseEntity.ok(ApiResponse.ok("레시피가 수정되었습니다.", updateRecipeId));
    }

    /**
     * 레시피 소프트 삭제 API.
     */
    @PostMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRecipe(@PathVariable Long id) {
        try {
            recipeService.deleteRecipe(id);
            return ResponseEntity.ok(ApiResponse.ok("레시피가 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("레시피 삭제에 실패했습니다."));
        }
    }

    /**
     * 삭제된 레시피 목록 조회 API.
     */
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<List<RecipeListDto>>> getDeletedRecipes(
            @RequestParam(required = false) Category category) {

        List<RecipeListDto> list = recipeService.getDeletedRecipes(category);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /**
     * 소프트 삭제된 레시피 복원 API.
     */
    @PostMapping("/deleteReturn/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReturn(@PathVariable Long id) {
        try {
            recipeService.deletereturn(id);
            return ResponseEntity.ok(ApiResponse.ok("레시피가 복원되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("레시피 복원에 실패했습니다."));
        }
    }

    @PostMapping("/hard_delete/{id}")
    public ResponseEntity<?> hardDeleteRecipe(@PathVariable Long id) {
        try {
            recipeService.permanentDelete(id);
            return ResponseEntity.ok().body("영구 삭제가 완료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("삭제 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 레시피 찜 등록 API.
     */
    @PostMapping("/toggleWish/{id}")
    public ResponseEntity<ApiResponse<Void>> toggleWish(@PathVariable Long id,
                                          @AuthenticationPrincipal CustomUserDetails customUserDetails,
                                          Authentication authentication) {
        // 인증 정보가 없는 경우(비로그인)는 401로 명확히 응답한다.
        if (customUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("로그인이 필요합니다."));
        }

        if (authentication == null) {
            log.info("인증 객체가 null입니다");
        } else {
            log.info("인증된 사용자 이메일: {}", authentication.getName());
        }
        try {
            recipeService.createWishes(id, customUserDetails.getEmail());
            return ResponseEntity.ok(ApiResponse.ok("관심목록에 등록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("관심목록 등록에 실패했습니다."));
        }
    }

    /**
     * 로그인 사용자의 찜 레시피 목록 조회 API.
     * - 기존 경로 호환을 위해 URI 오타(ResipeWishes)를 유지한다.
     */
    @GetMapping("/RecipeWishes")
    public ResponseEntity<ApiResponse<Page<WishDto>>> getMyWishes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Category category,
            @PageableDefault(size = 12, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("로그인이 필요합니다."));
        }

        Page<WishDto> wishes = recipeService.getMyWishedRecipes(
                userDetails.getEmail(),
                category != null ? category.name() : null,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(wishes));
    }

    @DeleteMapping("/deletewihses/{id}")
    public ResponseEntity<ApiResponse<String>> deletewihses(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        recipeService.removeWish(customUserDetails.getEmail(), id);

        return ResponseEntity.ok(ApiResponse.ok(null,"찜 목록에서 삭제되었습니다"));
    };


    /**
     * 로그인 사용자가 작성한 레시피 리뷰 목록 조회 API.
     * 마이페이지 통합 "내 리뷰" 화면에서 레시피/원데이/마켓 데이터를 함께 보여줄 때 사용합니다.
     */
    @GetMapping("/reviews/me")
    public ResponseEntity<ApiResponse<List<MyRecipeReviewDto>>> getMyRecipeReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("로그인이 필요합니다."));
        }

        List<MyRecipeReviewDto> reviews = recipeService.getMyRecipeReviews(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(reviews));
    }
    @PostMapping("/{id}/recommend")
    public ResponseEntity<?> toggleRecommend(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails // 🚩 로그인한 유저 정보
    ) {
        // 1. 로그인 체크
        if (userDetails == null) {
            return ResponseEntity.status(401).body("로그인이 필요한 기능입니다.");
        }

        try {
            // 2. 서비스 호출 (레시피 ID와 유저 ID 전달)
            // userDetails.getUser().getId() 부분은 한나님의 UserDetails 구조에 맞게 수정하세요.
            Long loginUserId = userDetails.getUser().getUserId();

            recipeService.toggleRecommendation(id, loginUserId);

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "추천 상태가 변경되었습니다."
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("추천 처리 중 오류가 발생했습니다.");
        }
    }
}
