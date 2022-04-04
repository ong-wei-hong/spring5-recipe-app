package guru.springframework.spring5recipeapp.services;

import guru.springframework.spring5recipeapp.commands.IngredientCommand;
import guru.springframework.spring5recipeapp.converters.IngredientCommandToIngredient;
import guru.springframework.spring5recipeapp.converters.IngredientToIngredientCommand;
import guru.springframework.spring5recipeapp.domain.Ingredient;
import guru.springframework.spring5recipeapp.domain.Recipe;
import guru.springframework.spring5recipeapp.repositories.RecipeRepository;
import guru.springframework.spring5recipeapp.repositories.UnitOfMeasureRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IngredientServiceImpl implements IngredientService {

    private final RecipeRepository recipeRepository;
    private final IngredientToIngredientCommand ingredientToIngredientCommand;
    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final IngredientCommandToIngredient ingredientCommandToIngredient;

    public IngredientServiceImpl(RecipeRepository recipeRepository, IngredientToIngredientCommand ingredientToIngredientCommand, UnitOfMeasureRepository unitOfMeasureRepository, IngredientCommandToIngredient ingredientCommandToIngredient) {
        this.recipeRepository = recipeRepository;
        this.ingredientToIngredientCommand = ingredientToIngredientCommand;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
        this.ingredientCommandToIngredient = ingredientCommandToIngredient;
    }

    @Override
    public IngredientCommand findCommandByRecipeIdAndIngredientId(Long recipeId, Long ingredientId) {
        return ingredientToIngredientCommand.convert(recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("recipe not found"))
                .getIngredients() // get list of ingredients
                .stream()
                .filter(ingredient -> ingredient.getId() == ingredientId)
                .findAny()
                .orElseThrow(() -> new RuntimeException("ingredient not found")));
    }

    @Override
    public IngredientCommand saveIngredientCommand(IngredientCommand command) {
        Recipe recipe = recipeRepository.findById(command.getRecipeId()).orElseThrow();
        Long commandId = command.getId();

        Optional<Ingredient> optionalIngredient = recipe.getIngredients()
                .stream()
                .filter(ingredient -> ingredient.getId().equals(commandId))
                .findAny();
        if (optionalIngredient.isPresent()) { // update
            Ingredient ingredient = optionalIngredient.get();
            ingredient.setDescription(command.getDescription());
            ingredient.setAmount(command.getAmount());
            ingredient.setUnit(
                    unitOfMeasureRepository.findById(command.getUom().getId())
                            .orElseThrow(() -> new RuntimeException("Uom not found"))
            );
        } else { // create
            Ingredient ingredient = ingredientCommandToIngredient.convert(command);
            ingredient.setRecipe(recipe);
            recipe.addIngredient(ingredient);
        }

        Recipe savedRecipe = recipeRepository.save(recipe);
        Ingredient savedIngredient = savedRecipe.getIngredients()
                .stream()
                .filter(ingredient -> ingredient.getId().equals(commandId))
                .findAny()
                .orElseGet( // if create this action will be executed instead
                        () -> savedRecipe.getIngredients()
                                .stream()
                                .filter(ingredient -> ingredient.getDescription().equals(command.getDescription()))
                                .filter(ingredient -> ingredient.getAmount().equals(command.getAmount()))
                                .filter(ingredient -> ingredient.getUnit().getId().equals(command.getUom().getId()))
                                .findAny()
                                .orElseThrow(() -> new RuntimeException("ingredient not found"))
                );

        return ingredientToIngredientCommand.convert(savedIngredient);
    }

    @Override
    public void deleteIngredientById(Long recipeId, Long ingredientId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found")); // get recipe

        Ingredient ingredient = recipe.getIngredients()
                .stream()
                .filter(i -> i.getId().equals(ingredientId))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Ingredient not found"));

        ingredient.setRecipe(null);
        recipe.getIngredients().remove(ingredient);

        recipeRepository.save(recipe);
    }
}
