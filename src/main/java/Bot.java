import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.FileReader;
import java.io.IOException;

public class Bot extends TelegramLongPollingBot {
    TInvestApi investApi;

    Bot() {
        investApi = new TInvestApi();
    }

    @Override
    public void onUpdateReceived(Update update) {
        SendMessage message = new SendMessage();
        try {
            String chatId = update.getMessage().getChatId().toString();
            message.setChatId(chatId);
            if (update.hasMessage() && update.getMessage().hasText()) {
                String[] request = update.getMessage().getText().split("\\s+");
                String response = null;
                response = parseMessage(request);
                message.setText(response);
            } else {
                message.setText("Server error");
            }
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String parseMessage(String[] request) throws Exception {
        String command = request[0];
        if (command.equals("/currencies")) {
            return investApi.totalAmountCurrencies();
        } else if (command.equals("/shares")) {
            return investApi.totalAmountShares();
        } else if (command.equals("/figi")) {
            return investApi.instrumentByFigi(request[1]);
        } else if (command.equals("/newstop")) {
            return investApi.newStopOrder(request[1], request[2]);
        } else if (command.equals("/withdrawstop")) {
            return investApi.withdrawStopOrder(request[1]);
        }  else if (command.equals("/liststop")) {
            return investApi.stopOrderList();
        }else {
            throw new Exception("Parse error");
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
