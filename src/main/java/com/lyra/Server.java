package com.lyra;

import com.lyra.googlepay.GooglePayController;

import static spark.Spark.port;
import static spark.Spark.post;

/**
 * This is the entry point for Spark Java Server. <p></p>
 *
 * It defines the port and the route for the GooglePay payment
 *
 * @author Lyra Network
 */
public class Server {
    public static void main(String[] args) {
        // Configure Spark
        port(9090);

        //Routes
        post("/", GooglePayController.createPayment);
    }
}
