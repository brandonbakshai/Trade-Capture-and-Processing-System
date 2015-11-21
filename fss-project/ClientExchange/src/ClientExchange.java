import java.io.IOException;
import java.util.Scanner;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.UnsupportedMessageType;
import quickfix.field.ClOrdID;
import quickfix.field.HandlInst;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.MessageCracker;
import quickfix.fix42.NewOrderSingle;

public class ClientExchange extends MessageCracker implements Application {

    /** (non-Javadoc)
     * @see quickfix.Application#onCreate(quickfix.SessionID)
     */
    @Override
    public void onCreate(SessionID sessionId) {
 
    }
 
    /** (non-Javadoc)
     * @see quickfix.Application#onLogon(quickfix.SessionID)
     */
    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("On logged on");
    }
 
    /** (non-Javadoc)
     * @see quickfix.Application#onLogout(quickfix.SessionID)
     */
    @Override
    public void onLogout(SessionID sessionId) {
 
    }
 
    /** (non-Javadoc)
     * @see quickfix.Application#toAdmin(quickfix.Message, quickfix.SessionID)
     */
    @Override
    public void toAdmin(Message message, SessionID sessionId) {
 
    }
 
    /** (non-Javadoc)
     * @see quickfix.Application#fromAdmin(quickfix.Message, quickfix.SessionID)
     */
    @Override
    public void fromAdmin(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
            RejectLogon {
 
    }
 
    /** (non-Javadoc)
     * @see quickfix.Application#toApp(quickfix.Message, quickfix.SessionID)
     */
    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
 
    }
 
    /*** (non-Javadoc)
     * @see quickfix.Application#fromApp(quickfix.Message, quickfix.SessionID)
     */
    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
            UnsupportedMessageType {
        crack(message, sessionId);
    }
     
    @Override
    public void onMessage(ExecutionReport message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        System.out.println("Received Execution report from server");
        System.out.println("Order Id : " + message.getOrderID().getValue());
        System.out.println("Order Status : " + message.getOrdStatus().getValue());
        System.out.println("Order Price : " + message.getPrice().getValue());
    }
    
    public static void main(String[] args) {
        SocketInitiator socketInitiator = null;
        try {
            SessionSettings initiatorSettings = new SessionSettings(
            		"./src/ClientExchange.cfg");
            Application initiatorApplication = new ClientExchange();
            FileStoreFactory fileStoreFactory = new FileStoreFactory(
                    initiatorSettings);
            FileLogFactory fileLogFactory = new FileLogFactory(
                    initiatorSettings);
            MessageFactory messageFactory = new DefaultMessageFactory();
            socketInitiator = new SocketInitiator(initiatorApplication, fileStoreFactory, initiatorSettings, fileLogFactory, messageFactory);
            socketInitiator.start();
            SessionID sessionId = socketInitiator.getSessions().get(0);
            Session.lookupSession(sessionId).logon();
            while(!Session.lookupSession(sessionId).isLoggedOn()){
                System.out.println("Waiting for login success");
                Thread.sleep(1000);
            }
            System.out.println("Logged In...");
             
            Thread.sleep(5000);
            bookSingleOrder(sessionId);
             
            System.out.println("Type to quit");
            Scanner scanner = new Scanner(System.in);
            scanner.next();
            Session.lookupSession(sessionId).disconnect("Done",false);
            socketInitiator.stop();
        } catch (ConfigError e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
     
    private static void bookSingleOrder(SessionID sessionID){
        //In real world this won't be a hardcoded value rather than a sequence.
        ClOrdID orderId = new ClOrdID("1");
        //to be executed on the exchange
        HandlInst instruction = new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE);
        //Since its FX currency pair name
        Symbol mainCurrency = new Symbol("EUR/USD");
        //Which side buy, sell
        Side side = new Side(Side.BUY);
        //Time of transaction
        TransactTime transactionTime = new TransactTime();
        //Type of our order, here we are assuming this is being executed on the exchange
        OrdType orderType = new OrdType(OrdType.FOREX_MARKET);
        NewOrderSingle newOrderSingle = new NewOrderSingle(orderId,instruction,mainCurrency, side, transactionTime,orderType);
        //Quantity
        newOrderSingle.set(new OrderQty(100));
        try {
            Session.sendToTarget(newOrderSingle, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }
    
}
