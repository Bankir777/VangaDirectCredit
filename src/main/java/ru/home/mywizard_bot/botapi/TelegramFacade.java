package ru.home.mywizard_bot.botapi;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.home.mywizard_bot.MyWizardTelegramBot;
import ru.home.mywizard_bot.cache.UserDataCache;
import ru.home.mywizard_bot.model.UserProfileData;
import ru.home.mywizard_bot.service.MainMenuService;
import ru.home.mywizard_bot.service.ReplyMessagesService;
import ru.home.mywizard_bot.utils.Emojis;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * @author Bankir
 */

@Component
@Slf4j
public class TelegramFacade {
    private final BotStateContext botStateContext;
    private final UserDataCache userDataCache;
    private final MainMenuService mainMenuService;
    private final MyWizardTelegramBot myWizardBot;
    private final ReplyMessagesService messagesService;

    public TelegramFacade(BotStateContext botStateContext, UserDataCache userDataCache, MainMenuService mainMenuService,
                          @Lazy MyWizardTelegramBot myWizardBot, ReplyMessagesService messagesService) {
        this.botStateContext = botStateContext;
        this.userDataCache = userDataCache;
        this.mainMenuService = mainMenuService;
        this.myWizardBot = myWizardBot;
        this.messagesService = messagesService;
    }

    public BotApiMethod<?> handleUpdate(Update update) {
        SendMessage replyMessage = null;

        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            log.info("New callbackQuery from User: {}, userId: {}, with data: {}", update.getCallbackQuery().getFrom().getUserName(),
                    callbackQuery.getFrom().getId(), update.getCallbackQuery().getData());
            return processCallbackQuery(callbackQuery);
        }


        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            log.info("New message from User:{}, userId: {}, chatId: {},  with text: {}",
                    message.getFrom().getUserName(), message.getFrom().getId(), message.getChatId(), message.getText());
            replyMessage = handleInputMessage(message);
        }

        return replyMessage;
    }


    private SendMessage handleInputMessage(Message message) {
        String inputMsg = message.getText();
        int userId = Math.toIntExact(message.getFrom().getId());
        long chatId = message.getChatId();
        BotState botState;
        SendMessage replyMessage;

        switch (inputMsg) {
            case "/start":
                botState = BotState.ASK_DESTINY;
                myWizardBot.sendPhoto(chatId, messagesService.getReplyText("reply.hello", Emojis.MAGE), "static/images/wizard_logo.jpg");
                break;
            case "Получить предсказание":
                botState = BotState.FILLING_PROFILE;
                break;
            case "Моя анкета":
                botState = BotState.SHOW_USER_PROFILE;
                break;
            case "Скачать анкету":
                myWizardBot.sendDocument(chatId, "Ваша анкета", getUsersProfile(userId));
                botState = BotState.SHOW_USER_PROFILE;
                break;
            case "Помощь":
                botState = BotState.SHOW_СONTACTS;
                break;
            default:
                botState = userDataCache.getUsersCurrentBotState(userId);
                break;
        }

        userDataCache.setUsersCurrentBotState(userId, botState);

        replyMessage = botStateContext.processInputMessage(botState, message);

        return replyMessage;
    }


    private BotApiMethod<?> processCallbackQuery(CallbackQuery buttonQuery) {
        final long chatId = buttonQuery.getMessage().getChatId();
        final int userId = Math.toIntExact(buttonQuery.getFrom().getId());
        BotApiMethod<?> callBackAnswer = mainMenuService.getMainMenuMessage(chatId, "Воспользуйтесь главным меню");

        //From Destiny choose buttons
        if (buttonQuery.getData().equals("buttonYes")) {
            callBackAnswer = new SendMessage(String.valueOf(chatId), "Как тебя зовут ?");
            userDataCache.setUsersCurrentBotState(userId, BotState.ASK_POSITION);
        } else if (buttonQuery.getData().equals("buttonNo")) {
            callBackAnswer = sendAnswerCallbackQuery("Ну и зачем ты тогда пришёл?", true, buttonQuery);
        } else if (buttonQuery.getData().equals("buttonIwillThink")) {
            callBackAnswer = sendAnswerCallbackQuery("Думай, только не долго, а то голова лопнет", true, buttonQuery);
        }

        //From Position choose buttons
        else if (buttonQuery.getData().equals("buttonCS")) {
            UserProfileData userProfileData = userDataCache.getUserProfileData(userId);
            userProfileData.setPosition("КС");
            userDataCache.saveUserProfileData(userId, userProfileData);
            userDataCache.setUsersCurrentBotState(userId, BotState.PROFILE_FILLED);
            callBackAnswer = new SendMessage(String.valueOf(chatId), "В каком городе работаешь?");
        }

        else if (buttonQuery.getData().equals("buttonRSM")) {
            UserProfileData userProfileData = userDataCache.getUserProfileData(userId);
            userProfileData.setPosition("РГКП / УРП");
            userDataCache.saveUserProfileData(userId, userProfileData);
            userDataCache.setUsersCurrentBotState(userId, BotState.PROFILE_FILLED);
            callBackAnswer = new SendMessage(String.valueOf(chatId), "В каком городе работаешь?");
        }

        else if (buttonQuery.getData().equals("buttonCO")) {
            UserProfileData userProfileData = userDataCache.getUserProfileData(userId);
            userProfileData.setPosition("Сотрудник ЦO");
            userDataCache.saveUserProfileData(userId, userProfileData);
            userDataCache.setUsersCurrentBotState(userId, BotState.PROFILE_FILLED);
            callBackAnswer = new SendMessage(String.valueOf(chatId), "В каком городе работаешь?");
        }

        else if (buttonQuery.getData().equals("buttonFO")) {
            UserProfileData userProfileData = userDataCache.getUserProfileData(userId);
            userProfileData.setPosition("Бывший сотрудник");
            userDataCache.saveUserProfileData(userId, userProfileData);
            userDataCache.setUsersCurrentBotState(userId, BotState.PROFILE_FILLED);
            callBackAnswer = new SendMessage(String.valueOf(chatId), "В каком городе работаешь?");
        }

        else {
            userDataCache.setUsersCurrentBotState(userId, BotState.SHOW_MAIN_MENU);
        }
        return callBackAnswer;
    }

    private AnswerCallbackQuery sendAnswerCallbackQuery(String text, boolean alert, CallbackQuery callbackquery) {
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setCallbackQueryId(callbackquery.getId());
        answerCallbackQuery.setShowAlert(alert);
        answerCallbackQuery.setText(text);
        return answerCallbackQuery;
    }

    @SneakyThrows
    public File getUsersProfile(int userId) {
        UserProfileData userProfileData = userDataCache.getUserProfileData(userId);
        File profileFile = ResourceUtils.getFile("classpath:static/docs/users_profile.txt");

        try (FileWriter fw = new FileWriter(profileFile.getAbsoluteFile());
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(userProfileData.toString());
        }
        return profileFile;
    }
}
