import java.util.Arrays;
import java.util.Random;

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
import quickfix.SocketAcceptor;
import quickfix.UnsupportedMessageType;
import quickfix.field.AvgPx;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecTransType;
import quickfix.field.ExecType;
import quickfix.field.LeavesQty;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.MessageCracker;
import quickfix.fix42.NewOrderSingle;

public class StockExchange extends MessageCracker implements Application {

    private int orderNum = 1;
    private int execNum = 1;

    // Initial market value (We have 3 different prices in the market)
    public double[] hh = {50, 50.1, 50.3};
    public double[] hp = {47.2, 46.8, 46.9};


    // Price movement limit
    public double hhMax = 52.5;
    public double hhMin = 47.5;
    public double hpMax = 48;
    public double hpMin = 45;


    /**
     * (non-Javadoc)
     *
     * @see quickfix.Application#onCreate(quickfix.SessionID)
     */
    @Override
    public void onCreate(SessionID sessionId) {
        System.out.println("Executor Session Created with SessionID = "
                + sessionId);
    }

    /**
     * (non-Javadoc)
     *
     * @see quickfix.Application#onLogon(quickfix.SessionID)
     */
    @Override
    public void onLogon(SessionID sessionId) {

    }

    /**
     * (non-Javadoc)
     *
     * @see quickfix.Application#onLogout(quickfix.SessionID)
     */
    @Override
    public void onLogout(SessionID sessionId) {

    }

    /**
     * (non-Javadoc)
     *
     * @see quickfix.Application#toAdmin(quickfix.Message, quickfix.SessionID)
     */
    @Override
    public void toAdmin(Message message, SessionID sessionId) {

    }

