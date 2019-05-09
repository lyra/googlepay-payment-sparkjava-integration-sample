package com.lyra.googlepay;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import spark.Route;

import java.util.HashMap;
import java.util.Map;

/**
 * Example controller that handles a GooglePay payment call on merchant servers.<p></p>
 * <p>
 * This controller just extracts parameters from Request and send them to the service component.<p></p>
 * <p>
 * For readability purposes in this example:
 * <li>We do not use logs</li>
 * <li>The JSON content is converted into a basic map structure. Use an appropiate DTO class hierarchy instead
 * if you want to provide a more scalable and robust code</li>
 *
 * @author Lyra Network
 */
public class GooglePayController {
    /**
     * Retrieve parameters from request in order to send them to the service layer.<p></p>
     * <p>
     * In the case of error:
     * <li>Returns HTTP code 400(Bad Request) if the JSON format is not valid</li>
     * <li>Returns HTTP code 500(Internal Server Error) if an unexpected error occur</li>
     */
    public static Route createPayment = (request, response) -> {
        String transactionStatus = "";
        response.type("application/json; charset=utf-8"); //We return always a JSON response

        try {
            //Retrieve parameters
            String ip = request.ip();
            Map requestData = new Gson().fromJson(request.body(), HashMap.class); //Parse JSON into a Map structure
            Map<String, String> createPaymentData = (Map) requestData.get("createPaymentData");
            Map payload = (Map) requestData.get("walletPayload");

            //Call Service
            transactionStatus = new GooglePayService().processPayment(createPaymentData, payload, ip);

        } catch (JsonSyntaxException jse) {
            response.status(400);
            return toJSONError("Bad Request");
        } catch (Exception e) {
            response.status(500);
            return toJSONError("Internal Server Error");
        }

        //If everything goes OK return transaction status
        return toJSONOk(transactionStatus);
    };

    /*
     *
     * Static example methods to build a JSON response for the client
     *
     */
    private static String toJSONOk(String transactionStatus) {
        return responseToJSON("OK", "", transactionStatus);
    }

    private static String toJSONError(String message) {
        return responseToJSON("ERROR", message, "");
    }

    private static String responseToJSON(String status, String errorMessage, String transactionStatus) {
        JsonObject responseObject = new JsonObject();

        responseObject.addProperty("status", status);
        responseObject.addProperty("errorMessage", errorMessage);
        responseObject.addProperty("transactionStatus", transactionStatus);

        return responseObject.toString();
    }
}