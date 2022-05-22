import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.Objects;
import java.util.logging.Level;

import static com.mongodb.client.model.Filters.eq;

public class MongoDB {
    private String host;
    private String port;

    MongoDB() {
        try {
            java.util.logging.Logger.getLogger("org.mongodb.driver").setLevel(Level.OFF);
            FileReader reader = new FileReader("credentials.json");
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            host = jsonObject.get("host").toString();
            port = jsonObject.get("port").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String setToken(String chatId, String userName, String token) {
        MongoClientURI connectionString = new MongoClientURI("mongodb://" + this.host + ":" + this.port);
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase("users");
        MongoCollection<Document> collection = database.getCollection("tokens");
        var doc = collection.find(eq("chatId", chatId)).first();
        Document newDoc = new Document("chatId", chatId)
                .append("userName", userName)
                .append("token", token);
        if (Objects.isNull(doc)) {
            database.getCollection(userName);
            collection.insertOne(newDoc);
            return "New user created";
        } else {
            collection.updateOne(eq("chatId", chatId), newDoc);
            return "User updated";
        }
    }

    public String getToken(String chatId) {
        MongoClientURI connectionString = new MongoClientURI("mongodb://" + this.host + ":" + this.port);
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase("users");
        MongoCollection<Document> collection = database.getCollection("tokens");
        var doc = collection.find(eq("chatId", chatId)).first();
        if (Objects.isNull(doc)) {
            return "No such user";
        } else {
            return doc.getString("token");
        }
    }

    public void newBuyOrder(String figi, String limitPrice, Integer lotCount, String chatId) {
        MongoClientURI connectionString = new MongoClientURI("mongodb://" + this.host + ":" + this.port);
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase("users");
        MongoCollection<Document> collection = database.getCollection(chatId + "orders");
        Document newDoc = new Document("orderType", "buy")
                .append("figi", figi)
                .append("limitPrice", limitPrice)
                .append("lotCount", lotCount);
        collection.insertOne(newDoc);
    }

    public void newSellOrder(String figi, String limitPrice, Integer lotCount, String chatId) {
        MongoClientURI connectionString = new MongoClientURI("mongodb://" + this.host + ":" + this.port);
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase("users");
        MongoCollection<Document> collection = database.getCollection(chatId + "orders");
        Document newDoc = new Document("orderType", "sell")
                .append("figi", figi)
                .append("limitPrice", limitPrice)
                .append("lotCount", lotCount);
        collection.insertOne(newDoc);
    }

    public MongoCollection<Document> getHistory(String chatId) {
        MongoClientURI connectionString = new MongoClientURI("mongodb://" + this.host + ":" + this.port);
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase("users");
        MongoCollection<Document> collection = database.getCollection(chatId + "orders");
        return collection;
    }
}
