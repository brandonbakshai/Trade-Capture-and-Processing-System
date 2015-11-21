package trading;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.HostServices;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.NewOrderSingle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Manages all the operations done while interacting with the GUI
 *
 */
public class Controller {

    private HostServices hostServices ;
    private static int orderNumber = 1;

    // Linked variables to fxml file
    @FXML //  fx:id="symbol"
    private TextField symbol;
    @FXML //  fx:id="expiry"
    private TextField expiry;
    @FXML //  fx:id="lots"
    private TextField lots;
    @FXML //  fx:id="price"
    private TextField price;
    @FXML //  fx:id="trader"
    private TextField trader;
    @FXML //  fx:id="buy"
    private ToggleGroup buy;
    @FXML //  fx:id="type"
    private ChoiceBox type;


    /** Sets the variable hostServices used to open the CSV files
     *
     * @param hostServices      Host services of the application
     */
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices ;
    }

    /** Checks that all the fields of the trade are correctly filled
     *
     * @param actionEvent       Event fired with the mouse click
     */
    public void validateTrade(ActionEvent actionEvent) {
        boolean valid = true;
        double lot = 0;
        double pri = 0;
        TradeInfo tradeInfo = new TradeInfo();

        // We only use three symbols for easier testing
        if (!symbol.getText().equals("HH") && !symbol.getText().equals("HP") && !symbol.getText().equals("NN"))
            valid = false;

        if(expiry.getText().length() != 8 || !isNumeric(expiry.getText()))
            valid = false;

        if(isNumeric(lots.getText()))
            lot = Double.parseDouble(lots.getText());
        else
            valid = false;

        if(isNumeric(price.getText()))
            pri = Double.parseDouble(price.getText());
        else
            valid = false;

        if(trader.getText().isEmpty())
            valid = false;


        if(valid) {
            RadioButton buyTemp = (RadioButton)buy.getSelectedToggle();
            tradeInfo.setSymbol(symbol.getText());
            tradeInfo.setContractExpiry(expiry.getText());
            tradeInfo.setLots(lot);
            tradeInfo.setPrice(pri);
            tradeInfo.setTrader(trader.getText());
            tradeInfo.setBuy(buyTemp.getText().equals("Buy"));
            tradeInfo.setDate(LocalDate.now().toString());
            tradeInfo.setTime(LocalTime.now().toString());
            tradeInfo.setType(type.getValue().toString());


            SocketInitiator socketInitiator = null;
            try {
                SessionSettings initiatorSettings = new SessionSettings(
                        "./src/fix/ClientExchange.cfg");
                Application initiatorApplication = new ClientExchange();
                FileStoreFactory fileStoreFactory = new FileStoreFactory(
                        initiatorSettings);
                FileLogFactory fileLogFactory = new FileLogFactory(
                        initiatorSettings);
                MessageFactory messageFactory = new DefaultMessageFactory();
                socketInitiator = new SocketInitiator(initiatorApplication, fileStoreFactory, initiatorSettings, fileLogFactory, messageFactory);

                System.out.println("Placing order");
                System.out.println("Qty: " + tradeInfo.getLots() + ", type: " + tradeInfo.getType());
                if (tradeInfo.getType().equals("Limit")) {
                    System.out.println("Limit price: " + tradeInfo.getPrice());
                }

                socketInitiator.start();
                SessionID sessionId = socketInitiator.getSessions().get(0);
                Session.lookupSession(sessionId).logon();
                while(!Session.lookupSession(sessionId).isLoggedOn()){
                    System.out.println("Waiting for login success");
                    Thread.sleep(1000);
                }


                Thread.sleep(2000);

                bookSingleOrder(tradeInfo, sessionId);

                Session.lookupSession(sessionId).disconnect("Done",false);
                socketInitiator.stop();

                if (!buyTemp.getText().equals("Buy"))
                    lot *= -1;
                tradeInfo.setLots(lot);
//                StoreTrade(tradeInfo);
                symbol.setText("");
                expiry.setText("");
                lots.setText("");
                price.setText("");
                trader.setText("");

            } catch (ConfigError | InterruptedException | IOException e) {
                e.printStackTrace();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("");
            alert.setHeaderText(null);
            alert.setContentText("Please fill correctly all of the fields");
            alert.showAndWait();
        }
    }

    public static boolean isNumeric(String s) {
        double i;
        try {
            i = Double.parseDouble(s);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    private static void bookSingleOrder(TradeInfo tradeInfo, SessionID sessionID){
        //Unique order identifier
        ClOrdID orderId = new ClOrdID(Integer.toString(orderNumber++));

        //to be executed on the exchange
        HandlInst instruction = new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE);

        //Get symbol name
        Symbol symbol = new Symbol(tradeInfo.getSymbol());


        // buy or sell
        Side side = new Side(Side.SELL);
        if (tradeInfo.getBuy())
            side = new Side(Side.BUY);


        // Time of transaction
        TransactTime transactionTime = new TransactTime();

        // Type of our order, here we are assuming this is being executed on the exchange
        OrdType orderType = new OrdType(OrdType.MARKET);
        if (tradeInfo.getType().equals("Limit"))
            orderType = new OrdType(OrdType.LIMIT);
        if (tradeInfo.getType().equals("Pegged"))
            orderType = new OrdType(OrdType.PEGGED);

        NewOrderSingle order = new NewOrderSingle(orderId, instruction, symbol, side, transactionTime, orderType);


        // Quantity
        order.set(new OrderQty(tradeInfo.getLots()));

        // Price
        order.set(new Price(tradeInfo.getPrice()));
        order.set(new FutSettDate(tradeInfo.getContractExpiry()));


        try {
            Session.sendToTarget(order, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }


    /** Writes all the trades into a CSV file called "trade_report.csv"
     *
     * @param actionEvent       Event fired with the mouse click
     */
    public void writeCSV(ActionEvent actionEvent) {
        Database tradeDB = new Database();
        List trades = tradeDB.getTradeHistory();
        TradeInfo trade = new TradeInfo();
        FileWriter writer = null;
        try {
            writer = new FileWriter("trade_report.csv");
            writer.append("Symbol, Contract Expiry, Lots, Price, Type, Trader, Trade Date, Transaction Time\n");
            for (Object currentTrade : trades) {
                trade = (TradeInfo) currentTrade;
                writer.append(trade.getSymbol());
                writer.append(", ");
                writer.append(trade.getContractExpiry());
                writer.append(", ");
                writer.append(Double.toString(Math.abs(trade.getLots())));
                writer.append(", ");
                writer.append(Double.toString(trade.getPrice()));
                writer.append(", ");
                if (trade.getBuy())
                    writer.append("Buy");
                else
                    writer.append("Sell");
                writer.append(", ");
                writer.append(trade.getTrader());
                writer.append(", ");
                writer.append(trade.getDate());
                writer.append(", ");
                writer.append(trade.getTime());
                writer.append('\n');
            }

        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Writing to CSV failed");
            alert.setHeaderText(null);
            alert.setContentText("There was an error while writing the CSV file");
            alert.showAndWait();
        } finally {
            try {
                writer.flush();
                writer.close();
                File excelFile = new File("trade_report.csv");
                hostServices.showDocument(excelFile.toURI().toURL().toExternalForm());
            } catch (IOException e) {
                System.out.println("IO Error while closing the file writer");
                e.printStackTrace();
            } catch (NullPointerException e) {
                System.out.println("Error while closing the file writer");
                e.printStackTrace();
            }
        }

    }

    /** Writes all the trade aggregates into a CSV file called "trade_report_aggregates.csv"
     *
     * @param actionEvent       Event fired with the mouse click
     */
    public void writeAggregatesCSV(ActionEvent actionEvent) {
        Database tradeDB = new Database();
        List trades = tradeDB.getAggregates();
        TradeInfo trade = new TradeInfo();
        FileWriter writer = null;
        try {
            writer = new FileWriter("trade_report_aggregates.csv");
            writer.append("Symbol, Contract Expiry, Trader, Lots (Aggregate)\n");
            for (Object currentTrade : trades) {
                trade = (TradeInfo) currentTrade;
                writer.append(trade.getSymbol());
                writer.append(", ");
                writer.append(trade.getContractExpiry());
                writer.append(", ");
                writer.append(trade.getTrader());
                writer.append(", ");
                writer.append(Double.toString(trade.getLots()));
                writer.append('\n');
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Writing to CSV failed");
            alert.setHeaderText(null);
            alert.setContentText("There was an error while writing the CSV file");
            alert.showAndWait();
        } finally {
            try {
                writer.flush();
                writer.close();
                File excelFile = new File("trade_report_aggregates.csv");
                hostServices.showDocument(excelFile.toURI().toURL().toExternalForm());
            } catch (IOException e) {
                System.out.println("IO Error while closing the file writer");
                e.printStackTrace();
            } catch (NullPointerException e) {
                System.out.println("Error while closing the file writer");
                e.printStackTrace();
            }
        }

    }

    /** Calls the function that deletes the database
     *
     */
    public void deleteDB() {
        Database tradeDB = new Database();
        tradeDB.deleteDB(); // Deletes the database (for testing purposes only)
    }

    /** Calls the function that stores the trade info into the database and
     * tells the user the result
     *
     * @param tradeInfo     The object containing all the trade information
     */
    public void StoreTrade(TradeInfo tradeInfo) {
        Database tradeDB = new Database();
        boolean success = tradeDB.insert(tradeInfo);
        if (success) 
        {
            String message = "The trade has been stored\n" +
                    tradeInfo.getSymbol() + "\n" +
                    tradeInfo.getContractExpiry() + "\n" +
                    tradeInfo.getLots() + "\n" +
                    tradeInfo.getPrice() + "\n" +
                    tradeInfo.getTrader() + "\n";
            if(tradeInfo.getBuy())
                message += "Buy";
            else
                message += "Sell";
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Trade confirmation");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
        else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Trade error");
            alert.setHeaderText(null);
            alert.setContentText("There was an error connecting to the database");
            alert.showAndWait();
        }
    }
}
