# ChatBotSample
Simple java Symphony bot

Set the src/main/resources/sample-config.yml to look like this

sessionAuthURL: https://your-pod.symphony.com/sessionauth
keyAuthUrl: https://your-km.symphony.com:443/keyauth
localKeystorePath: complete path to your keystore path
localKeystorePassword: keystore password
botCertPath: complete path to your bot's p12 file
botCertPassword: password
botEmailAddress: bot.user@example.com
agentAPIEndpoint: https://your-agent.symphony.com/agent
podAPIEndpoint: https://your-pod.symphony.com/pod

Run BotMainApp.java
