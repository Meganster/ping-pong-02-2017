package sample.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;


@SuppressWarnings("NullableProblems")
public class Message<T> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @NotNull
    private String type;
    @NotNull
    private T data;
    @NotNull
    private long timestamp;
    @NotNull
    private int messageId;

    @NotNull
    public String getType() {
        return type;
    }

    @NotNull
    public T getData() {
        return data;
    }

    public Message(@NotNull @JsonProperty("type") String type, @NotNull @JsonProperty("data") T data) {
        this.type = type;
        this.data = data;
    }

    public int getId() {
        return messageId;
    }

    public void setId(int id) {
        messageId = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStingMessage() throws JsonProcessingException {
        final ObjectNode node = OBJECT_MAPPER.createObjectNode();
        node.put("type", type);
        node.put("timestamp", timestamp);
        node.put("id", messageId);
        node.set("data", OBJECT_MAPPER.valueToTree(data));
        return OBJECT_MAPPER.writeValueAsString(node);
    }
}

