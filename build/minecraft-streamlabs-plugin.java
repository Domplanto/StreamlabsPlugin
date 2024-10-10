package com.example.streamlabsplugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.pubsub.events.ChannelPointsRedemptionEvent;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class StreamlabsPlugin extends JavaPlugin {

    private FileConfiguration config;
    private FileConfiguration actionsConfig;
    private String streamlabsToken;
    private TwitchClient twitchClient;
    private Timer donationCheckTimer;
    private Map<String, Long> actionCooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadActionsConfig();

        streamlabsToken = config.getString("streamlabs.access_token");
        startDonationCheckTimer();
        setupTwitchClient();

        getLogger().info("StreamlabsPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (donationCheckTimer != null) {
            donationCheckTimer.cancel();
        }
        if (twitchClient != null) {
            twitchClient.close();
        }
        getLogger().info("StreamlabsPlugin has been disabled!");
    }

    private void loadActionsConfig() {
        File actionsFile = new File(getDataFolder(), "actions.yml");
        if (!actionsFile.exists()) {
            saveResource("actions.yml", false);
        }
        actionsConfig = YamlConfiguration.loadConfiguration(actionsFile);
    }

    private void setupTwitchClient() {
        OAuth2Credential credential = new OAuth2Credential("twitch", config.getString("twitch.oauth_token"));
        twitchClient = TwitchClientBuilder.builder()
                .withEnablePubSub(true)
                .withChatAccount(credential)
                .withEnableChat(true)
                .build();

        twitchClient.getPubSub().listenForChannelPointsRedemptionEvents(credential, config.getString("twitch.channel_id"));
        twitchClient.getEventManager().onEvent(ChannelPointsRedemptionEvent.class, this::onChannelPointsRedemption);

        String channelName = config.getString("twitch.channel_name");
        twitchClient.getChat().joinChannel(channelName);
    }

    private void startDonationCheckTimer() {
        donationCheckTimer = new Timer();
        donationCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForStreamlabsEvents();
            }
        }, 0, config.getLong("streamlabs.check_interval", 5000));
    }

    private void checkForStreamlabsEvents() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://streamlabs.com/api/v1.0/donations?access_token=" + streamlabsToken))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray donations = jsonResponse.getJSONArray("data");

            for (int i = 0; i < donations.length(); i++) {
                JSONObject donation = donations.getJSONObject(i);
                processStreamlabsEvent(donation);
            }
        } catch (Exception e) {
            getLogger().warning("Error checking for Streamlabs events: " + e.getMessage());
        }
    }

    private void processStreamlabsEvent(JSONObject event) {
        String type = event.getString("type");
        String platform = event.getString("platform");
        String username = event.getString("name");
        String message = event.optString("message", "");
        String amount = event.optString("amount", "");
        String formattedAmount = event.optString("formatted_amount", "");

        String eventKey = platform + "_" + type;
        executeActions(eventKey, platform, username, amount, formattedAmount, message);
    }

    private void onChannelPointsRedemption(ChannelPointsRedemptionEvent event) {
        String username = event.getRedemption().getUser().getDisplayName();
        String rewardTitle = event.getRedemption().getReward().getTitle();
        int pointsCost = event.getRedemption().getReward().getCost();

        String eventKey = "twitch_channel_points";
        executeActions(eventKey, "twitch", username, String.valueOf(pointsCost), rewardTitle, rewardTitle);
    }

    private void executeActions(String eventKey, String platform, String username, String amount, String formattedAmount, String message) {
        if (checkCooldown(eventKey)) {
            List<String> actions = actionsConfig.getStringList(eventKey);
            for (String action : actions) {
                String[] parts = action.split(":", 2);
                if (parts.length == 2) {
                    String actionType = parts[0].trim();
                    String actionValue = parts[1].trim();

                    switch (actionType) {
                        case "command":
                            executeCommand(actionValue, platform, username, amount, formattedAmount, message);
                            break;
                        case "broadcast":
                            broadcastMessage(actionValue, platform, username, amount, formattedAmount, message);
                            break;
                        default:
                            getLogger().warning("Unknown action type: " + actionType);
                    }
                }
            }
        }
    }

    private void executeCommand(String command, String platform, String username, String amount, String formattedAmount, String message) {
        String formattedCommand = command
            .replace("%platform%", platform)
            .replace("%username%", username)
            .replace("%amount%", amount)
            .replace("%formatted_amount%", formattedAmount)
            .replace("%message%", message);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
    }

    private void broadcastMessage(String messageFormat, String platform, String username, String amount, String formattedAmount, String message) {
        String broadcastMessage = messageFormat
            .replace("%platform%", platform)
            .replace("%username%", username)
            .replace("%amount%", amount)
            .replace("%formatted_amount%", formattedAmount)
            .replace("%message%", message);
        Bukkit.broadcastMessage(broadcastMessage);
    }

    private boolean checkCooldown(String action) {
        long now = System.currentTimeMillis();
        long cooldownTime = config.getLong("cooldowns." + action, 0);
        if (actionCooldowns.containsKey(action)) {
            if (now - actionCooldowns.get(action) < cooldownTime) {
                return false;
            }
        }
        actionCooldowns.put(action, now);
        return true;
    }
}
