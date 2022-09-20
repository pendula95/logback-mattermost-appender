/*
 * Copyright (c) 2022. Lazar Bulic lazarbulic@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pendula95.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MattermostAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Layout<ILoggingEvent> defaultLayout = new LayoutBase<>() {
        public String doLayout(ILoggingEvent event) {
            return "-- [" + event.getLevel() + "]" +
                    event.getLoggerName() + " - " +
                    event.getFormattedMessage().replaceAll("\n", "\n\t");
        }
    };
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String webhook;
    private String channel;
    private String username;
    private String iconUrl;
    private String iconEmoji;

    private String serverAddress;
    private String token;
    private String channelId;
    private Boolean colorCoding = false;

    private URI webhookUri;
    private URI serverAddressUri;

    private Layout<ILoggingEvent> layout = defaultLayout;

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        try {
            if (webhookUri != null) {
                sendMessageWithWebhookUri(iLoggingEvent);
            } else {
                sendMessageWithToken(iLoggingEvent);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            addError("Error while sending message to Mattermost", e);
        }
    }

    @Override
    public void start() {
        if ((serverAddress == null || serverAddress.isEmpty()) && (webhook == null || webhook.isEmpty())) {
            addError("Server address or webhook uri must be set");
            return;
        }

        if (webhook != null && !webhook.isEmpty()) {
            try {
                webhookUri = URI.create(webhook);
            } catch (IllegalArgumentException e) {
                addError("Invalid webhook", e);
                return;
            }
        } else {
            try {
                serverAddressUri = URI.create(serverAddress).resolve("/api/v4/posts");
            } catch (IllegalArgumentException e) {
                addError("Invalid serverAddress", e);
                return;
            }

            if (token == null || token.isEmpty()) {
                addError("Token must be set");
                return;
            } else if (channelId == null || channelId.isEmpty()) {
                addError("ChannelId must be set");
                return;
            }
        }
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    private void sendMessageWithWebhookUri(final ILoggingEvent evt) throws IOException, InterruptedException {
        String[] parts = layout.doLayout(evt).split("\n", 2);

        Map<String, Object> message = new HashMap<>();
        message.put("text", parts[0]);

        if (channel != null && !channel.isEmpty()) {
            message.put("channel", channel);
        }
        if (username != null && !username.isEmpty()) {
            message.put("username", username);
        }
        if (iconUrl != null && !iconUrl.isEmpty()) {
            message.put("icon_url", iconUrl);
        }
        if (iconEmoji != null && !iconEmoji.isEmpty()) {
            message.put("icon_emoji", iconEmoji);
        }

        if (parts.length > 1 && parts[1].length() > 0) {
            message.put("attachments", List.of(generateAttachment(evt, parts[1])));
        }

        HttpResponse<String> response = doPost(webhookUri, objectMapper.writeValueAsString(message), null);

        if (response.statusCode() != 200) {
            addError(String.format("Error while sending message to Mattermost, received response status code %d", response.statusCode()));
        }
    }

    private void sendMessageWithToken(final ILoggingEvent evt) throws IOException, InterruptedException {
        String[] parts = layout.doLayout(evt).split("\n", 2);

        Map<String, Object> message = new HashMap<>();
        message.put("channel_id", channelId);
        message.put("message", parts[0]);

        if (parts.length > 1 && parts[1].length() > 0) {
            Map<String, List<ObjectNode>> props = new HashMap<>();
            props.put("attachments", List.of(generateAttachment(evt, parts[1])));
            message.put("props", props);
        }

        HttpResponse<String> response = doPost(serverAddressUri, objectMapper.writeValueAsString(message), token);

        if (response.statusCode() != 201) {
            addError(String.format("Error while sending message to Mattermost, received response status code %d %s", response.statusCode(), evt));
        }
    }

    private ObjectNode generateAttachment(final ILoggingEvent evt, String text) {
        ObjectNode attachment = objectMapper.createObjectNode();
        attachment.put("title", evt.getLevel().toString());
        attachment.put("text", text);
        if (colorCoding && colorByEvent(evt) != null) {
            attachment.put("color", colorByEvent(evt));
        }
        return attachment;
    }

    private HttpResponse<String> doPost(final URI uri, final String body, final String token) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");

        if (token != null && !token.isEmpty()) {
            requestBuilder = requestBuilder
                    .header("Authorization", "Bearer " + token);
        }

        return httpClient
                .send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String colorByEvent(ILoggingEvent evt) {
        if (Level.ERROR.equals(evt.getLevel())) {
            return "danger";
        } else if (Level.WARN.equals(evt.getLevel())) {
            return "warning";
        } else if (Level.INFO.equals(evt.getLevel())) {
            return "good";
        }

        return null;
    }

    public String getWebhook() {
        return webhook;
    }

    public void setWebhook(String webhook) {
        this.webhook = webhook;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getIconEmoji() {
        return iconEmoji;
    }

    public void setIconEmoji(String iconEmoji) {
        this.iconEmoji = iconEmoji;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public Boolean getColorCoding() {
        return colorCoding;
    }

    public void setColorCoding(Boolean colorCoding) {
        this.colorCoding = colorCoding;
    }

    public Layout<ILoggingEvent> getLayout() {
        return layout;
    }

    public void setLayout(final Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }
}
