package com.project.hanspoon.recipe.service;

import com.project.hanspoon.common.user.entity.User;
import com.project.hanspoon.common.user.repository.UserRepository;
import com.project.hanspoon.recipe.component.RecipeParser;
import com.project.hanspoon.recipe.constant.Category;
import com.project.hanspoon.recipe.dto.*;
import com.project.hanspoon.recipe.entity.*;
import com.project.hanspoon.recipe.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
@Transactional
public class RecipeService {

    @Value("${itemImgLocation}")
    private String itemImgLocation;

    private final RecipeRepository recipeRepository; // 레시피 메인 레포스토리
    private final IngredientGroupRepository ingredientGroupRepository; // 재료 그룹 레포스토리
    private final IngredientRepository ingredientRepository; // 개별 재료 레포스토리
    private final InstructionRepository instructionRepository; // 개별 조리 방법 레포스토리
    private final InstructionGroupRepository instructionGroupRepository; // 조리 방법 그룹 레포스토리
    private final RecipeRelationRepository recipeRelationRepository; // 서브 레시피 레포스토리
    private final RecipeParser recipeParser; //
    private final RecipeWishesRepository recipeWishesRepository;
    private final UserRepository userRepository;

    /**
     * 다양한 단위를 g 기준으로 환산한다.
     * - 베이커 퍼센트를 계산할 때 기준 단위를 통일하기 위한 메서드다.
     */
    public double convertToGram(String unit, double amount){
        return switch (unit) {
            case "큰술" -> amount * 15;
            case "작은술" -> amount * 5;
            case "컵" -> amount * 200;
            case "근" -> amount * 600;
            case "g" -> amount ;
            default -> amount; // 정의되지 않은 단위는 일단 그대로 반환
        };
    }

    /**
     * 업로드 파일을 저장하고 저장된 파일명을 반환한다.
     * - 실패 시 빈 문자열("")을 반환한다.
     */
    public String uploadFile(MultipartFile file){
        if (file == null || file.isEmpty()) return "";

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            return "";
        }

        int extensionIndex = originalFileName.lastIndexOf(".");
        String extension = extensionIndex >= 0 ? originalFileName.substring(extensionIndex) : "";
        String savedFileName = UUID.randomUUID() + extension;
        log.info("저장될 파일명: "+ savedFileName);

        try {
            // 1. 디렉토리 생성 확인
            Path uploadPath = Paths.get(itemImgLocation);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 2. ★ transferTo 대신 Files.copy 사용 (이게 훨씬 안전합니다)
            Path filePath = uploadPath.resolve(savedFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("파일 저장 완료: " + savedFileName);
            return savedFileName;

        } catch (IOException e) {
            log.error("파일 저장 실패", e);
            return "";
        }
    }


    /**
     * 레시피 저장(메인 + 재료 + 조리단계 + 서브레시피 관계).
     */
    public void saveRecipe(RecipeFormDto recipeFormDto,
                           MultipartFile recipeImage,
                           List<MultipartFile> instructionImages){

        // 1) 메인 레시피 엔티티 생성
        Recipe mainRecipe = Recipe.createRecipe(recipeFormDto);

        // 2) 대표 이미지 저장
        if (recipeImage != null && !recipeImage.isEmpty()) {
            String savedMainImgName = uploadFile(recipeImage);
            mainRecipe.updateRecipeImg(savedMainImgName);
        }

        // 3) 메인 레시피 저장
        mainRecipe = recipeRepository.save(mainRecipe);

        // 4) 하위 데이터 저장
        saveIngredientsAndInstructions(mainRecipe, recipeFormDto, instructionImages);
        saveRecipeRelations(mainRecipe, recipeFormDto.getSubrecipe());

    }

    /**
     * 메인 레시피와 서브 레시피 연결 정보를 저장한다.
     */
    private void saveRecipeRelations(Recipe mainRecipe, List<Long> subRecipeIds) {
        if (subRecipeIds != null) {
            for (Long subId : subRecipeIds) {
                Recipe subRecipe = recipeRepository.findById(subId)
                        .orElseThrow(() -> new EntityNotFoundException("서브 레시피를 찾을 수 없습니다. ID: " + subId));

                RecipeRelation relation = RecipeRelation.builder()
                        .mainRecipe(mainRecipe)
                        .subRecipe(subRecipe)
                        .build();
                recipeRelationRepository.save(relation);
            }
        }
    }
    @Transactional(readOnly = true)
    public RecipeDetailDto getRecipeDtl(Long id, String userEmail){

        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(()-> new EntityNotFoundException("레시피를 찾을 수 없습니다"));

        // 비로그인 요청은 찜 여부를 false로 처리해 상세 조회가 항상 동작하도록 한다.
        boolean liked = false;
        if (userEmail != null && !userEmail.isBlank()) {
            liked = recipeWishesRepository.existsByUserEmailAndRecipeId(userEmail, id);
        }

        RecipeDetailDto dto = RecipeDetailDto.fromEntity(recipe, liked);

        dto.getInstructionGroup().forEach(group -> {
            group.getInstructions().forEach(inst -> {
                // 조리문 원본 템플릿(@재료명)은 프론트에서 인분 변경 시 동적 치환하므로 원본을 유지한다.
                // String parsed = recipeParser.parse(inst.getContent(), dto.getIngredientMap(),1.0);
            });
        });

        return dto;
  }

