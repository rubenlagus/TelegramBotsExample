package org.telegram.updatesreceivers;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.BuildVars;
import org.telegram.api.Update;
import org.telegram.methods.Constants;
import org.telegram.methods.GetUpdates;
import org.telegram.methods.SendMessage;
import org.telegram.services.BotLogger;
import org.telegram.updateshandlers.UpdatesCallback;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Thread to request updates with active wait
 * @date 20 of June of 2015
 */
public class UpdatesThread {
    private static volatile BotLogger log = BotLogger.getLogger(UpdatesThread.class.getName());

    private final UpdatesCallback callback;
    private final ReaderThread readerThread;
    private final HandlerThread handlerThread;
    private int lastReceivedUpdate;
    private String token;
    private final ConcurrentLinkedQueue<Update> receivedUpdates = new ConcurrentLinkedQueue<>();

    public UpdatesThread(String token, UpdatesCallback callback) {
        this.token = token;
        this.callback = callback;
        this.lastReceivedUpdate = -1;
        this.readerThread = new ReaderThread();
        this.readerThread.start();
        this.handlerThread = new HandlerThread();
        this.handlerThread.start();
    }

    private class ReaderThread extends Thread {
        @Override
        public void run() {
            setPriority(Thread.MIN_PRIORITY);
            while(true) {
                GetUpdates request = new GetUpdates();
                request.setLimit(100);
                request.setOffset(lastReceivedUpdate + 1);
                CloseableHttpClient httpclient = HttpClientBuilder.create().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
                String url = Constants.BASEURL + token + "/" + GetUpdates.PATH;
                HttpPost httpPost = new HttpPost(url);
                List<NameValuePair> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(new BasicNameValuePair(GetUpdates.OFFSET_FIELD, request.getOffset()+""));
                nameValuePairs.add(new BasicNameValuePair(GetUpdates.LIMIT_FIELD, request.getLimit()+""));
                if (request.getTimeout() != null) {
                    nameValuePairs.add(new BasicNameValuePair(GetUpdates.TIMEOUT_FIELD, request.getTimeout()+""));
                }
                try {
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
                    httpPost.addHeader("Content-type", "application/x-www-form-urlencoded");
                    httpPost.addHeader("charset", "UTF-8");
                    HttpResponse response;
                    log.debug(httpPost.toString());
                    response = httpclient.execute(httpPost);
                    HttpEntity ht = response.getEntity();

                    BufferedHttpEntity buf = new BufferedHttpEntity(ht);
                    String responseContent = EntityUtils.toString(buf, "UTF-8");

                    try {
                        JSONObject jsonObject = new JSONObject(responseContent);
                        if (!jsonObject.getBoolean("ok")) {
                            throw new InvalidObjectException(jsonObject.toString());
                        }
                        JSONArray jsonArray = jsonObject.getJSONArray("result");
                        log.debug(jsonArray.toString());
                        if (jsonArray.length() != 0) {
                            List<Update> updates = new ArrayList<>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                Update update = new Update(jsonArray.getJSONObject(i));
                                if (update.getUpdateId() > lastReceivedUpdate) {
                                    lastReceivedUpdate = update.getUpdateId();
                                }
                                updates.add(update);

                            }
                            synchronized (receivedUpdates) {
                                receivedUpdates.addAll(updates);
                                receivedUpdates.notifyAll();
                            }
                        } else {
                            try {
                                synchronized (this) {
                                    this.wait(500);
                                }
                            } catch (InterruptedException e) {
                                log.error(e);
                                continue;
                            }
                        }
                    } catch (JSONException e) {
                        log.warning(e);
                    }
                } catch (IOException e) {
                    log.warning(e);
                }
            }
        }
    }

    private class HandlerThread extends Thread {
        @Override
        public void run() {
            setPriority(Thread.MIN_PRIORITY);
            while(true) {
                Update update = receivedUpdates.poll();
                if (update == null) {
                    synchronized (receivedUpdates) {
                        try {
                            receivedUpdates.wait();
                        } catch (InterruptedException e) {
                            log.error(e);
                            continue;
                        }
                        update = receivedUpdates.poll();
                        if (update == null) {
                            continue;
                        }
                    }
                }

                callback.onUpdateReceived(update);
            }
        }
    }
}
