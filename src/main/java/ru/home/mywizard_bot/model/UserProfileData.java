package ru.home.mywizard_bot.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

/**
 * Данные анкеты пользователя
 */

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileData implements Serializable {
    String name;
    String position;
    String town;

    @Override
    public String toString() {
        return "Имя = " + name +
                ", \nДолжность = " + position +
                ", \nГород работы = " + town;
    }
}
