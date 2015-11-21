package trading;

import java.util.DoubleSummaryStatistics;

/** Object that contains the information of a trade
 * @author Daniel Chaparro Altamirano
 */
public class TradeInfo {
    private String symbol;
    private String contractExpiry;
    private double lots;
    private double price;
    private String trader;
    private boolean buy;
    private String date;
    private String time;
    private String type;
    private double offset;

    /** Default constructor creates an empty TradeInfo object
     *
     */
    public TradeInfo() {
        symbol = "";
        contractExpiry = "";
        lots = 0;
        price = 0;
        trader = "";
        buy = true;
        date = "";
        time = "";
        type = "";
        offset = 0;
    }

    /** Sets the symbol of a trade
     *
     * @param symbol        Symbol of a trade, e.g. HH
     */
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    /** Sets the contract expiry date of a trade
     *
     * @param contractExpiry        Expiry date of a trade, e.g. JUL 2019
     */
    public void setContractExpiry(String contractExpiry) {
        this.contractExpiry = contractExpiry;
    }

    /** Sets the lots of a trade
     *
     * @param lots          Lots of a trade (numeric value)
     */
    public void setLots(double lots) {
        this.lots = lots;
    }

    /** Sets the price of a trade
     *
     * @param price         Price of a trade (numeric value)
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /** Sets the name of the trader that executed the trade
     *
     * @param trader        Name of the trader, e.g. John Doe
     */
    public void setTrader(String trader) {
        this.trader = trader;
    }

    /** Sets the buy/sell flag of a trade
     *
     * @param buy           Flag od a trade. true = buy, false = sell
     */
    public void setBuy(boolean buy) {
        this.buy = buy;
    }

    /** Sets the date of a trade
     *
     * @param date          Date a trade was executed
     */
    public void setDate(String date) {
        this.date = date;
    }

    /** Sets the time of a trade
     *
     * @param time          Time the trade was executed
     */
    public void setTime(String time) {
        this.time = time;
    }

    /** Sets the type of a trade
     *
     * @param type          Type of the trade
     */
    public void setType(String type) {
        this.type = type;
    }

    /** Sets the offset of a pegged order
     *
     * @param offset          Offset of the order
     */
    public void setOffset(double offset) {
        this.offset = offset;
    }

    /** Gets the symbol of a trade
     *
     * @return      symbol of a trade
     */
    public String getSymbol() {
        return symbol;
    }

    /** Gets the expiry date of a trade
     *
     * @return      expiry date of a trade
     */
    public String getContractExpiry() {
        return contractExpiry;
    }

    /** Gets the lots of a trade
     *
     * @return      lots of a trade
     */
    public double getLots() {
        return lots;
    }

    /** Gets the price of a trade
     *
     * @return      price of a trade
     */
    public double getPrice() {
        return price;
    }

    /** Gets the trader that executed a trade
     *
     * @return      Name of the trader that executed the trade
     */
    public String getTrader() {
        return trader;
    }

    /** Gets the buy/sell flag of a trade
     *
     * @return      Flag indicating buy or sell. True = buy, false = sell
     */
    public boolean getBuy() {
        return buy;
    }

    /** Gets the date of a trade
     *
     * @return      Date of the trade
     */
    public String getDate() {
        return date;
    }

    /** Gets the time of a trade
     *
     * @return      Time of the trade
     */
    public String getTime() {
        return time;
    }

    /** Gets the type of a trade
     *
     * @return      Type of the trade
     */
    public String getType() {
        return type;
    }

    /** Gets the offset of an order
     *
     * @return      Offset of the order
     */
    public double getOffset() {
        return offset;
    }
}
