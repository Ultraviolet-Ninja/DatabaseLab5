package johnsonmr;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static johnsonmr.QueryFactory.CUMULATIVE_RETURN_COLUMN_NAME;

public class PortfolioSimulator {
    private static final int MAX_PRECISION = 3;

    private final Stock[] stockOrder;
    private final Connection connection;
    private final List<PortfolioRecord> portfolioRecords;
    private final List<SpyRecord> spyRecords;

    public PortfolioSimulator(Connection connection, Stock[] stockOrder) {
        this.stockOrder = stockOrder;
        this.connection = connection;
        portfolioRecords = new ArrayList<>();
        spyRecords = new ArrayList<>();
        selectAllSPYRecords();
        selectAllStockRecords();
    }

    private void selectAllStockRecords() {
        String query = QueryFactory.buildSelectStockRecords(stockOrder);

        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(query)) {

            while (results.next()) {
                BigDecimal[] stockCumulativeReturns = new BigDecimal[stockOrder.length];
                for (int i = 0; i < stockOrder.length; i++) {
                    String columnName = stockOrder[i].name() + CUMULATIVE_RETURN_COLUMN_NAME;
                    stockCumulativeReturns[i] = new BigDecimal(results.getString(columnName));
                }

                PortfolioRecord temp = new PortfolioRecord(
                        Date.valueOf(results.getString("date")),
                        stockCumulativeReturns
                );
                portfolioRecords.add(temp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void selectAllSPYRecords() {
        String columnName = "SPY" + CUMULATIVE_RETURN_COLUMN_NAME;

        String query = QueryFactory.buildSelectSPYRecords(columnName);
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(query)) {

            while (results.next()) {
                SpyRecord temp = new SpyRecord(
                        Date.valueOf(results.getString("date")),
                        new BigDecimal(results.getString(columnName))
                );
                spyRecords.add(temp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public BigDecimal[] executeOptimization() {
        List<double[]> possibleAllocations = createPossibleAllocations();

        BigDecimal maxSharpeRatio = BigDecimal.ZERO;
        double[] maxAllocations = new double[0];
        for (double[] currentAllocations : possibleAllocations) {
            setAllocations(currentAllocations);

            BigDecimal[] portfolioDailyValues = createPortfolioDailyValues();
            BigDecimal[] portfolioBenchmarkDifferences = calculateDifferenceValues(portfolioDailyValues);

            BigDecimal average = calculateAverage(portfolioBenchmarkDifferences);
            BigDecimal standardDeviation = calculateStandardDeviation(portfolioBenchmarkDifferences);
            BigDecimal currentSharpeRatio = calculateSharpeRatio(
                    portfolioBenchmarkDifferences.length,
                    average,
                    standardDeviation
            );

            if (maxSharpeRatio.compareTo(currentSharpeRatio) < 0) {
                maxSharpeRatio = currentSharpeRatio;
                maxAllocations = currentAllocations;
            }
        }
        setAllocations(maxAllocations);
        System.out.println(JDBCProject.ANSI_YELLOW + "The best allocation for this stock order was " +
                Arrays.toString(maxAllocations) + JDBCProject.ANSI_RESET + "\n");
        return executeSingleRun();
    }

    private List<double[]> createPossibleAllocations() {
        final double[] increments = new double[]{0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
        List<double[]> possibleAllocations = new ArrayList<>();

        for (double firstValue : increments) {
            for (double secondValue : increments) {
                for (double thirdValue : increments) {
                    for (double fourthValue : increments) {
                        double sum = firstValue + secondValue + thirdValue + fourthValue;

                        if (sum == 1.0) {
                            possibleAllocations.add(new double[]{firstValue, secondValue, thirdValue, fourthValue});
                        }
                    }
                }
            }
        }

        return possibleAllocations;
    }

    private void setAllocations(double[] newAllocations) {
        for (int i = 0; i < newAllocations.length; i++) {
            stockOrder[i].setAllocation(newAllocations[i]);
        }
    }

    public BigDecimal[] executeSingleRun() {
        BigDecimal[] portfolioDailyValues = createPortfolioDailyValues();
        BigDecimal[] portfolioBenchmarkDifferences = calculateDifferenceValues(portfolioDailyValues);

        updateDatabaseValues(portfolioDailyValues, portfolioBenchmarkDifferences);

        BigDecimal average = calculateAverage(portfolioBenchmarkDifferences);
        BigDecimal standardDeviation = calculateStandardDeviation(portfolioBenchmarkDifferences);
        BigDecimal sharpeRatio = calculateSharpeRatio(
                portfolioBenchmarkDifferences.length,
                average,
                standardDeviation
        );
        BigDecimal overallCumulativeReturn = calculateOverallCumulativeReturns(portfolioDailyValues);

        return new BigDecimal[]{average, standardDeviation, sharpeRatio, overallCumulativeReturn};
    }

    private void updateDatabaseValues(BigDecimal[] portfolioDailyValues, BigDecimal[] portfolioBenchmarkDifferences) {
        final String query = QueryFactory.buildInsertQuery(stockOrder);

        try {
            for (int i = 0; i < portfolioRecords.size(); i++) {
                updateSingleRecord(
                        query,
                        portfolioRecords.get(i),
                        spyRecords.get(i).spyCumulativeReturn(),
                        portfolioDailyValues[i],
                        portfolioBenchmarkDifferences[i]
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void updateSingleRecord(String query, PortfolioRecord portfolioRecord, BigDecimal spyRecordCumulativeReturn,
                                    BigDecimal totalPortfolioValue, BigDecimal portfolioBenchmarkDifference) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        for (int i = 0; i < stockOrder.length; i++) {
            BigDecimal temp = portfolioRecord.calculateSingleStockReturns(i, stockOrder);
            preparedStatement.setString(i + 1, "$" + temp.round(new MathContext(2)));
        }
        preparedStatement.setString(5, "$" + spyRecordCumulativeReturn);
        preparedStatement.setBigDecimal(6, totalPortfolioValue);
        preparedStatement.setString(7, "$" + totalPortfolioValue);
        preparedStatement.setBigDecimal(8, portfolioBenchmarkDifference);
        preparedStatement.setDate(9, portfolioRecord.day());
        preparedStatement.executeUpdate();
    }

    private BigDecimal[] createPortfolioDailyValues() {
        BigDecimal[] dailyValues = new BigDecimal[portfolioRecords.size()];

        for (int i = 0; i < portfolioRecords.size(); i++) {
            BigDecimal result = portfolioRecords.get(i).calculateTotalDailyReturns(stockOrder);
            int precision = result.compareTo(BigDecimal.ONE) > 0 ? 3 : 2;
            dailyValues[i] = result.round(new MathContext(precision));
        }

        return dailyValues;
    }

    private BigDecimal[] calculateDifferenceValues(BigDecimal[] portfolioDailyValues) {
        BigDecimal[] differences = new BigDecimal[spyRecords.size()];

        for (int i = 0; i < spyRecords.size(); i++) {
            differences[i] = portfolioDailyValues[i].subtract(
                    spyRecords.get(i).spyCumulativeReturn()
            );
        }

        return differences;
    }

    private BigDecimal calculateSharpeRatio(int numberOfDays, BigDecimal average, BigDecimal standardDeviation) {
        BigDecimal squareRootDays = BigDecimal.valueOf(numberOfDays)
                .sqrt(new MathContext(MAX_PRECISION));

        return squareRootDays
                .multiply(average)
                .divide(standardDeviation, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverage(BigDecimal[] portfolioBenchmarkDifferences) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal difference : portfolioBenchmarkDifferences) {
            sum = sum.add(difference);
        }
        return sum.divide(BigDecimal.valueOf(portfolioBenchmarkDifferences.length), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStandardDeviation(BigDecimal[] portfolioBenchmarkDifferences) {
        BigDecimal sum = BigDecimal.ZERO, standardDeviation = BigDecimal.ZERO;
        int length = portfolioBenchmarkDifferences.length;

        for (BigDecimal difference : portfolioBenchmarkDifferences) {
            sum = sum.add(difference);
        }

        BigDecimal mean = sum.divide(BigDecimal.valueOf(length), RoundingMode.HALF_UP);

        for (BigDecimal difference : portfolioBenchmarkDifferences) {
            BigDecimal temp = difference
                    .subtract(mean)
                    .pow(2);
            standardDeviation = standardDeviation.add(temp);
        }
        return standardDeviation
                .divide(BigDecimal.valueOf(length), RoundingMode.HALF_UP)
                .sqrt(new MathContext(MAX_PRECISION));
    }

    private BigDecimal calculateOverallCumulativeReturns(BigDecimal[] portfolioBenchmarkDifferences) {
        int last = portfolioBenchmarkDifferences.length - 1;
        return portfolioBenchmarkDifferences[last]
                .subtract(portfolioBenchmarkDifferences[0])
                .divide(portfolioBenchmarkDifferences[0], RoundingMode.HALF_UP);
    }
}
