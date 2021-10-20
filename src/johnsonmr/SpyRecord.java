package johnsonmr;

import java.math.BigDecimal;
import java.sql.Date;

public record SpyRecord(Date day, BigDecimal spyCumulativeReturn) {
}
