package johnsonmr;

public enum Stock {
    GOOG, CELG, NVDA, FB;

    public double getAllocation() {
        return allocation;
    }

    public void setAllocation(double allocation) {
        this.allocation = allocation;
    }

    private double allocation;

    Stock() {
        allocation = 0.0;
    }

    public static Stock convertFromString(String stockString) {
        for (Stock stock : Stock.values()) {
            if (stock.name().equals(stockString.trim().toUpperCase()))
                return stock;
        }
        return null;
    }
}
