package ru.home.mywizard_bot.service;

import org.springframework.stereotype.Service;
import java.util.Random;

/**
 * Генерирует предсказание
 *
 *  @author Банкир
 */
@Service
public class PredictionService {
    private final Random random = new Random();
    private final ReplyMessagesService messagesService;


    public PredictionService(ReplyMessagesService messagesService) {
        this.messagesService = messagesService;
    }


    public String getPredictionCS() {
            int predictionNumber = random.nextInt(50);
            String replyMessagePropertie = String.format("%s%d", "reply.prediction.cs", predictionNumber);
            return messagesService.getReplyText(replyMessagePropertie);
    }

    public String getPredictionRSM() {
        int predictionNumber = random.nextInt(40);
        String replyMessagePropertie = String.format("%s%d", "reply.prediction.rsm", predictionNumber);
        return messagesService.getReplyText(replyMessagePropertie);
    }

    public String getPredictionCO() {
        int predictionNumber = random.nextInt(30);
        String replyMessagePropertie = String.format("%s%d", "reply.prediction.co", predictionNumber);
        return messagesService.getReplyText(replyMessagePropertie);
    }
}
