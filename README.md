# StreamlabsPlugin
A Minecraft plugin that allows you to create interactive Streams in Minecraft


1. Copy the updated config.yml and actions.yml content into their respective files in the plugin's folder (usually plugins/StreamlabsPlugin/).

Edit the config.yml file:

2. Set your Streamlabs access token in the streamlabs.access_token field.
3. Set your Twitch OAuth token, channel name, and channel ID in the twitch section.
4. Adjust the check_interval if needed.
   Set appropriate cooldowns for each event type, including the new twitch_channel_points event.


Edit the actions.yml file:

5. Customize the actions for each event type.
   You can now add multiple actions per event, using either command: or broadcast: for each action.

6. Restart your Minecraft server or reload the plugins.

This updated version includes the following enhancements:

Support for Twitch channel points redemptions.
Multiple actions (commands and broadcasts) can be defined for each event type.
All aspects are now editable in the YAML files, including the new Twitch channel points event.
The plugin now processes both Streamlabs events and Twitch channel point redemptions.
Placeholders (%username%, %amount%, %formatted_amount%, %message%, %platform%) are available for all action types.

The plugin is now more flexible and customizable, allowing you to define multiple actions for each event type and supporting Twitch channel points, all configurable through the YAML files.
