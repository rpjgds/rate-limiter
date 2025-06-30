package rpjgds;

import java.time.ZonedDateTime;

public class Request {
    private ZonedDateTime requestTime;
    private String clientId;

    public Request(String clientId, ZonedDateTime requestTime) {
        this.setClientId(clientId);
        this.setRequestTime(requestTime);
    }

    public ZonedDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(ZonedDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
