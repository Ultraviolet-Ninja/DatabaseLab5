package johnsonmr;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SpyRecord(LocalDate day, BigDecimal spyCumulativeReturn) {
}
