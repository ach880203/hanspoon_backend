package com.project.hanspoon.recipe.component;

import com.project.hanspoon.recipe.dto.IngredientDto;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RecipeParser {

    public String parse(String content, Map<String, IngredientDto> ingMap, double multiplier) {
        if (content == null) return "";
        String result = content;

        for (String name : ingMap.keySet()) {
            IngredientDto ing = ingMap.get(name);
            String formatted = formatAmount(ing.getBaseAmount() * multiplier);

            result = result.replace("{" + name + "_amount}", name + " " + formatted)
                             .replace("{" + name + "_unit}", ing.getUnit());
        }
        return result;
    }
    private String formatAmount (double amount) {
        return (amount == (long) amount)
                ? String.format("%d", (long) amount)
                : String.format("%.1f", amount);
    }

    public String parseContent(String content, Map<String, IngredientDto> ingredientMap, double i) {
        return null;
    }
}
