# ChatBotSample
Simple java Symphony bot using the Symphony OSF Java Client for checking safe status of members of a room during a business continuity incident.

Set the src/main/resources/sample-config.yml to look like this

    sessionAuthURL: https://your-pod.symphony.com/sessionauth
    keyAuthUrl: https://your-km.symphony.com:443/keyauth
    localKeystorePath: complete path to your jks keystore
    localKeystorePassword: keystore password
    botCertPath: complete path to your bot's p12 file
    botCertPassword: password
    botEmailAddress: bot.user@example.com
    agentAPIEndpoint: https://your-agent.symphony.com/agent
    podAPIEndpoint: https://your-pod.symphony.com/pod
    mongoURL: URL to your mongo database

Run BotMainApp.java

Existing functionality:

- Bot listens for #safetycheck in rooms it is a member of
    - Initiates a roll call for and listens for members to mark themselves by sending #safe, sends 1-1 message to member who initiated the Safety Check
    - Accepts adding visitors who are not members of the room by sending #visitor @mention-user
    - Accepts marking other members or visitors safe by sending #safe @mention-user
- Member who initiated the Safety Check can request real-time reports of the safety check by sending #report on 1-1 to the bot
- Member who initiated the Safety Check can end safety check by sending #endsafetycheck to the room where it's taking place and the bot will send a report to the initiator 1-1
- If all members are marked safe, the safety check will end and a report will be sent to the initiator 1-1
