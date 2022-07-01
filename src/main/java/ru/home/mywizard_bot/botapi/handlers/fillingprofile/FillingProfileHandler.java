package ru.home.mywizard_bot.botapi.handlers.fillingprofile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.home.mywizard_bot.botapi.BotState;
import ru.home.mywizard_bot.botapi.InputMessageHandler;
import ru.home.mywizard_bot.cache.UserDataCache;
import ru.home.mywizard_bot.model.UserProfileData;
import ru.home.mywizard_bot.service.PredictionService;
import ru.home.mywizard_bot.service.ReplyMessagesService;
import ru.home.mywizard_bot.utils.Emojis;
import java.util.ArrayList;
import java.util.List;


/**
 * Формирует анкету пользователя.
 */

@Slf4j
@Component
public class FillingProfileHandler implements InputMessageHandler {
    private UserDataCache userDataCache;
    private ReplyMessagesService messagesService;
    private PredictionService predictionService;

    public FillingProfileHandler(UserDataCache userDataCache, ReplyMessagesService messagesService,
                                 PredictionService predictionService) {
        this.userDataCache = userDataCache;
        this.messagesService = messagesService;
        this.predictionService = predictionService;
    }

    @Override
    public SendMessage handle(Message message) {
        if (userDataCache.getUsersCurrentBotState(Math.toIntExact(message.getFrom().getId())).equals(BotState.FILLING_PROFILE)) {
            userDataCache.setUsersCurrentBotState(Math.toIntExact(message.getFrom().getId()), BotState.ASK_NAME);
        }
        return processUsersInput(message);
    }

    @Override
    public BotState getHandlerName() {
        return BotState.FILLING_PROFILE;
    }

    private SendMessage processUsersInput(Message inputMsg) {
        String usersAnswer = inputMsg.getText();
        int userId = Math.toIntExact(inputMsg.getFrom().getId());
        long chatId = inputMsg.getChatId();
        UserProfileData profileData = userDataCache.getUserProfileData(userId);
        BotState botState = userDataCache.getUsersCurrentBotState(userId);

        SendMessage replyToUser = null;

        if(botState.equals(BotState.ASK_NAME))

            {
                replyToUser = messagesService.getReplyMessage(chatId, "reply.askName");
                userDataCache.setUsersCurrentBotState(userId, BotState.ASK_POSITION);
            }

        if(botState.equals(BotState.ASK_POSITION))

            {
                profileData.setName(usersAnswer);
                replyToUser = messagesService.getReplyMessage(chatId, "reply.position");
                replyToUser.setReplyMarkup(getPositionButtonsMarkup());
            }

        if(botState.equals(BotState.ASK_TOWN))

            {
                profileData.setPosition(usersAnswer);
                replyToUser = messagesService.getReplyMessage(chatId, "reply.askTown");
                userDataCache.setUsersCurrentBotState(userId, BotState.PROFILE_FILLED);
            }

        if(botState.equals(BotState.PROFILE_FILLED))

            {
                profileData.setTown(usersAnswer);
                userDataCache.setUsersCurrentBotState(userId, BotState.SHOW_MAIN_MENU);

                String profileFilledMessage = messagesService.getReplyText("reply.profileFilled",
                        profileData.getName(), profileData.getTown(), Emojis.SPARKLES);


                String predictionMessage = predictionService.getPredictionCS();
                String end = "\n..............................................................\n\nТы можешь попросить ещё сколько угодно предсказаний, но актуальным на сегодня может быть только одно!";

                switch (profileData.getPosition()) {
                    case "КС":
                        replyToUser = new SendMessage(String.valueOf(chatId), String.format("%s%n%n%s %s", profileFilledMessage, Emojis.SCROLL, predictionMessage + end));
                        break;
                    case "РГКП / УРП":
                        predictionMessage = predictionService.getPredictionRSM();
                        replyToUser = new SendMessage(String.valueOf(chatId), String.format("%s%n%n%s %s", profileFilledMessage, Emojis.SCROLL, predictionMessage + end));
                        break;
                    case "Сотрудник ЦO":
                        predictionMessage = predictionService.getPredictionCO();
                        replyToUser = new SendMessage(String.valueOf(chatId), String.format("%s%n%n%s %s", profileFilledMessage, Emojis.SCROLL, predictionMessage + end));
                        break;
                    default:
                        replyToUser = new SendMessage(String.valueOf(chatId), String.format("%s%n%n%s %s", profileFilledMessage, Emojis.SCROLL,
                                "Тебе суждено вернуться в Директ Кредит и работать там до скончания своих веков!!!   ХА-ХА-ХА-ХА!!!" + end));
                        break;
                }
                replyToUser.setParseMode("HTML");
            }
        userDataCache.saveUserProfileData(userId,profileData);

        return replyToUser;
    }

    private InlineKeyboardMarkup getPositionButtonsMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton buttonPositionCS = new InlineKeyboardButton();
        buttonPositionCS.setText("КС");
        InlineKeyboardButton buttonPositionRSM = new InlineKeyboardButton();
        buttonPositionRSM.setText("РГКП / УРП");
        InlineKeyboardButton buttonPositionCO = new InlineKeyboardButton();
        buttonPositionCO.setText("Сотрудник офиса");
        InlineKeyboardButton buttonPositionFO = new InlineKeyboardButton();
        buttonPositionFO.setText("Бывший сотрудник");

        buttonPositionCS.setCallbackData("buttonCS");
        buttonPositionRSM.setCallbackData("buttonRSM");
        buttonPositionCO.setCallbackData("buttonCO");
        buttonPositionFO.setCallbackData("buttonFO");

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(buttonPositionCS);
        keyboardButtonsRow1.add(buttonPositionRSM);

        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        keyboardButtonsRow2.add(buttonPositionCO);
        keyboardButtonsRow2.add(buttonPositionFO);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow1);
        rowList.add(keyboardButtonsRow2);

        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }
}



