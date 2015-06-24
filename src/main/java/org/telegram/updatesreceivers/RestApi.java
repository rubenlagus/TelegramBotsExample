package org.telegram.updatesreceivers;

import org.telegram.api.Update;
import org.telegram.updateshandlers.UpdatesCallback;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Rest api to for webhook callback function
 * @date 20 of June of 2015
 */
@Path("callback")
public class RestApi {

    private final UpdatesCallback callback;

    public RestApi(UpdatesCallback callback) {
        this.callback = callback;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateReceived(Update update) {
        this.callback.onUpdateReceived(update);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String testReceived() {
        return "Hi there";
    }
}
