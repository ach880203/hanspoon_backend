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

    @Value("c:/hanspoon/img")
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

    public String uploadFile(MultipartFile file){
        if (file == null || file.isEmpty()) return "";


        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String savedFileName = UUID.randomUUID().toString() + extension;
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


    // 1. 계산 로직 : 모든 그룹의 재료를 합쳐서 기준값을 구해야한다
    public Long saveRecipe(RecipeFormDto recipeFormDto,
                           MultipartFile recipeImage,
                           List<MultipartFile> instructionImages){

        //1. 레시피 메인 저장
        Recipe mainrecipe = Recipe.createRecipe(recipeFormDto);

        if (recipeImage != null && !recipeImage.isEmpty()) {
            String savedMainImgName = uploadFile(recipeImage);
            mainrecipe.updateRecipeImg(savedMainImgName);

        }
        recipeRepository.save(mainrecipe);

        saveIngredientsAndInstructions(mainrecipe, recipeFormDto, instructionImages);

        saveRecipeRelations(mainrecipe, recipeFormDto.getSubrecipe());

        return mainrecipe.getId();
    }

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
    public RecipeDetailDto getRecipeDtl(Long id){

        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(()-> new EntityNotFoundException("레시피를 찾을 수 없습니다"));

        RecipeDetailDto dto = RecipeDetailDto.fromEntity(recipe);

        dto.getInstructionGroup().forEach(group->{
            group.getInstructions().forEach(inst->{
                String parsed = recipeParser.parse(inst.getContent(), dto.getIngredientMap(),1.0);
                //inst.setContent(parsed);
            });
        });

        return dto;
  }



  public Page<Recipe> getRecipeList
          (String keyword, Pageable pageable, Category category) {
            if (category == null) {
                if (keyword == null || keyword.isEmpty()) {
                    return recipeRepository.findByDeletedFalse(pageable);
                }
                return recipeRepository.findByTitleContainingAndDeletedFalse(keyword, pageable);
            }
        return recipeRepository.findByCategoryAndTitleContainingAndDeletedFalse
                (category, keyword, pageable);
    }

    @Transactional
    public Long deleteRecipe(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("레시피를 찾을 수 없습니다"));

        recipe.delete();

        return recipe.getId();
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

    public void saveIngredientsAndInstructions(
            Recipe recipe, RecipeFormDto recipeFormDto,
            List<MultipartFile> instructionImages) {

        ingredientGroupRepository.deleteByRecipe(recipe);
        instructionGroupRepository.deleteByRecipe(recipe);

        double mainTotalAmount = recipeFormDto.getIngredientGroup().stream()
                .flatMap(group -> group.getIngredients().stream())
                .filter(dto -> dto.isMain())
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

    public void createWishes(Long id, String email) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("레시피를 찾을 수 없습니다"));

        User user = userRepository.findByEmail(email)
                        .orElseThrow(()-> new EntityNotFoundException("사용자를 찾을 수 없습니다"));

        recipeWishesRepository.save(new RecipeWish(recipe, user));
    }

    public  Page<Recipe> getMyWishedRecipes(String email, String category, Pageable pageable) {
        if (category == null || category.isEmpty()) {
            return recipeWishesRepository.findRecipeByUserEmail(email, pageable);
        } else {
            return recipeWishesRepository.findRecipeByUserEmailAndCategory(email, category, pageable);
        }
    }
}
