package trading;

import java.util.*;

import static java.util.Arrays.asList;

import com.mongodb.*;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/** Manages all the database operations
 *
 */
public class Database {
  
    // data fields
    private MongoCollection table;
    private MongoDatabase db;

    /** Tries to open a connection to the database "fss-project" and the collection "trades"
     *
     */
    public Database()
    {
        try {
            // connect to mongo
            MongoClient mongo = new MongoClient("localhost", 27017);
            // get database
            db = mongo.getDatabase("fss-project");
            // get trades table
            table = db.getCollection("trades");
        } catch (MongoException e) {
            e.printStackTrace();
        }
    }

    /** Deletes all the information in the database
     *
     */
    public void deleteDB() {
        db.getCollection("trades").drop();
    }

    /** Gets the collection that contains the trade information
     *
     * @return      The collection "trades" of the database "fss-project"
     */
    public MongoCollection getTable()
    {
        return this.table;
    }

    /** Inserts a new trade in the database
     *
     * @param tradeInfo     The trade to store in the database
     * @return      boolean indicating if the insertion of the trade was successful
     */
    public boolean insert(TradeInfo tradeInfo)
    {  
        try {
            Document doc = new Document();
            doc.put("symbol",          tradeInfo.getSymbol());
            doc.put("contractExpiry", tradeInfo.getContractExpiry());
            doc.put("lots",            tradeInfo.getLots());
            doc.put("price",           tradeInfo.getPrice());
            doc.put("trader",          tradeInfo.getTrader());
            doc.put("buy",             tradeInfo.getBuy());
            doc.put("date",            tradeInfo.getDate());
            doc.put("time",            tradeInfo.getTime());

            table.insertOne(doc);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } 
        return true;
    }

    /** Returns all the trades stored in the database
     *
     * @return      A List with all the trades in the database
     */
    public List getTradeHistory()
    {
        List<TradeInfo> list = new ArrayList<TradeInfo>();
        MongoCursor cursor = getTable().find().iterator();
        while (cursor.hasNext()) 
        {
            Document trade = (Document)cursor.next();
            TradeInfo tradeInfo = new TradeInfo();
            tradeInfo.setSymbol((String) trade.get("symbol"));
            tradeInfo.setContractExpiry((String) trade.get("contractExpiry"));
            tradeInfo.setLots((double) trade.get("lots"));
            tradeInfo.setPrice((double) trade.get("price"));
            tradeInfo.setTrader((String) trade.get("trader"));
            tradeInfo.setBuy((boolean) trade.get("buy"));
            tradeInfo.setDate((String) trade.get("date"));
            tradeInfo.setTime((String) trade.get("time"));
            list.add(tradeInfo);
        }
        return list;
    }

    /** Calculates the aggregate positions and returns it as a list
     *
     * @return      A list with the aggregate positions
     */
    public List getAggregates() {
        Map<String, Object> dbObjIdMap = new HashMap<String, Object>();
        dbObjIdMap.put("symbol", "$symbol");
        dbObjIdMap.put("trader", "$trader");
        dbObjIdMap.put("contractExpiry", "$contractExpiry");
        DBObject groupFields = new BasicDBObject( "_id", new BasicDBObject(dbObjIdMap));
        dbObjIdMap = new HashMap<String, Object>();
        dbObjIdMap.put("$sum", "$lots");
        groupFields.put("count", dbObjIdMap);

        AggregateIterable<Document> iterable = db.getCollection("trades").aggregate(asList(
                new Document("$group", groupFields)));

        List<TradeInfo> list = new ArrayList<>();

        iterable.forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {
                TradeInfo tradeInfo = new TradeInfo();
                Document temp = (Document) document.get("_id");
                tradeInfo.setSymbol((String) temp.get("symbol"));
                tradeInfo.setContractExpiry((String) temp.get("contractExpiry"));
                tradeInfo.setTrader((String) temp.get("trader"));
                tradeInfo.setLots((double) document.get("count"));
                list.add(tradeInfo);
                System.out.println();
            }
        });
        return list;
    }
}
