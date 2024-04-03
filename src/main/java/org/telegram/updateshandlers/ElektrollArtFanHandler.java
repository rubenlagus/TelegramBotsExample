package org.telegram.updateshandlers;


import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;


/**
 * @author Clevero
 * @version 1.0
 * Handler for updates to ElektrollArtFanBot
 * This bot is an example for using inline buttons, here to make a gallery.
 * Bot contains some images from ElektrollArt that are all licensed under creative commons
 */
@Slf4j
public class ElektrollArtFanHandler implements LongPollingSingleThreadUpdateConsumer {

	private final ArrayList<String[]> urls;
	private final String BACK = "⬅️  Back";
	private final String NEXT = "Next ➡️";
	private final String INDEX_OUT_OF_RANGE = "Requested index is out of range!";

	private final TelegramClient telegramClient;

	public ElektrollArtFanHandler(String botToken) {
		telegramClient = new OkHttpTelegramClient(botToken);
		this.urls = new ArrayList<>();
		this.addUrls();
	}

	private void addUrls(){
		
		/*
		 * Just some sample links of my fav images from elektrollart.de
		 */
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=2964", "http://www.elektrollart.de/wp-content/uploads/deer-724x1024.png", "Deer Nature (cc-by)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=2960", "http://www.elektrollart.de/wp-content/uploads/butterfly_wallpaper_by_elektroll-d424m9d-1024x576.png", "Butterfly Wallpaper (cc-by)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=2897", "http://www.elektrollart.de/wp-content/uploads/ilovefs_wallpaper-1024x576.png", "I Love Free Software – Wallpaper (CC0)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=3953", "http://www.elektrollart.de/wp-content/uploads/diaspora_wallpaper_by_elektroll-d4anyj4-1024x576.png", "diaspora Wallpaper (CC-BY-SA)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=549", "http://www.elektrollart.de/wp-content/uploads/diaspora_flower-1024x576.png", "Diaspora Digital Wallpaper (CC-BY-SA)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=534", "http://www.elektrollart.de/wp-content/uploads/debian-butterfly-1024x576.png", "Debian-Butterfly Wallpaper (CC-BY-SA)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=531", "http://www.elektrollart.de/wp-content/uploads/cc-white-1920x1080-1024x576.png", "CC-Wallpaper (CC-BY-NC-SA)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=526", "http://www.elektrollart.de/wp-content/uploads/debian-gal-1920x1080-1024x576.png", "Debian Wallpaper (CC-BY-SA)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=523", "http://www.elektrollart.de/wp-content/uploads/Ubuntusplash-1920x1080-1024x576.png", "Ubuntu Wallpaper (CC-BY-NC-SA)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=559", "http://www.elektrollart.de/wp-content/uploads/skullgirll_a-1024x576.png", "Skullgirl Wallpapers (CC-BY-NC-SA)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=559", "http://www.elektrollart.de/wp-content/uploads/skullgirll_b-1024x576.png", "Skullgirl Wallpapers (CC-BY-NC-SA)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=847", "http://www.elektrollart.de/wp-content/uploads/archlinux_wallpaper-1024x576.png", "ArchLinux (CC0)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=1381", "http://www.elektrollart.de/wp-content/uploads/tuxxi-small-724x1024.png", "Piep (CC-BY)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=4264", "http://www.elektrollart.de/wp-content/uploads/Thngs_left_unsaid-724x1024.jpg", "Things Left Unsaid (CC-BY)"});
		this.urls.add(new String[]{"http://www.elektrollart.de/?p=2334", "http://www.elektrollart.de/wp-content/uploads/redpanda-1024x826.png", "<3 mozilla (CC0)"});
	}
	
