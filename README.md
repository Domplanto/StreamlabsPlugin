# StreamlabsPlugin
A Minecraft plugin that allows you to create interactive Streams in Minecraft


1. Copy the updated config.yml and actions.yml content into their respective files in the plugin's folder (usually plugins/StreamlabsPlugin/).

# Edit the config.yml file:

2. Set your Streamlabs access token in the streamlabs.access_token field.
3. Set your Twitch OAuth token, channel name, and channel ID in the twitch section.
4. Adjust the check_interval if needed.
   Set appropriate cooldowns for each event type, including the new twitch_channel_points event.


# Edit the actions.yml file:

5. Customize the actions for each event type.
   You can now add multiple actions per event, using either command: or broadcast: for each action.

6. Restart your Minecraft server or reload the plugins.

PS: It is made by AI. So I have no idea if it works!
