import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

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

    public String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    public String convertToCSV(List<String> data) {
        return data.stream()
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    public void writeCSV(List<List<String>> dataLines) throws IOException {
        var oldFile = new File("candle.csv");
        oldFile.delete();

        var oldPhoto = new File("plot.png");
        oldPhoto.delete();

        File csvOutputFile = new File("candle.csv");
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            dataLines.stream()
                    .map(this::convertToCSV)
                    .forEach(pw::println);
        }
        assertTrue(csvOutputFile.exists());
    }

    public String plotByFigi(String figi, String chatId) {
        var investApi = getInvestApi(chatId);
        var accounts = investApi.getSandboxService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();
        var candles = investApi.getMarketDataService().getCandlesSync(figi,
                Instant.now().minus(30, ChronoUnit.DAYS),
                Instant.now(),
                CandleInterval.CANDLE_INTERVAL_DAY);
        List<List<String>> dataLines = new ArrayList<>();
        List<String> line = new ArrayList<>();
        line.add("date");
        line.add("open");
        line.add("high");
        line.add("low");
        line.add("close");
        dataLines.add(line);
        for (var candle : candles) {
            line = new ArrayList<>();
            line.add(String.valueOf(candle.getTime().getSeconds()));
            line.add(candle.getOpen().getUnits()+"."+candle.getOpen().getNano());
            line.add(candle.getHigh().getUnits()+"."+candle.getHigh().getNano());
            line.add(candle.getLow().getUnits()+"."+candle.getLow().getNano());
            line.add(candle.getClose().getUnits()+"."+candle.getClose().getNano());
            dataLines.add(line);
        }
        try {
            writeCSV(dataLines);
            var figInfo = investApi.getInstrumentsService().getInstrumentByFigiSync(figi);
            return plotCsv(figInfo.getTicker());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String plotCsv(String figi) {
        try {
            Runtime.getRuntime().exec("python3 plot.py " + figi).waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "plot.png";
    }

    public String moexPlot(String chatId) {
        return plotByFigi("BBG004730JJ5", chatId);
    }

    public String usdPlot(String chatId) {
        return plotByFigi("BBG0013HGFT4", chatId);
    }

    public String spPlot(String chatId) {
        var investApi = getInvestApi(chatId);
        var a = investApi.getInstrumentsService().getAllBondsSync().stream().filter(e -> e.getTicker().equals("TSPX")).collect(Collectors.toList());;
        var b = investApi.getInstrumentsService().getAllEtfsSync().stream().filter(e -> e.getTicker().equals("TSPX")).collect(Collectors.toList());
        var c = investApi.getInstrumentsService().getAllSharesSync().stream().filter(e -> e.getTicker().equals("TSPX")).collect(Collectors.toList());
        return plotByFigi("TCS00A102EQ8", chatId);
    }
}
