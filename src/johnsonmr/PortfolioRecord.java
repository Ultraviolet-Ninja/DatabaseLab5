package johnsonmr;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioRecord(LocalDate day, BigDecimal[] cumulativeReturns) {
    public BigDecimal calculateDailyReturns(Stock[] stockOrder) {
        BigDecimal result = BigDecimal.ZERO;

        for (int stockIndex = 0; stockIndex < stockOrder.length; stockIndex++) {
            BigDecimal temp = cumulativeReturns[stockIndex];
            temp = temp.multiply(BigDecimal.valueOf(stockOrder[stockIndex].getAllocation()));
            result = result.add(temp);
        }

        return result;
    }
}
