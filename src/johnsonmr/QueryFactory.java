package johnsonmr;

public class QueryFactory {
    public static final String TABLE_NAME = "Analysis",
            CUMULATIVE_RETURN_COLUMN_NAME = "_Cumulative_Return", VALUE_COLUMN_NAME = "_Value";

    public static String buildInsertQuery(Stock[] stockOrder) {
        final String delimiter = "=?,";
        StringBuilder query = new StringBuilder("UPDATE ").append(TABLE_NAME).append(" SET ");
        for (Stock stock : stockOrder) {
            query.append(stock.name()).append(VALUE_COLUMN_NAME).append(delimiter);
        }
        query.append("SPY").append(VALUE_COLUMN_NAME).append(delimiter)
                .append("Portfolio").append(CUMULATIVE_RETURN_COLUMN_NAME).append(delimiter)
                .append("Portfolio").append(VALUE_COLUMN_NAME).append(delimiter)
                .append("Ra_Rb").append("=?");

        query.append("WHERE Date=?");

        return query.toString();
    }

    public static String buildSelectSPYRecords(String columnName) {
        return "SELECT date, " + columnName + " FROM " + TABLE_NAME;
    }

    public static String buildSelectStockRecords(Stock[] stockOrder) {
        StringBuilder query = new StringBuilder("SELECT date, ");

        for (int i = 0; i < stockOrder.length; i++) {
            query.append(stockOrder[i].name()).append(CUMULATIVE_RETURN_COLUMN_NAME);
            if (i != stockOrder.length - 1)
                query.append(", ");
        }
        query.append(" FROM ").append(TABLE_NAME);
        return query.toString();
    }
}