    /**
     * (non-Javadoc)
     *
     * @see quickfix.Application#fromAdmin(quickfix.Message, quickfix.SessionID)
     */
    @Override
    public void fromAdmin(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
            RejectLogon {

    }

    /**
     * (non-Javadoc)
     *
     * @see quickfix.Application#toApp(quickfix.Message, quickfix.SessionID)
     */
    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {

    }

    /**
     * (non-Javadoc)
     *
     * @see quickfix.Application#fromApp(quickfix.Message, quickfix.SessionID)
     */
    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
            UnsupportedMessageType {
        crack(message, sessionId);
    }

    public void changePrice() {
        double movement;
        double price;
        for (int i = 0; i < 3; i++) {
            price = hh[i];
            movement = Math.random() - .5 + price;
            // Limit the price
            price = (movement > hhMax) ? hhMax : movement;
            price = (movement < hhMin) ? hhMin : movement;
            hh[i] = price;
        }
        for (int i = 0; i < 3; i++) {
            price = hp[i];
            movement = Math.random() - .5 + price;
            // Limit the price
            price = (movement < hpMin) ? hpMin : movement;
            price = (movement > hpMax) ? hpMax : movement;
            hp[i] = price;
        }
    }

    public void onMessage(NewOrderSingle message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        OrdType orderType = message.getOrdType();
        Symbol symbol = message.getSymbol();
        Price price = message.getPrice();
        OrderQty qty = message.getOrderQty();
        OrderID orderNumber = new OrderID(Integer.toString(orderNum++));
        ExecID execId = new ExecID(Integer.toString(execNum++));
        ExecTransType exectutionTransactioType = new ExecTransType(ExecTransType.NEW);
        //Represents side, 1=buy, 2=sell
        Side side = message.getSide();
        //Hardcoded since we are not using it
        LeavesQty leavesQty = new LeavesQty(100);
        //Hardcoded since we are not using it
        CumQty cummulativeQuantity = new CumQty(100);
        //Hardcoded since we are not using it
        AvgPx avgPx = new AvgPx(0);
        ExecType purposeOfExecutionReport;
        OrdStatus orderStatus;
        ExecutionReport executionReport;


        int qtyd = (int) qty.getValue();
        char type = orderType.getValue();
        double priced = price.getValue();
        String sym = symbol.getValue();
        int maxQty = qtyd;
        int sellingQty;

        System.out.println("Order received, executing order");
        // While the order has not been completely filled check the market
        while (qtyd > 0) {

            // Check the least expensive orders first
            Arrays.sort(hh);
            Arrays.sort(hp);

            System.out.println("Prices (HH): [" + hh[0] + ", " + hh[1] + ", " + hh[2] + "]");
            System.out.println("Prices (HP): [" + hp[0] + ", " + hp[1] + ", " + hp[2] + "]");

            // Market order
            if (type == '1') {
                if (sym.equals("HH")) {
                    for (double currPrice : hh) {
                        if (qtyd <= 0) break;
                        price.setValue(currPrice);
                        sellingQty = (int) Math.round(Math.random() * maxQty);

                        if (sellingQty >= qtyd) {
                            qty.setValue(qtyd);
                            purposeOfExecutionReport = new ExecType(ExecType.FILL);
                            orderStatus = new OrdStatus(OrdStatus.FILLED);
                            qtyd = 0;
                        }
                        else {
                            qty.setValue(sellingQty);
                            purposeOfExecutionReport = new ExecType(ExecType.PARTIAL_FILL);
                            orderStatus = new OrdStatus(OrdStatus.PARTIALLY_FILLED);
                            qtyd -= sellingQty;
                        }
                        executionReport = new ExecutionReport(orderNumber, execId, exectutionTransactioType,
                                purposeOfExecutionReport, orderStatus, symbol, side, leavesQty, cummulativeQuantity, avgPx);
                        executionReport.set(price);
                        executionReport.set(qty);
                        try {
                            Session.sendToTarget(executionReport, sessionID);
                            System.out.println("Execution report sent");
                        } catch (SessionNotFound e) {
                            System.out.println("Cannot communicate with client");
                        }
                    }
                }
                if (sym.equals("HP")) {
                    for (double currPrice : hp) {
                        if (qtyd <= 0) break;
                        price.setValue(currPrice);
                        sellingQty = (int) Math.round(Math.random() * maxQty);
                        if (sellingQty >= qtyd) {
                            qty.setValue(qtyd);
                            purposeOfExecutionReport = new ExecType(ExecType.FILL);
                            orderStatus = new OrdStatus(OrdStatus.FILLED);
                            qtyd = 0;
                        }
                        else {
                            qty.setValue(sellingQty);
                            purposeOfExecutionReport = new ExecType(ExecType.PARTIAL_FILL);
                            orderStatus = new OrdStatus(OrdStatus.PARTIALLY_FILLED);
                            qtyd -= sellingQty;
                        }
                        executionReport = new ExecutionReport(orderNumber, execId, exectutionTransactioType,
                                purposeOfExecutionReport, orderStatus, symbol, side, leavesQty, cummulativeQuantity, avgPx);
                        executionReport.set(price);
                        executionReport.set(qty);
                        try {
                            Session.sendToTarget(executionReport, sessionID);
                            System.out.println("Execution report sent");
                        } catch (SessionNotFound e) {
                            System.out.println("Cannot communicate with client");
                        }
                    }
                }
            }


            // Limit order
            if (type == '2') {
                if (sym.equals("HH")) {
                    for (double currPrice : hh) {
                        if (qtyd <= 0) break;
                        if (currPrice <= priced) {
                            System.out.println(currPrice + " < " + priced);
                            price.setValue(currPrice);
                            sellingQty = (int) Math.round(Math.random() * maxQty);
                            if (sellingQty >= qtyd) {
                                qty.setValue(qtyd);
                                purposeOfExecutionReport = new ExecType(ExecType.FILL);
                                orderStatus = new OrdStatus(OrdStatus.FILLED);
                                qtyd = 0;
                            }
                            else {
                                qty.setValue(sellingQty);
                                purposeOfExecutionReport = new ExecType(ExecType.PARTIAL_FILL);
                                orderStatus = new OrdStatus(OrdStatus.PARTIALLY_FILLED);
                                qtyd -= sellingQty;
                            }
                            executionReport = new ExecutionReport(orderNumber, execId, exectutionTransactioType,
                                    purposeOfExecutionReport, orderStatus, symbol, side, leavesQty, cummulativeQuantity, avgPx);
                            executionReport.set(price);
                            executionReport.set(qty);
                            try {
                                Session.sendToTarget(executionReport, sessionID);
                                System.out.println("Execution report sent");
                            } catch (SessionNotFound e) {
                                System.out.println("Cannot communicate with client");
                            }
                        }
                    }
                }
                if (sym.equals("HP")) {
                    for (double currPrice : hp) {
                        if (qtyd <= 0) break;
                        if (currPrice <= priced) {
                            price.setValue(currPrice);
                            sellingQty = (int) Math.round(Math.random() * maxQty);
                            if (sellingQty >= qtyd) {
                                qty.setValue(qtyd);
                                purposeOfExecutionReport = new ExecType(ExecType.FILL);
                                orderStatus = new OrdStatus(OrdStatus.FILLED);
                                qtyd = 0;
                            }
                            else {
                                qty.setValue(sellingQty);
                                purposeOfExecutionReport = new ExecType(ExecType.PARTIAL_FILL);
                                orderStatus = new OrdStatus(OrdStatus.PARTIALLY_FILLED);
                                qtyd -= sellingQty;
                            }
                            executionReport = new ExecutionReport(orderNumber, execId, exectutionTransactioType,
                                    purposeOfExecutionReport, orderStatus, symbol, side, leavesQty, cummulativeQuantity, avgPx);
                            executionReport.set(price);
                            executionReport.set(qty);
                            try {
                                Session.sendToTarget(executionReport, sessionID);
                                System.out.println("Execution report sent");
                            } catch (SessionNotFound e) {
                                System.out.println("Cannot communicate with client");
                            }
                        }
                    }
                }
            }


            // Pegged order
            if (type == 'P') {
                if (sym.equals("HH")) {
                    double currPrice = hh[0];
                    price.setValue(currPrice);
                    sellingQty = (int) Math.round(Math.random() * maxQty);
                    if (sellingQty >= qtyd) {
                        qty.setValue(qtyd);
                        purposeOfExecutionReport = new ExecType(ExecType.FILL);
                        orderStatus = new OrdStatus(OrdStatus.FILLED);
                        qtyd = 0;
                    }
                    else {
                        qty.setValue(sellingQty);
                        purposeOfExecutionReport = new ExecType(ExecType.PARTIAL_FILL);
                        orderStatus = new OrdStatus(OrdStatus.PARTIALLY_FILLED);
                        qtyd -= sellingQty;
                    }
                    executionReport = new ExecutionReport(orderNumber, execId, exectutionTransactioType,
                            purposeOfExecutionReport, orderStatus, symbol, side, leavesQty, cummulativeQuantity, avgPx);
                    executionReport.set(price);
                    executionReport.set(qty);
                    try {
                        Session.sendToTarget(executionReport, sessionID);
                        System.out.println("Execution report sent");
                    } catch (SessionNotFound e) {
                        System.out.println("Cannot communicate with client");
                    }
                }
                if (sym.equals("HP")) {
                    double currPrice = hp[0];
                    price.setValue(currPrice);
                    sellingQty = (int) Math.round(Math.random() * maxQty);
                    if (sellingQty >= qtyd) {
                        qty.setValue(qtyd);
                        purposeOfExecutionReport = new ExecType(ExecType.FILL);
                        orderStatus = new OrdStatus(OrdStatus.FILLED);
                        qtyd = 0;
                    }
                    else {
                        qty.setValue(sellingQty);
                        purposeOfExecutionReport = new ExecType(ExecType.PARTIAL_FILL);
                        orderStatus = new OrdStatus(OrdStatus.PARTIALLY_FILLED);
                        qtyd -= sellingQty;
                    }
                    executionReport = new ExecutionReport(orderNumber, execId, exectutionTransactioType,
                            purposeOfExecutionReport, orderStatus, symbol, side, leavesQty, cummulativeQuantity, avgPx);
                    executionReport.set(price);
                    executionReport.set(qty);
                    try {
                        Session.sendToTarget(executionReport, sessionID);
                        System.out.println("Execution report sent");
                    } catch (SessionNotFound e) {
                        System.out.println("Cannot communicate with client");
                    }
                }
            }
            changePrice();
        }
        System.out.println("Ordered has been filled");
    }



    public static void main(String[] args) throws InterruptedException {
        SocketAcceptor socketAcceptor = null;
        try {
            SessionSettings executorSettings = new SessionSettings(
                    "./src/StockExchange.cfg");
            StockExchange application = new StockExchange();
            FileStoreFactory fileStoreFactory = new FileStoreFactory(
                    executorSettings);
            MessageFactory messageFactory = new DefaultMessageFactory();
            FileLogFactory fileLogFactory = new FileLogFactory(executorSettings);
            socketAcceptor = new SocketAcceptor(application, fileStoreFactory,
                    executorSettings, fileLogFactory, messageFactory);
            socketAcceptor.start();
        } catch (ConfigError e) {
            e.printStackTrace();
        }
    }
}