    // 기존 컨트롤러 호출(파라미터 1개)과 호환되도록 오버로드를 제공한다.
    @Transactional(readOnly = true)
    public RecipeDetailDto getRecipeDtl(Long id) {
        return getRecipeDtl(id, null);
    }

    /**
     * 레시피 목록 조회.
     * - keyword가 null이면 빈 문자열로 정규화해 repository 쿼리 오류를 방지한다.
     */

    @Transactional(readOnly = true)
    public Page<RecipeListDto> getRecipeListDto(String keyword, Pageable pageable, Category category) {
        String normalizedkeyword = (keyword == null || keyword.trim().isEmpty()) ? "" : keyword.trim();
        log.info("DEBUG: 카테고리=" + category + ", 키워드=[ " + normalizedkeyword + "]");

        Page<Recipe> recipePage;

        if(category == null) {
            if (normalizedkeyword.isEmpty()) {
                recipePage = recipeRepository.findByDeletedFalse(pageable);
            } else{
                recipePage = recipeRepository.findByTitleContainingAndDeletedFalse
                        (normalizedkeyword, pageable);
            }
        } else {
            recipePage = recipeRepository.findByCategoryAndTitleContainingAndDeletedFalse
                    (category, normalizedkeyword, pageable);
        }

        log.info("DEBUG: DB 조회 결과 건수=" + recipePage.getContent().size());

        // 2. 위에서 가져온 '필터링된 결과물'을 DTO로 변환만 하는 거예요.
        return recipePage.map(recipe -> RecipeListDto.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .recipeImg(recipe.getRecipeImg())
                .category(recipe.getCategory() != null ? recipe.getCategory().name() : "ETC")
                .reviewCount(recipe.getRecipeRevs().size()) // 이제 에러 안 남!
                .build());
    }

    /**
     * 목록 조회용 DTO를 트랜잭션 내부에서 생성한다.
     * 컨트롤러 레벨에서 지연 로딩 컬렉션을 직접 접근하면 LazyInitializationException이 날 수 있어
     * 변환 책임을 서비스로 이동했다.
     */
    @Transactional(readOnly = true)
    public Page<RecipeListDto> getRecipeListForView(String keyword, Pageable pageable, Category category) {
        return getRecipeListDto(keyword, pageable, category);
    }

    @Transactional
    public void deleteRecipe(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("레시피를 찾을 수 없습니다"));

