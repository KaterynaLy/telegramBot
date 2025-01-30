package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "replyfasterbot";
    public static String TELEGRAM_BOT_TOKEN;
    public static String OPEN_AI_TOKEN;

    public DialogMode mode = DialogMode.MAIN;
    public ChatGPTService gptService = new ChatGPTService(OPEN_AI_TOKEN);
    private List<String> chat;
    private UserInfo myInfo;
    private UserInfo personInfo;
    private int questions;

    static {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config.properties"));
            TELEGRAM_BOT_TOKEN = properties.getProperty("TELEGRAM_BOT_TOKEN");
            OPEN_AI_TOKEN = properties.getProperty("OPEN_AI_TOKEN");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        String message = getMessageText();

        try {
            switch (message) {
                case "/start" -> {
                    mode = DialogMode.MAIN;
                    showMainMenu("Main menu", "/start",
                            "головне меню бота", "/profile",
                            "генерація Tinder-профілю", "/opener",
                            "повідомлення для знайомства", "/message",
                            "листування від вашого імені", "/date",
                            "Chat GPT", "/gpt");
                    sendPhotoMessage("main");
                    String menu = loadMessage("main");
                    sendTextMessage(menu);
                    return;
                }
                case "/gpt" -> {
                    mode = DialogMode.GPT;
                    String gptMessage = loadMessage("gpt");
                    sendTextMessage(gptMessage);
                    sendPhotoMessage("gpt");
                    return;
                }
                case "/date" -> {
                    mode = DialogMode.DATE;
                    sendPhotoMessage("date");
                    String dateMessage = loadMessage("date");
                    sendTextButtonsMessage(dateMessage, "Ariana", "date_grande", "Margo", "date_robbie", "Zendeya", "date_zendaya", "Gosling", "date_gosling", "Hardy", "date_hardy");
                    return;
                }
                case "/message" -> {
                    mode = DialogMode.MESSAGE;
                    sendPhotoMessage("message");
                    String gptMessageHelper = loadMessage("message");
                    sendTextMessage(gptMessageHelper);
                    sendTextButtonsMessage(gptMessageHelper, "Next message ", "message_next", "Invite dating ", "message_date");
                    chat = new ArrayList<>();
                    return;
                }
                case "/profile" -> {
                    mode = DialogMode.PROFILE;
                    sendPhotoMessage("profile");
                    String profileMessage = loadMessage("profile");
                    sendTextMessage(profileMessage);

                    myInfo = new UserInfo();
                    questions = 1;
                    sendTextMessage("Name?");
                    return;
                }
                case "/opener" -> {
                    mode = DialogMode.OPENER;
                    sendPhotoMessage("opener");
                    String profileMessage = loadMessage("opener");
                    sendTextMessage(profileMessage);

                    personInfo = new UserInfo();
                    questions = 1;
                    sendTextMessage("Name?");
                    return;
                }
            }

            switch (mode) {
                case GPT -> {
                    String prompt = loadPrompt("gpt");
                    Message msg = sendTextMessage("ChatGPT is in process ..");
                    try {
                        String answer = gptService.sendMessage(prompt, message);
                        updateTextMessage(msg, answer);
                    } catch (Exception e) {
                        updateTextMessage(msg, "The was a mistake. chat should be reloaded");
                        e.printStackTrace();
                    }
                }
                case DATE -> {
                    String query = getCallbackQueryButtonKey();
                    if (query.startsWith("date_")) {
                        sendPhotoMessage(query);
                        String prompt = loadPrompt(query);
                        gptService.setPrompt(prompt);
                        return;
                    }
                    Message msg = sendTextMessage("ChatGPT is in process ..");
                    try {
                        String answer = gptService.addMessage(message);
                        updateTextMessage(msg, answer);
                    } catch (Exception e) {
                        updateTextMessage(msg, "The was a mistake. chat should be reloaded");
                        e.printStackTrace();
                    }
                }
                case MESSAGE -> {
                    String query = getCallbackQueryButtonKey();
                    if (query.startsWith("message_")) {
                        String prompt = loadPrompt(query);
                        String history = String.join("/n/n", chat);
                        Message msg = sendTextMessage("Wait a bit...");
                        try {
                            String answer = gptService.sendMessage(prompt, history);
                            updateTextMessage(msg, answer);
                        } catch (Exception e) {
                            updateTextMessage(msg, "The was a mistake. chat should be reloaded");
                            e.printStackTrace();
                        }
                    }
                    chat.add(message);
                }
                case PROFILE -> {
                    if (questions <= 6) {
                        askQuestion(message, myInfo, "profile");
                    }

                }
                case OPENER -> {
                    if (questions <= 6) {
                        askQuestion(message, personInfo, "opener");
                    }
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void askQuestion(String message, UserInfo user, String profileName) {
        switch (questions) {
            case 1 -> {
                user.name = message;
                questions = 2;
                sendTextMessage("Age?");
                return;
            }
            case 2 -> {
                user.age = message;
                questions = 3;
                sendTextMessage("City?");
                return;
            }
            case 3 -> {
                user.city = message;
                questions = 4;
                sendTextMessage("Carrea?");
                return;
            }
            case 4 -> {
                user.occupation = message;
                sendTextMessage("Hobby?");
                return;
            }
            case 5 -> {
                user.hobby = message;
                sendTextMessage("Goals?");
                return;
            }
            case 6 -> {
                user.goals = message;

                String prompt = loadPrompt(profileName);
                Message msg = sendTextMessage("Wait..");

                String answer = gptService.sendMessage(prompt, user.toString());
                updateTextMessage(msg, answer);

                return;
            }
        }
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
