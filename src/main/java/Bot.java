import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    InvestApiWrapper investApi;

    Bot() {
        investApi = new InvestApiWrapper();
    }

    @Override
    public void onUpdateReceived(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String response = null;
                response = parseMessageTextResponse(update);
                if (response.equals("Photo")){
                    SendPhoto msgPhoto = new SendPhoto();
                    msgPhoto.setChatId(chatId);
                    response = parseMessagePhotoResponse(update);
                    var file = new InputFile(new File(response));
                    msgPhoto.setPhoto(file);
                    execute(msgPhoto);
                } else if (response.equals("Doc")) {
                    var msgDoc = new SendDocument();
                    msgDoc.setChatId(chatId);
                    response = parseMessageDocResponse(update);
                    var file = new InputFile(new File(response));
                    msgDoc.setDocument(file);
                    execute(msgDoc);
                } else if (response.equals("Keyboard")) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Keyboard switch");
                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> keyboard = new ArrayList<>();
                    KeyboardRow row = new KeyboardRow();
                    row.add("buylist");
                    row.add("shares");
                    keyboard.add(row);
                    row = new KeyboardRow();
                    row.add("moex plot");
                    row.add("usd rub plot");
                    row.add("s&p500 plot");
                    keyboard.add(row);
                    keyboardMarkup.setKeyboard(keyboard);
                    message.setReplyMarkup(keyboardMarkup);
                    execute(message);
                } else {
                    SendMessage msg = new SendMessage();
                    msg.setChatId(chatId);
                    msg.setText(response);
                    execute(msg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String parseMessageDocResponse(Update update) {
        String[] request = update.getMessage().getText().split("\\s+");
        String command = request[0];
        String chatId = update.getMessage().getChatId().toString();
        return investApi.history(chatId);
    }

    private String parseMessagePhotoResponse(Update update) throws Exception {
        String[] request = update.getMessage().getText().split("\\s+");
        String command = request[0];
        String chatId = update.getMessage().getChatId().toString();
        if (command.equals("/candle")) {
            return investApi.plotByFigi(request[1], chatId);
        } else if (command.equals("moex")) {
            return investApi.moexPlot(chatId);
        } else if (command.equals("usd")) {
            return investApi.usdPlot(chatId);
        } else if (command.equals("s&p500")) {
            return investApi.spPlot(chatId);
        } else  {
            throw new Exception("Parse error");
        }
    }

    private String parseMessageTextResponse(Update update) throws Exception {
        String[] request = update.getMessage().getText().split("\\s+");
        String command = request[0];
        String chatId = update.getMessage().getChatId().toString();
        if (command.equals("/currencies")) {
            return investApi.totalAmountCurrencies(chatId);
        } else if (command.equals("/shares") || command.equals("shares")) {
            return investApi.totalAmountShares(chatId);
        } else if (command.equals("/figi")) {
            return investApi.instrumentByFigi(request[1], chatId);
        } else if (command.equals("/newbuy")) {
            return investApi.newBuyOrder(request[1], request[2], Integer.valueOf(request[3]), chatId);
        } else if (command.equals("/newsell")) {
            return investApi.newSellOrder(request[1], request[2], Integer.valueOf(request[3]), chatId);
        } else if (command.equals("/withdrawstop")) {
            return investApi.withdrawStopOrder(request[1], chatId);
        } else if (command.equals("/buylist") || command.equals("buylist")) {
            return investApi.buyOrderList(chatId);
        } else if (command.equals("/settoken")) {
            String userName = update.getMessage().getChat().getUserName();
            return investApi.setToken(chatId, userName, request[1]);
        } else if (command.equals("/showtoken")) {
            return investApi.showToken(chatId);
        } else if (command.equals("/candle") || command.equals("moex") || command.equals("usd") || command.equals("s&p500")) {
            return "Photo";
        } else if (command.equals("/switch")) {
            return "Keyboard";
        } else if (command.equals("/history")) {
            return "Doc";
        } else {
            return "No match";
        }
    }

    @Override
    public String getBotUsername() {
        try {
            FileReader reader = new FileReader("credentials.json");
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            return jsonObject.get("bot_name").toString();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getBotToken() {
        try {
            FileReader reader = new FileReader("credentials.json");
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            return jsonObject.get("bot_token").toString();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