        recipe.delete();
    }

    @Transactional
    public Long updateRecipe(
            Long id, RecipeFormDto recipeFormDto, MultipartFile recipeImage,
            List<MultipartFile> instructionImages) {

        log.info("===== 업데이트 시작=========");

        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(EntityNotFoundException::new);

        recipe.setTitle(recipeFormDto.getTitle());
        recipe.setCategory(recipeFormDto.getCategory());
        recipe.setBaseServings(recipeFormDto.getBaseServings());

        if(recipeImage !=null && !recipeImage.isEmpty()) {
            String savedMainImgName = uploadFile(recipeImage);
            recipe.updateRecipeImg(savedMainImgName);
        }

        recipe.getRecipeIngredientGroup().clear();
        recipe.getRecipeInstructionGroup().clear();
        recipe.getSubRecipeRelations().clear();

        log.info("====새로 저장=====");
        if (instructionImages != null) {
            log.info("조리 과정 이미지 개수: {}", instructionImages.size());
            instructionImages.forEach(file ->
                    log.info("전달된 파일명: {}, 크기: {}", file.getOriginalFilename(), file.getSize())
            );
        } else {
            log.info("조리 과정 이미지 리스트(instructionImages)가 null입니다.");
        }

        log.info("saveIngredientsAndInstructions 메서드 진입 직전");

        saveIngredientsAndInstructions(recipe, recipeFormDto, instructionImages);
        saveRecipeRelations(recipe, recipeFormDto.getSubrecipe());


        return recipe.getId();
    }

    public Recipe findById(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("해당 레시피가 존재하지 않습니다 id=" + id));
    }

    public List<Recipe> findAll() {
        return recipeRepository.findAll();
    }

    /**
     * 재료 그룹/재료와 조리 그룹/조리단계를 저장한다.
     * - 먼저 기존 하위 데이터를 지운 뒤 재생성한다.
     * - 베이커 퍼센트는 main 재료 우선 기준으로 계산한다.
     */
    public void saveIngredientsAndInstructions(
            Recipe recipe, RecipeFormDto recipeFormDto,
            List<MultipartFile> instructionImages) {

        ingredientGroupRepository.deleteByRecipe(recipe);
        instructionGroupRepository.deleteByRecipe(recipe);

        double mainTotalAmount = recipeFormDto.getIngredientGroup().stream()
                .flatMap(group -> group.getIngredients().stream())
                .filter(IngredientDto::isMain)
                .mapToDouble(dto -> convertToGram(dto.getUnit(), dto.getBaseAmount()))
                .sum();

        if (mainTotalAmount <= 0) {
            mainTotalAmount = recipeFormDto.getIngredientGroup().stream()
                    .flatMap(group -> group.getIngredients().stream())
                    .mapToDouble(dto -> convertToGram(dto.getUnit(), dto.getBaseAmount()))
                    .sum();
        }

        final double finalBasis = mainTotalAmount;

        recipeFormDto.getIngredientGroup().forEach(groupDto -> {
            RecipeIngredientGroup group = RecipeIngredientGroup.builder()
                    .name(groupDto.getName())
                    .recipe(recipe)
                    .sortOrder(groupDto.getSortOrder())
                    .build();
            ingredientGroupRepository.save(group);

            groupDto.getIngredients().forEach(ingreDto -> {
                double currentGram = convertToGram(ingreDto.getUnit(), ingreDto.getBaseAmount());
                double calculatedRatio = (finalBasis > 0) ? (currentGram / finalBasis) * 100 : 0;

                RecipeIngredient ingredient = RecipeIngredient.builder()
                        .recipeIngredientGroup(group)
                        .name(ingreDto.getName())
                        .baseAmount(ingreDto.getBaseAmount())
                        .ratio(calculatedRatio)
                        .unit(ingreDto.getUnit())
                        .tasteType(ingreDto.getTasteType())
                        .main(ingreDto.isMain())
                        .build();
                ingredientRepository.save(ingredient);
            });
        });

        int fileIdx = 0;

        for (InstructionGroupDto instGroupDto :
                recipeFormDto.getInstructionGroup()) {
            RecipeInstructionGroup instGroup = RecipeInstructionGroup.builder()
                    .title(instGroupDto.getTitle())
                    .recipe(recipe)
                    .sortOrder(instGroupDto.getSortOrder())
                    .build();
            instructionGroupRepository.save(instGroup);

            for (InstructionDto instDto : instGroupDto.getInstructions()) {
                String savedFileName = instDto.getInstImg();

                if (instructionImages != null && fileIdx < instructionImages.size()) {
                    MultipartFile file = instructionImages.get(fileIdx);

                    if (file != null && !file.isEmpty()) {
                        try {
                            savedFileName = uploadFile(file);
                            log.info("조리 사진 저장 완료: " + savedFileName);
                        } catch (Exception e) {
                            log.info("파일 저장 중 에러 발생", e);
                        }
                    }
                    fileIdx++;
                }

                RecipeInstruction instruction = RecipeInstruction.builder()
                        .recipeInstructionGroup(instGroup)
                        .stepOrder(instDto.getStepOrder())
                        .content(instDto.getContent())
                        .instImg(savedFileName)
                        .build();
                instructionRepository.save(instruction);
                }
            }
        }


    public void deletereturn(Long id) {
            Recipe recipe = recipeRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("레시피를 찾을 수 없습니다"));

            recipe.deleteReturn();

    }

    public List<RecipeListDto> getDeletedRecipes(Category category) {
        List<Recipe> recipes = (category == null)
            ? recipeRepository.findByDeletedTrue()
            : recipeRepository.findByDeletedTrueAndCategory(category);

        return recipes.stream().map(recipe -> RecipeListDto.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .recipeImg(recipe.getRecipeImg())
                .category(String.valueOf(recipe.getCategory()))
                .build()).toList();
    }

    /**
     * 레시피 찜 등록.
     * - 같은 사용자가 같은 레시피를 중복 찜하는 요청은 무시한다.
     */
    public void createWishes(Long id, String email) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("레시피를 찾을 수 없습니다"));

        User user = userRepository.findByEmail(email)
                        .orElseThrow(()-> new EntityNotFoundException("사용자를 찾을 수 없습니다"));

        if (recipeWishesRepository.existsByUserEmailAndRecipeId(email, id)) {
            return;
        }

        recipeWishesRepository.save(new RecipeWish(recipe, user));
    }

    public  Page<WishDto> getMyWishedRecipes(String email, String category, Pageable pageable) {
        Page<RecipeWish> wishPage;

        if (category == null || category.isEmpty()) {
            wishPage = recipeWishesRepository.findByUserEmail(email, pageable);
        } else {
            wishPage = recipeWishesRepository.findByUserEmailAndCategory(email, category, pageable);
        }
        return wishPage.map(recipeWish -> new WishDto(recipeWish, recipeWish.getRecipe()));
    }
}
