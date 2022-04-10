import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;

import java.io.FileReader;
import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class InvestApiWrapper {
    MongoDB mongoApi;

    public InvestApiWrapper() {
        FileReader reader = null;
        mongoApi = new MongoDB();
        try {
            reader = new FileReader("credentials.json");
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private InvestApi getInvestApi(String chatId) {
        var token = mongoApi.getToken(chatId);
        if ("No such user".equals(token)) {
            return null;
        }
        return InvestApi.createSandbox(token);
    }

    public String totalAmountCurrencies(String chatId) {
        var investApi = getInvestApi(chatId);
        var accounts = investApi.getSandboxService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();
        var portfolio = investApi.getSandboxService().getPortfolioSync(mainAccount);
        return portfolio.getTotalAmountCurrencies().toString();
    }

    public String totalAmountShares(String chatId) {
        var investApi = getInvestApi(chatId);
        var accounts = investApi.getSandboxService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();
        var portfolio = investApi.getSandboxService().getPortfolioSync(mainAccount);
        return portfolio.getTotalAmountShares().toString();
    }

    public String newBuyOrder(String figi, String limitPrice, Integer lotCount, String chatId) {
        var stopPrice = Quotation.newBuilder().setUnits(Long.parseLong(limitPrice)).build();
        var investApi = getInvestApi(chatId);
        var accounts = investApi.getSandboxService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();
        var response = investApi.getSandboxService().postOrderSync(figi,
                lotCount,
                stopPrice,
                OrderDirection.ORDER_DIRECTION_BUY,
                mainAccount,
                OrderType.ORDER_TYPE_LIMIT,
                "1234");
        return "id\n" + response.getOrderId();
    }

    public String newSellOrder(String figi, String limitPrice, Integer lotCount, String chatId) {
        var stopPrice = Quotation.newBuilder().setUnits(Long.parseLong(limitPrice)).build();
        var investApi = getInvestApi(chatId);
        var accounts = investApi.getSandboxService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();
        var response = investApi.getSandboxService().postOrderSync(figi,
                lotCount,
                stopPrice,
                OrderDirection.ORDER_DIRECTION_SELL,
                mainAccount,
                OrderType.ORDER_TYPE_LIMIT,
                "1234");
        return "id\n" + response.getOrderId();
    }

    public String buyOrderList(String chatId) {
        var investApi = getInvestApi(chatId);
        var accounts = investApi.getSandboxService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();
        var buyOrders = investApi.getSandboxService().getOrdersSync(mainAccount);
        StringBuilder message = new StringBuilder("Стоп заявки:\n");
        for (OrderState order : buyOrders) {
            var priceInfo = investApi.getMarketDataService().getLastPricesSync(List.of(order.getFigi()));
            while(!priceInfo.get(0).hasPrice()) {
                priceInfo = investApi.getMarketDataService().getLastPricesSync(List.of(order.getFigi()));
            }
            message.append("id ").append(order.getOrderId()).append("\n")
                    .append("figi ").append(order.getFigi()).append("\n")
                    .append("лотность ").append(order.getLotsRequested()).append("\n")
                    .append("текущая цена ").append(extractPrice(priceInfo.get(0).getPrice().toString())).append("\n")
                    .append("лимитная цена ").append(extractPrice(order.getInitialOrderPrice().toString())).append("\n");
        }
        return message.toString();
    }

    public String extractPrice(String price) {
        Pattern extractPrice = Pattern.compile("\\d+");
        Matcher matchPrice = extractPrice.matcher(price);
        matchPrice.find();
        return matchPrice.group();
    }

    public String withdrawStopOrder(String stopOrderId, String chatId) {
        var investApi = getInvestApi(chatId);
        var accounts = investApi.getSandboxService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();
        investApi.getSandboxService().cancelOrder(mainAccount, stopOrderId);
        return "стоп заявка с id " + stopOrderId + " отменена";
    }

    public String instrumentByFigi(String figi, String chatId) {
        var investApi = getInvestApi(chatId);
        var accounts = investApi.getSandboxService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();
        var figInfo = investApi.getInstrumentsService().getInstrumentByFigiSync(figi);
        var priceInfo = investApi.getMarketDataService().getLastPricesSync(List.of(figi));
        while(!priceInfo.get(0).hasPrice()) {
            priceInfo = investApi.getMarketDataService().getLastPricesSync(List.of(figi));
        }
        return MessageFormat.format("{0}\nцена: {1}",
                figInfo.getTicker(),
                extractPrice(priceInfo.get(0).getPrice().toString()));
    }

    public String setToken(String chatId, String userName, String token) {
        return mongoApi.setToken(chatId, userName, token);
    }

    public String showToken(String chatId) {
        return mongoApi.getToken(chatId);
    }
}
