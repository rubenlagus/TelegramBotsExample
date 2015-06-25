package org.telegram.updatesreceivers;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.BuildVars;
import org.telegram.api.Update;
import org.telegram.methods.Constants;
import org.telegram.methods.GetUpdates;
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
            while(true) {
                GetUpdates request = new GetUpdates();
                request.setOffset(lastReceivedUpdate+1);
                CloseableHttpClient httpclient = HttpClientBuilder.create().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
                String url = Constants.BASEURL + token + "/" + GetUpdates.PATH;
                HttpGet httpGet = new HttpGet(url + request.getUrlParams());
                httpGet.addHeader("Content-type", "application/x-www-form-urlencoded");
                httpGet.addHeader("charset", "UTF-8");
                HttpResponse response;
                try {
                    response = httpclient.execute(httpGet);
                    HttpEntity ht = response.getEntity();

                    BufferedHttpEntity buf = new BufferedHttpEntity(ht);
                    String responseContent = EntityUtils.toString(buf, "UTF-8");

                    try {
                        JSONObject jsonObject = new JSONObject(responseContent);
                        if (!jsonObject.getBoolean("ok")) {
                            throw new InvalidObjectException(jsonObject.toString());
                        }
                        JSONArray jsonArray = jsonObject.getJSONArray("result");
                        if (jsonArray.length() != 0) {
                            List<Update> updates = new ArrayList<Update>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                Update update = new Update(jsonArray.getJSONObject(i));
                                if (update.getUpdateId() > lastReceivedUpdate) {
                                    lastReceivedUpdate = update.getUpdateId();
                                }
                                updates.add(update);

                            }
                            receivedUpdates.addAll(updates);
                            synchronized (receivedUpdates) {
                                receivedUpdates.notifyAll();
                            }
                        } else {
                            try {
                                synchronized (this) {
                                    this.wait(500);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                continue;
                            }
                        }
                    } catch (JSONException e) {
                        try {
                            synchronized (this) {
                                this.wait(500);
                            }
                        } catch (InterruptedException e1) {
                            e.printStackTrace();
                            continue;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class HandlerThread extends Thread {
        @Override
        public void run() {
            while(true) {
                Update update = receivedUpdates.poll();
                if (update == null) {
                    synchronized (receivedUpdates) {
                        try {
                            receivedUpdates.wait();
                        } catch (InterruptedException e) {
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
