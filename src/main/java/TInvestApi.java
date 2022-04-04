import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;

import java.io.FileReader;
import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TInvestApi {
    InvestApi api;
    String mainAccount;

    public TInvestApi() {
        FileReader reader = null;
        try {
            reader = new FileReader("credentials.json");
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            api = InvestApi.createSandbox(jsonObject.get("invest_api_token").toString());
            var accounts = api.getSandboxService().getAccountsSync();
            mainAccount = accounts.get(0).getId();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String totalAmountCurrencies() {
        var portfolio = api.getSandboxService().getPortfolioSync(mainAccount);
        return portfolio.getTotalAmountCurrencies().toString();
    }

    public String totalAmountShares() {
        var portfolio = api.getSandboxService().getPortfolioSync(mainAccount);
        return portfolio.getTotalAmountShares().toString();
    }

    public String newStopOrder(String figi, String limitPrice) {
        var stopPrice = Quotation.newBuilder().setUnits(Long.parseLong(limitPrice)).build();
        var response = api.getSandboxService().postOrderSync(figi,
                1,
                stopPrice,
                OrderDirection.ORDER_DIRECTION_BUY,
                mainAccount,
                OrderType.ORDER_TYPE_LIMIT,
                "1");
        return "id\n" + response.getOrderId();
    }

    public String stopOrderList() {
        var stopOrders = api.getSandboxService().getOrdersSync(mainAccount);
        StringBuilder message = new StringBuilder("Стоп заявки:\n");
        for (OrderState order : stopOrders) {
            var priceInfo = api.getMarketDataService().getLastPricesSync(List.of(order.getFigi()));
            message.append("id ").append(order.getOrderId()).append("\n")
                    .append("figi ").append(order.getFigi()).append("\n")
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

    public String withdrawStopOrder(String stopOrderId) {
        api.getSandboxService().cancelOrder(mainAccount, stopOrderId);
        return "стоп заявка с id " + stopOrderId + " отменена";
    }

    public String instrumentByFigi(String figi) {
        var figInfo = api.getInstrumentsService().getInstrumentByFigiSync(figi);
        var priceInfo = api.getMarketDataService().getLastPricesSync(List.of(figi));
        return MessageFormat.format("{0}\nцена: {1}",
                figInfo.getTicker(),
                extractPrice(priceInfo.get(0).getPrice().toString()));
    }
}
