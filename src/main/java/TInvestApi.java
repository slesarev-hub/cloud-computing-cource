import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import ru.tinkoff.piapi.core.InvestApi;

import java.io.FileReader;

public class TInvestApi {
    InvestApi api;
    public InvestApi() {
        FileReader reader = new FileReader("credentials.json");
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
        api = InvestApi.create(jsonObject.get("invest_api_token"));
    }
}
