package johnsonmr;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class JDBCProject {
    public static final String ANSI_RESET = "\u001B[0m",
            ANSI_YELLOW = "\u001B[33m", ANSI_GREEN = "\u001B[32m";

    private static final String LOGIN = "root", PASSWORD = "PYSRF&1%Jk766bKkqnY&sv";

    private static final String DATABASE_DRIVER = "com.mysql.jdbc.Driver",
            DATABASE_URL = "jdbc:mysql://localhost", DATABASE_NAME = "lab5";

    /**
     * Verifies that the allocations that the user inputs are delimited by "," or ", "
     * and are of the format 1.0 or 0.X where X is any single digit
     */
    private static final String ALLOCATION_VALIDATION_REGEX = "(?:(?:0\\.\\d|1\\.0), ?){3}(?:0\\.\\d|1\\.0)";

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        printYellowText("Connecting as user '" + LOGIN + "'...");
        try {
            Class.forName(DATABASE_DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
        Connection connection = createConnection();
        String option = promptSimulationMode(in);

        Stock[] stockOrder = option.equals("1") ?
                promptFullUserInput(in):
                promptStockOrder(in);

        PortfolioSimulator simulator = new PortfolioSimulator(connection, stockOrder);
        BigDecimal[] results = option.equals("1") ?
                simulator.executeSingleRun():
                simulator.executeOptimization();
        printResults(results);
    }

    private static Connection createConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection((DATABASE_URL + "/" + DATABASE_NAME),
                    LOGIN, PASSWORD);
            connection.setClientInfo("autoReconnect", "true");
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
        System.out.println(ANSI_GREEN + "Connection complete!" + ANSI_RESET);
        return connection;
    }

    private static String promptSimulationMode(Scanner in) {
        printYellowText("\nSelect Single Execution (1) or Sharpe Ratio Optimization (2)");
        String option = in.nextLine();
        while (!option.matches("[12]")) {
            System.err.println("Invalid Input. Please choose 1 or 2");
            option = in.nextLine();
        }
        return option;
    }

    private static Stock[] promptFullUserInput(Scanner in) {
        Stock[] stockOrder = promptStockOrder(in);
        promptStockAllocations(stockOrder, in);
        return stockOrder;
    }

    private static Stock[] promptStockOrder(Scanner in) {
        printYellowText("Enter the order stocks for the following stocks: GOOG, CELG, NVDA, FB");
        String stockResponse = in.nextLine();

        while (!validateStockResponse(stockResponse)) {
            printYellowText("Example: GOOG,FB,CELG,NVDA");
            stockResponse = in.nextLine();
        }

        return createStockOrder(stockResponse);
    }

    private static boolean validateStockResponse(String stockResponse) {
        String[] splitResponse = stockResponse.split(",");
        if (splitResponse.length != Stock.values().length) {
            System.err.println("Invalid number of stocks entered");
            return false;
        }

        Set<Stock> stockSet = new HashSet<>();
        for (String response : splitResponse) {
            Stock stock = Stock.convertFromString(response);
            if (stock == null || stockSet.contains(stock)) {
                System.err.println("Invalid Stock");
                return false;
            }
            stockSet.add(stock);
        }

        return true;
    }

    private static Stock[] createStockOrder(String stockResponse) {
        String[] splitResponse = stockResponse.split(",");
        Stock[] stockOrder = new Stock[splitResponse.length];

        for (int i = 0; i < stockOrder.length; i++) {
            stockOrder[i] = Stock.convertFromString(splitResponse[i]);
        }

        return stockOrder;
    }

    private static void promptStockAllocations(Stock[] stockOrder, Scanner in) {
        printYellowText("Enter the decimal allocations for each stock");
        printYellowText("Must total to 1.0" + ANSI_RESET);

        String input = in.nextLine();

        while (!validateAllocation(input)) {
            printYellowText("Example Input: 0.1,0.2,0.3,0.4");
            input = in.nextLine();
        }

        setAllocation(stockOrder, input);
    }

    private static boolean validateAllocation(String input) {
        if (!input.matches(ALLOCATION_VALIDATION_REGEX)) {
            System.err.println("Invalid Input");
            return false;
        }

        double totalAllocation = 0.0;
        for (String number : input.split(",")) {
            double value = Double.parseDouble(number.trim());
            totalAllocation += value;
        }

        if (totalAllocation != 1.0) {
            System.err.format("Expected allocation: %.1f\nReceived allocation: %.1f\n\n", 1.0, totalAllocation);
            System.err.println("Please try again");
            return false;
        }

        return true;
    }

    private static void setAllocation(Stock[] stockOrder, String input) {
        String[] splitInput = input.split(",");
        for (int i = 0; i < stockOrder.length; i++) {
            stockOrder[i].setAllocation(Double.parseDouble(splitInput[i].trim()));
        }
    }

    private static void printResults(BigDecimal[] simulationResults) {
        printYellowText("The average of this simulation is " + simulationResults[0]);
        printYellowText("The standard deviation of this simulation is " + simulationResults[1]);
        printYellowText("The Sharpe Ratio of this simulation is " + simulationResults[2]);
        printYellowText("The overall cumulative returns of this simulation is " + simulationResults[3]);
    }

    private static void printYellowText(String input) {
        System.out.println(ANSI_YELLOW + input + ANSI_RESET);
    }
}
