package rpjgds;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LimitTest {

    @Test
    void check_workedExamples() {
        Map<Long, Long> client1 = new HashMap<Long,Long>() {
            {
                put(Long.valueOf("20250625142700"),100L);
                put(Long.valueOf("20250625142800"),1L);
                put(Long.valueOf("20250625142900"),94L);
            }
        };
        Map<Long, Long> client2 = new HashMap<Long,Long>() {
            {
                put(Long.valueOf("20250625142700"),50L);
                put(Long.valueOf("20250625142900"),20L);
            }
        };
        Map<Long, Long> client3 = new HashMap<Long,Long>() {
            {
                put(Long.valueOf("20250625143000"),4L);
            }
        };
        Map<String,Map> data = new HashMap<String,Map>()
        {
            {
                put("client_1", client1);
                put("client_2", client2);
                put("client_3", client3);
            }
        };

        Map<String, Long> limits = new HashMap<String,Long>()
        {
            {
                put("client_1", 100L);
                put("client_2", 50L);
                put("client_3", 5L);
            }
        };

        Limit limit = new Limit(data, limits);

        Map<Request, Boolean> tests = new HashMap<Request, Boolean>() {
            {
                //client 1
                put(new Request("client_1", Instant.parse("2025-06-25T14:27:30.00Z").atZone(ZoneId.of("UTC"))),false);
                put(new Request("client_1", Instant.parse("2025-06-25T14:28:30.00Z").atZone(ZoneId.of("UTC"))),true);
                put(new Request("client_1", Instant.parse("2025-06-25T14:29:30.00Z").atZone(ZoneId.of("UTC"))),true);
                put(new Request("client_1", Instant.parse("2025-06-25T14:30:30.00Z").atZone(ZoneId.of("UTC"))),true);
                //gradual allow
                put(new Request("client_1", Instant.parse("2025-06-25T14:28:00.00Z").atZone(ZoneId.of("UTC"))),false);
                put(new Request("client_1", Instant.parse("2025-06-25T14:28:01.00Z").atZone(ZoneId.of("UTC"))),true);

                //client 2
                put(new Request("client_2", Instant.parse("2025-06-25T14:27:30.00Z").atZone(ZoneId.of("UTC"))),false);
                put(new Request("client_2", Instant.parse("2025-06-25T14:28:30.00Z").atZone(ZoneId.of("UTC"))),true);
                put(new Request("client_2", Instant.parse("2025-06-25T14:29:30.00Z").atZone(ZoneId.of("UTC"))),true);
                put(new Request("client_2", Instant.parse("2025-06-25T14:30:30.00Z").atZone(ZoneId.of("UTC"))),true);
                //gradual allow
                put(new Request("client_2", Instant.parse("2025-06-25T14:28:00.00Z").atZone(ZoneId.of("UTC"))),false);
                put(new Request("client_2", Instant.parse("2025-06-25T14:28:01.00Z").atZone(ZoneId.of("UTC"))),true);

                //client 3
                
            }
        };
        for (Map.Entry<Request, Boolean> entry : tests.entrySet()) {
            assertEquals(entry.getValue(), limit.check(entry.getKey()));
        }
    }

    @Test
    void check_cumulative() {
        Map<Long, Long> client = new HashMap<Long,Long>() {
            {
                put(Long.valueOf("20250625142700"),100L);
            }
        };
        Map<String,Map> data = new HashMap<String,Map>()
        {
            {
                put("client", client);
            }
        };

        
        Map<String,Long> limits = new HashMap<String,Long>()
        {
            {
                put("client", 100L);
            }
        };
        //Maxed out the previous period
        Limit limit = new Limit(data, limits);
        assertEquals(false, limit.checkAndRecord(new Request("client", Instant.parse("2025-06-25T14:28:00.00Z").atZone(ZoneId.of("UTC")))));
        // rejected requests don't affect count
        assertEquals(100L, limit.data.get("client").get(Long.valueOf("20250625142700")));
        //next second, request is allowed
        assertEquals(true, limit.checkAndRecord(new Request("client", Instant.parse("2025-06-25T14:28:01.00Z").atZone(ZoneId.of("UTC")))));
        // accepted requests increment count
        assertEquals(1L, limit.data.get("client").get(Long.valueOf("20250625142800")));
        // another request in the same second also succeeds
        assertEquals(true, limit.checkAndRecord(new Request("client", Instant.parse("2025-06-25T14:28:01.00Z").atZone(ZoneId.of("UTC")))));
        // count incremented
        assertEquals(2L, limit.data.get("client").get(Long.valueOf("20250625142800")));
        // a third request in the same second now fails
        assertEquals(false, limit.checkAndRecord(new Request("client", Instant.parse("2025-06-25T14:28:01.00Z").atZone(ZoneId.of("UTC")))));
        // count unaffected
        assertEquals(2L, limit.data.get("client").get(Long.valueOf("20250625142800")));

    }
}