	@Override
	public void consume(Update update) {

		//check if the update has a message
		if (update.hasMessage()) {
			Message message = update.getMessage();

			//check if the message contains a text
			if (message.hasText()) {
				String input = message.getText();

				if (input.equals("/start")) {
					/*
					 * we just add the first link from our array
					 *
					 * We use markdown to embedd the image
					 */
					SendMessage sendMessagerequest = new SendMessage(message.getChatId().toString(), "[​](" + this.urls.get(0)[1] + ")");
					sendMessagerequest.enableMarkdown(true);

					sendMessagerequest.setReplyMarkup(this.getGalleryView(0, -1));


					try {
						telegramClient.execute(sendMessagerequest);
					} catch (TelegramApiException e) {
						log.error("Error sending start message", e);
					}
				}
			}
		} else if (update.hasCallbackQuery()) {
			CallbackQuery callbackquery = update.getCallbackQuery();
			String[] data = callbackquery.getData().split(":");
			int index = Integer.parseInt(data[2]);

			if (data[0].equals("gallery")) {

				InlineKeyboardMarkup markup = null;

				if (data[1].equals("back")) {
					markup = this.getGalleryView(Integer.parseInt(data[2]), 1);
					if (index > 0) {
						index--;
					}
				} else if (data[1].equals("next")) {
					markup = this.getGalleryView(Integer.parseInt(data[2]), 2);
					if (index < this.urls.size() - 1) {
						index++;
					}
				} else if (data[1].equals("text")) {
					try {
						this.sendAnswerCallbackQuery("Please use one of the given actions below, instead.", false, callbackquery);
					} catch (TelegramApiException e) {
						log.error("Send text response", e);
					}
				}

				if (markup == null) {
					try {
						this.sendAnswerCallbackQuery(INDEX_OUT_OF_RANGE, false, callbackquery);
					} catch (TelegramApiException e) {
						log.error("Send index out of range response", e);
					}
				} else {

					EditMessageText editMarkup = new EditMessageText("[​](" + this.urls.get(index)[1] + ")");
					editMarkup.setChatId(callbackquery.getMessage().getChatId().toString());
					editMarkup.setInlineMessageId(callbackquery.getInlineMessageId());
					editMarkup.enableMarkdown(true);
					editMarkup.setMessageId(callbackquery.getMessage().getMessageId());
					editMarkup.setReplyMarkup(markup);
					try {
						telegramClient.execute(editMarkup);
					} catch (TelegramApiException e) {
						log.error("Error updating markup", e);
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param text The text that should be shown
	 * @param alert If the text should be shown as a alert or not
	 * @param callbackquery
	 * @throws TelegramApiException
	 */
	private void sendAnswerCallbackQuery(String text, boolean alert, CallbackQuery callbackquery) throws TelegramApiException{
		AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(callbackquery.getId());
		answerCallbackQuery.setShowAlert(alert);
		answerCallbackQuery.setText(text);
		telegramClient.execute(answerCallbackQuery);
	}
	
	/**
	 * 
	 * @param index Index of the current image
	 * @param action What button was clicked
	 * @return
	 */
	private InlineKeyboardMarkup getGalleryView(int index, int action){
		/*
		 * action = 1 -> back
		 * action = 2 -> next
		 * action = -1 -> nothing
		 */
		
		if(action == 1 && index > 0){
			index--;
		}
		else if((action == 1 && index == 0)){
			return null;
		}
		else if(action == 2 && index >= this.urls.size()-1){
			return null;
		}
		else if(action == 2){
			index++;
		}
		
		return InlineKeyboardMarkup
				.builder()
				.keyboardRow(new InlineKeyboardRow(
						InlineKeyboardButton.builder().text(this.urls.get(index)[2]).callbackData("gallery:text:" + index).build()
				))
				.keyboardRow(new InlineKeyboardRow(
						InlineKeyboardButton.builder().text(BACK).callbackData("gallery:back:" + index).build(),
						InlineKeyboardButton.builder().text(NEXT).callbackData("gallery:next:" + index).build()
				))
				.keyboardRow(new InlineKeyboardRow(
						InlineKeyboardButton.builder().text("Link").callbackData(this.urls.get(index)[0]).build()
				))
				.build();
	}
}
