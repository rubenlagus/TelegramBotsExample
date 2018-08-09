package org.telegram.updateshandlers;


import org.telegram.BotConfig;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Clevero
 * @version 1.0
 * @brief Handler for updates to ElektrollArtFanBot
 * This bot is an example for using inline buttons, here to make a gallery.
 * Bot contains some images from ElektrollArt that are all licensed under creative commons
 * @date 28 of October of 2016
 */
public class ElektrollArtFanHandler extends TelegramLongPollingBot {

	private ArrayList<String[]> urls;
	final private String BACK = "⬅️  Back";
	final private String NEXT = "Next ➡️";
	final private String INDEX_OUT_OF_RANGE = "Requested index is out of range!";
	
	public ElektrollArtFanHandler() {
		this.urls = new ArrayList<>();
		this.addUrls();
	}
	
	@Override
	public String getBotUsername() {
		return BotConfig.ELEKTROLLART_USER;
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
	public void onUpdateReceived(Update update) {
		
		//check if the update has a message
		if(update.hasMessage()){
			Message message = update.getMessage();
			
			//check if the message contains a text
			if(message.hasText()){
				String input = message.getText();
				
				if(input.equals("/start")){
					SendMessage sendMessagerequest = new SendMessage();
					sendMessagerequest.setChatId(message.getChatId().toString());
					/*
					 * we just add the first link from our array
					 * 
					 * We use markdown to embedd the image
					 */
					sendMessagerequest.setText("[​](" + this.urls.get(0)[1] + ")");
					sendMessagerequest.enableMarkdown(true);
					
					sendMessagerequest.setReplyMarkup(this.getGalleryView(0, -1));
					
					
					try {
						execute(sendMessagerequest);
					} catch (TelegramApiException e) {
						e.printStackTrace();
					}
				}
			}
		}
		else if(update.hasCallbackQuery()){
			CallbackQuery callbackquery = update.getCallbackQuery();
			String[] data = callbackquery.getData().split(":");
			int index = Integer.parseInt(data[2]);
			
			if(data[0].equals("gallery")){
				
				InlineKeyboardMarkup markup = null;
				
				if(data[1].equals("back")){
					markup = this.getGalleryView(Integer.parseInt(data[2]), 1);
					if(index > 0){
						index--;
					}
				}else if(data[1].equals("next")){
					markup = this.getGalleryView(Integer.parseInt(data[2]), 2);
					if(index < this.urls.size()-1){
						index++;
					}
				}else if(data[1].equals("text")){
					try {
						this.sendAnswerCallbackQuery("Please use one of the given actions below, instead.", false, callbackquery);
					} catch (TelegramApiException e) {
						e.printStackTrace();
					}
				}
				
				if(markup == null){
					try {
						this.sendAnswerCallbackQuery(INDEX_OUT_OF_RANGE, false, callbackquery);
					} catch (TelegramApiException e) {
						e.printStackTrace();
					}
				}else{
					
					EditMessageText editMarkup = new EditMessageText();
					editMarkup.setChatId(callbackquery.getMessage().getChatId().toString());
					editMarkup.setInlineMessageId(callbackquery.getInlineMessageId());
					editMarkup.setText("[​](" + this.urls.get(index)[1] + ")");
					editMarkup.enableMarkdown(true);
					editMarkup.setMessageId(callbackquery.getMessage().getMessageId());
					editMarkup.setReplyMarkup(markup);
					try {
						execute(editMarkup);
					} catch (TelegramApiException e) {
						e.printStackTrace();
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
		AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
		answerCallbackQuery.setCallbackQueryId(callbackquery.getId());
		answerCallbackQuery.setShowAlert(alert);
		answerCallbackQuery.setText(text);
		execute(answerCallbackQuery);
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
		
		InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
		
		List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
		
		List<InlineKeyboardButton> rowInline = new ArrayList<>();
		rowInline.add(new InlineKeyboardButton().setText(this.urls.get(index)[2]).setCallbackData("gallery:text:" + index));
		
		
		List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
		rowInline2.add(new InlineKeyboardButton().setText(BACK).setCallbackData("gallery:back:" + index));
		rowInline2.add(new InlineKeyboardButton().setText(NEXT).setCallbackData("gallery:next:" + index));
		
		List<InlineKeyboardButton> rowInline3 = new ArrayList<>();
		rowInline3.add(new InlineKeyboardButton().setText("Link").setUrl(this.urls.get(index)[0]));
		
		
		rowsInline.add(rowInline);
		rowsInline.add(rowInline3);
		rowsInline.add(rowInline2);
		
		markupInline.setKeyboard(rowsInline);
		
		return markupInline;
	}
	
	@Override
	public String getBotToken() {
		return BotConfig.ELEKTROLLART_TOKEN;
	}

}
