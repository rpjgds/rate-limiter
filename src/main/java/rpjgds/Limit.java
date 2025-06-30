package rpjgds;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Map;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Limit {
    public Map<String,Map> data;
    public Map<String,Long> limits;
    private static final Logger LOG = LogManager.getLogger(Limit.class);

    public Limit(Map<String,Map> data, Map<String,Long> limits) {
        this.data = data;
        this.limits = limits;
    }

    private Long countForClientPeriod(String client, Long period) {
        Map<String,Long> clientPeriods = this.data.get(client);
        if (clientPeriods == null) {
            return 0L;
        }
        Long count = clientPeriods.get(period);
        if (count == null) {
            return 0L;
        }
        return count;
    }

    private Long renderPeriod(ZonedDateTime i) {
        StringBuffer sb = new StringBuffer();
        sb
            .append(String.format("%4s", i.get(ChronoField.YEAR_OF_ERA)))
            .append(String.format("%2s", i.get(ChronoField.MONTH_OF_YEAR)))
            .append(String.format("%2s", i.get(ChronoField.DAY_OF_MONTH)))
            .append(String.format("%2s", i.get(ChronoField.HOUR_OF_DAY)))
            .append(String.format("%2s", i.get(ChronoField.MINUTE_OF_HOUR)))
            .append(String.format("%2s", i.get(ChronoField.SECOND_OF_MINUTE)));
        return Long.valueOf(sb.toString().replace(' ', '0'));
    }

    private Long periodForZonedDateTime(ZonedDateTime zdt) {
        Long timestamp = renderPeriod(zdt);
        LOG.debug("timestamp: "+timestamp);
        Long period = renderPeriod(zdt.truncatedTo(ChronoUnit.MINUTES));
        LOG.debug("period: "+period);
        return period;
    }

    public Boolean checkAndRecord(Request request) {
        Boolean result = check(request);
        //only record on accepted requests
        if (!result) {
            return result;
        }
        Long periodForRequest = periodForZonedDateTime(request.getRequestTime());
        Map<Long,Long> periodsForClient = this.data.get(request.getClientId());
        Long countForPeriod = countForClientPeriod(request.getClientId(), periodForRequest);

        periodsForClient.put(periodForRequest, ++countForPeriod);
        this.data.put(request.getClientId(), periodsForClient);

        return result;
    }

    public Boolean check(Request request) {
        Long periodLength = 60L;

        Long limit = this.limits.get(request.getClientId());
        if (limit == null) {
            // no limit
            return true;
        }
        ZonedDateTime currentTimestampZonedDateTime = request.getRequestTime();
        LOG.debug("currentTimestampZonedDateTime: {}", currentTimestampZonedDateTime);
        Long currentPeriod = periodForZonedDateTime(currentTimestampZonedDateTime);
        LOG.debug("currentPeriod: {}", currentPeriod);

        ZonedDateTime previousTimestampZonedDateTime = currentTimestampZonedDateTime.minus(Duration.of(1,ChronoUnit.MINUTES));
        LOG.debug("previousTimestampZonedDateTime: {}", previousTimestampZonedDateTime);
        Long previousTimestamp = renderPeriod(previousTimestampZonedDateTime);
        LOG.debug("previousTimestamp: {}", previousTimestamp);
        Long previousPeriod = renderPeriod(previousTimestampZonedDateTime.truncatedTo(ChronoUnit.MINUTES));
        LOG.debug("previousPeriod: {}", previousPeriod);

        Long currentCount = countForClientPeriod(request.getClientId(), currentPeriod);
        LOG.debug("currentCount: {}", currentCount);
        Long previousCount = countForClientPeriod(request.getClientId(), previousPeriod);
        LOG.debug("previousCount: {}", previousCount);

        Double partialPreviousPeriod = Double.valueOf(periodLength - (previousTimestamp - previousPeriod));
        LOG.debug("partialPreviousPeriod: {}", partialPreviousPeriod);
        Double partialPreviousCount = previousCount * (partialPreviousPeriod / periodLength);
        LOG.debug("partialPreviousCount: {}", partialPreviousCount);

        LOG.debug("partialPreviousCount + currentCount >= limit: {}", (partialPreviousCount + currentCount >= limit));
        if (partialPreviousCount + currentCount >= limit) {
            return false;
        }
        return true;
    }
}