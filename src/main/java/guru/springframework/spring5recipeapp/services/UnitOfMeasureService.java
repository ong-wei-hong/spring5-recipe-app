package guru.springframework.spring5recipeapp.services;

import guru.springframework.spring5recipeapp.commands.UnitOfMeasureCommand;

import java.util.List;

public interface UnitOfMeasureService {
    List<UnitOfMeasureCommand> findAllCommands();
}
