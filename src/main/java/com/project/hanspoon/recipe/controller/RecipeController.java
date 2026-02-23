package com.project.hanspoon.recipe.controller;

import com.project.hanspoon.common.security.CustomUserDetails;
import com.project.hanspoon.common.user.repository.UserRepository;
import com.project.hanspoon.recipe.constant.Category;
import com.project.hanspoon.recipe.dto.RecipeDetailDto;
import com.project.hanspoon.recipe.dto.RecipeFormDto;
import com.project.hanspoon.recipe.dto.RecipeListDto;
import com.project.hanspoon.recipe.entity.Recipe;
import com.project.hanspoon.recipe.repository.RecipeRepository;
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
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recipe")
@Log4j2
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    @PostMapping("/new")
    public ResponseEntity<?> createRecipe(@Valid @RequestPart("recipe") RecipeFormDto recipeFormDto,
                             @RequestPart(value = "recipeImage", required = false) MultipartFile recipeImage,
                             BindingResult bindingResult, List<MultipartFile> instructionImages){
        if (recipeImage != null) {
            log.info("====파일 업로드 감지 ======");
            log.info("파일명:" + recipeImage.getOriginalFilename());
            log.info("파일크기" + recipeImage.getSize());
            log.info("콘텐츠 타입" + recipeImage.getContentType());
        }else {
            log.info("=======업로드된 파일이 없습니다=========");
        }
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }
        try {
            // 컨트롤러에서는 파일 저장(uploadFile)을 직접 하지 마세요!
            // 서비스의 saveRecipe가 파일 저장 + DB 저장을 한 번에 처리하도록 맡깁니다.
            recipeService.saveRecipe(recipeFormDto, recipeImage, instructionImages);

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error("레시피 저장 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    
    @GetMapping("/detail/{id}")
    public ResponseEntity<RecipeDetailDto> getRecipeDetail(@PathVariable("id") Long id){

            RecipeDetailDto detail = recipeService.getRecipeDtl(id);

        return ResponseEntity.ok(detail);
    }

    @GetMapping("/list")
    public ResponseEntity<Page<RecipeListDto>> getrecipeList(
            @RequestParam(value = "category", required = false) Category category,
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 8, sort = "id", direction = Sort.Direction.DESC) Pageable pageable){

        Page<Recipe> recipePage = recipeService.getRecipeList(keyword, pageable, category);

        Page<RecipeListDto> recipeList = recipePage
                .map(recipe -> RecipeListDto.builder()
                        .id(recipe.getId())
                        .title(recipe.getTitle())
                        .recipeImg(recipe.getRecipeImg())
                        .category(recipe.getCategory() != null ? recipe.getCategory().name() : "ETC")
                        .reviewCount(recipe.getRecipeRevs().size())
                        .build());

        return ResponseEntity.ok(recipeList);
    }

    @GetMapping("/edit/{id}")
    public ResponseEntity<RecipeDetailDto> getupdateRecipe(@PathVariable Long id) {

        RecipeDetailDto recipeDetailDto = recipeService.getRecipeDtl(id);

        return ResponseEntity.ok(recipeDetailDto);
    }

    @PostMapping("/edit/{id}")
    public ResponseEntity<?> updateRecipe(@PathVariable Long id,
                                          @Valid @RequestPart("recipe") RecipeFormDto recipeFormDto,
                                          @RequestPart(value = "recipeImage", required = false) MultipartFile recipeImage,
                                          List<MultipartFile> instructionImages) {
        recipeFormDto.setId(id);

        Long updateRecipeId = recipeService.updateRecipe(id, recipeFormDto, recipeImage, instructionImages);

        return ResponseEntity.ok(updateRecipeId);
    }


    @PostMapping("/delete/{id}")
    public ResponseEntity<?> deleteRecipe(@PathVariable Long id) {
        try {
            recipeService.deleteRecipe(id);
            return ResponseEntity.ok("삭제 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("삭제실패");
        }
    }

    @GetMapping("/deleted")
    public ResponseEntity<List<RecipeListDto>> getDeletedRecipes(
            @RequestParam(required = false) Category category) {

        List<RecipeListDto> list = recipeService.getDeletedRecipes(category);
        return ResponseEntity.ok(list);
    }


    @PostMapping("/deleteReturn/{id}")
    public ResponseEntity<?> deleteReturn(@PathVariable Long id) {
        try {
            recipeService.deletereturn(id);
            return ResponseEntity.ok("복원 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("복원실패");
        }
    }

    @PostMapping("/createWishes/{id}")
    public ResponseEntity<?> createWishes(@PathVariable Long id,
                                          @AuthenticationPrincipal CustomUserDetails customUserDetails,
                                          Authentication authentication) {
        if (authentication == null) {
            log.info("인증 객체가 null입니다");
        }else{
            log.info("인증된 사용사 이메일:" + authentication.getName());
        }
        try {
            recipeService.createWishes(id,customUserDetails.getEmail());
            return ResponseEntity.ok("관심목록 등록 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("관심목록 등록 실패");
        }
    }

    @GetMapping("/RecipeWishes")
    public ResponseEntity<Page<Recipe>> getMyWishes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Category category,
            @PageableDefault(size = 12, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
            ) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Page<Recipe> wishes = recipeService.getMyWishedRecipes(
                userDetails.getEmail(),
                category != null ? category.name() : null,
                pageable
        );
        return ResponseEntity.ok(wishes);
    }

}
