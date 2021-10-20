package johnsonmr;

import java.math.BigDecimal;
import java.sql.Date;

public record PortfolioRecord(Date day, BigDecimal[] cumulativeReturns) {
    public BigDecimal calculateTotalDailyReturns(Stock[] stockOrder) {
        BigDecimal result = BigDecimal.ZERO;

        for (int stockIndex = 0; stockIndex < stockOrder.length; stockIndex++) {
            BigDecimal temp = calculateSingleStockReturns(stockIndex, stockOrder);
            result = result.add(temp);
        }

        return result;
    }

    public BigDecimal calculateSingleStockReturns(int index, Stock[] stockOrder) {
        BigDecimal temp = cumulativeReturns[index];
        return temp.multiply(BigDecimal.valueOf(stockOrder[index].getAllocation()));
    }
}
