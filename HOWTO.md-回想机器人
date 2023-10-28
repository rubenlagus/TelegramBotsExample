Let us create a simple echo bot

- follow  [these](https://github.com/rubenlagus/TelegramBots/blob/master/eclipse%20configuration.md) steps to work with eclipse

- got to org.telegram and edit `BuildVars.java` and fill it out


```
public static final Boolean debug = true;
public static final String pathToLogs = "./";
public static final String linkDB = "jdbc:mysql://[IP_OF_YOU_MYSQL_SERVER]:3306/[DATABASE]?useUnicode=true&characterEncoding=UTF-8";
public static final String controllerDB = "com.mysql.jdbc.Driver";
public static final String userDB = "[YOUR_DB_USERNAME]";
public static final String password = "[YOUR_SECRET_DB_PASSWORD]";
```

For our project those settings are enough. 

[DATABASE]: your database. i.e. myProject


- next, go to org.telegram and edit `BotConfig.java`
Here we must fill in our login credentials for our bot.

```
public static final String TOKENMYPROJECT = "[YOUR_TOP_SECRET_TOKEN]";
public static final String USERNAMEMYPROJECT = "myProjectBot";
```
[YOUR_TOP_SECRET_TOKEN]: your token you got from the [BotFather](https://telegram.me/BotFather)


- go to org.telegram.updatehandlers and create a new class. This class is responsible for your bot actions. (in our case just return the text back). This class should extending `TelegramLongPollingBot`.


It should look similiar like this:




```
package org.telegram.updateshandlers;

import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

public class MyProjectHandler extends TelegramLongPollingBot {

        @Override
        public String getBotUsername() {
                // TODO Auto-generated method stub
                return null;
        }

        @Override
        public void onUpdateReceived(Update arg0) {
                // TODO Auto-generated method stub
                
        }

        @Override
        public String getBotToken() {
                // TODO Auto-generated method stub
                return null;
        }

        
        
}
```

Then you can program your bot. First edit getBotToken() and getBotUsername(). Simply return your credentials mentioned in BotConfig. So for example `return BotConfig.USERNAMEMYPROJECT;`



The onUpdateReceived() method could look like this:

```     
        @Override
        public void onUpdateReceived(Update update) {
                
                //check if the update has a message
                if(update.hasMessage()){
                        Message message = update.getMessage();
                        
                        //check if the message has text. it could also  contain for example a location ( message.hasLocation() )
                        if(message.hasText()){
                                
                                //create a object that contains the information to send back the message
                                SendMessage sendMessageRequest = new SendMessage();
                                sendMessageRequest.setChatId(message.getChatId().toString()); //who should get the message? the sender from which we got the message...
                                sendMessageRequest.setText("you said: " + message.getText());
                                try {
                                        sendMessage(sendMessageRequest); //at the end, so some magic and send the message ;)
                                } catch (TelegramApiException e) {
                                        //do some error handling
                                }//end catch()
                        }//end if()
                }//end  if()
                
        }//end onUpdateReceived()
```
- go to the `Main.java` in org.telegram and register your updatehandler
```
public static void main(String[] args) {

        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new MyProjectHandler());
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }//end catch()
    }//end main()
```
