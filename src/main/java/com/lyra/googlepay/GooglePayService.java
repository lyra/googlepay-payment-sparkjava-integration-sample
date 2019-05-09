package com.lyra.googlepay;

import com.google.gson.Gson;
import com.lyra.ServerConfiguration;
import com.lyra.vads.ws.v5.CommonResponse;
import eu.payzen.webservices.sdk.Payment;
import eu.payzen.webservices.sdk.ServiceResult;
import eu.payzen.webservices.sdk.builder.PaymentBuilder;
import eu.payzen.webservices.sdk.builder.request.*;
import org.apache.commons.lang3.StringUtils;

import javax.xml.ws.WebServiceException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class processes GooglePay payment, performing an authorization and creating a new transaction in Lyra
 * platform.<p></p>
 * <p>
 * Note that this example uses Lyra WS SOAP API via an open source SDK that simplifies this process in Java, hiding
 * the WS port creation, facilitating the parameter creation using builders and providing generated stubs.<br>
 * <a href="/a>https://github.com/payzen/payzen-webservices-v5-sdk-java}">
 * https://github.com/payzen/payzen-webservices-v5-sdk-java</a><br>
 * Feel free to use other method/tools to call the WebServices as needed.
 *
 * @author Lyra Network
 */
public class GooglePayService {
    private static final String ORDER_ID_EXAMPLE_PREFIX = "GooglePay Demo - ";

    /**
     * This method allows to process a GooglePay payment. It calls a WebService SOAP that performs the authorization
     * of the transaction. <a href="/a>https://payzen.io/en-EN/form-payment/quick-start-guide/send-an-html-payment-form-via-post..html"}>
     * See guide to create payment with Lyra's WebServices SOAP v5</a></p>
     * <p>
     * If the WebService call is Ok and the transaction is created, it returns the status of the
     * transaction.<p></p>
     * If there is any problem, it will launch a runtime exception ({@link WebServiceException})
     *
     * @param createPaymentData The information of the payment (amount, currency, order ID, etc)
     * @param payload           The Technical information needed for GooglePay Wallet Payload
     * @param ip                The client's IP that should be stored in the transaction
     * @return the status of the transaction. The transaction could be accepted (codes AUTHORISED, AUTHORISED_TO_VALIDATE
     * or WAITING_AUTHORISATION_TO_VALIDATE) or refused (code REFUSED)
     */
    public String processPayment(Map createPaymentData, Map payload, String ip) {
        String paymentResult = null;

        //Read configuration data
        Map<String, String> configuration =
                ServerConfiguration.getConfiguration((String) createPaymentData.get("mode"));

        //Read parameters from paymentDetails
        Long amount = Long.valueOf((String) createPaymentData.get("amount"));
        String orderId = calculateOrderId((String) createPaymentData.get("orderId")); //Generate the OrderID if not provided
        Integer currency = Integer.valueOf((String) createPaymentData.get("currency"));
        String email = (String) createPaymentData.get("email");
        String cardSecurityCode = (String) createPaymentData.get("cardSecurityCode");
        String paymentOptionCode = (String) createPaymentData.get("paymentOptionCode");
        String productInformation = parseProductInformationToString((Map) createPaymentData.get("appVersion"));
        String deviceInformation = new Gson().toJson(createPaymentData.get("deviceInformation"));

        //Read parameters from payload
        String walletPayload = new Gson().toJson(payload);

        //Perform SOAP Web Service call on payment platform
        ServiceResult webServiceResult = null;
        Map<String, String> wsConfiguration = createWsConfiguration(configuration);
        try {
            webServiceResult = Payment.create(
                    PaymentBuilder.getBuilder()
                            .paymentSource("EC")
                            .submissionDate(new Date())
                            .comment("Mobile demo GooglePay")
                            .payment(PaymentRequestBuilder.create()
                                    .amount(Long.valueOf(amount))
                                    .currency(currency)
                                    .expectedCaptureDate(new Date())
                                    .paymentOptionCode(paymentOptionCode)
                                    .build())
                            .order(OrderRequestBuilder.create()
                                    .orderId(orderId)
                                    .build())
                            .card(CardRequestBuilder.create()
                                    .scheme("GOOGLEPAY")
                                    .cardSecurityCode(cardSecurityCode)
                                    .walletPayload(walletPayload)
                                    .build())
                            .tech(TechRequestBuilder.create()
                                    .browserUserAgent(deviceInformation)
                                    .integrationType(productInformation)
                                    .build())
                            .customer(CustomerRequestBuilder.create()
                                    .billingDetailsRequest(
                                            CustomerRequestBuilder.BillingDetailsRequestBuilder.create()
                                                    .email(StringUtils.isNotEmpty(email) ? email : null)
                                                    .build())
                                    .build())
                            .buildCreate(),
                    wsConfiguration
            );
        } catch (WebServiceException wse) { //Handle Errors
            //re-catch it on main app logic
            throw wse;
        }

        //Handle WebService response
        CommonResponse wsCommonResponse = webServiceResult.getCommonResponse();
        //If WebService ends with Error code reports it to controller
        if (wsCommonResponse.getResponseCode() != 0) {
            String wsError = String.format("Cannot create transaction because something wrong happened. Error Code: %d. " +
                            "Error detail: %s"
                    , wsCommonResponse.getResponseCode(), wsCommonResponse.getResponseCodeDetail());

            throw new WebServiceException(wsError);
        } else {
            // Webservice call ends OK, so it is time to retrieve the status of created transaction
            String transactionStatus = wsCommonResponse.getTransactionStatusLabel();

            paymentResult = transactionStatus;
        }

        //Returns payment transaction status if WS call was OK
        return paymentResult;
    }

    /*
     * Parse all the production information and flat it in a single line with the format:
     * applicationId (versionCode - versionName)
     */
    private static String parseProductInformationToString(Map<String, String> appVersion) {
        StringBuilder productInformation = new StringBuilder();
        if (appVersion != null) {
            productInformation.append(appVersion.get("applicationId")).append(' ');
            productInformation.append("(").append(appVersion.get("versionCode")).append(" - ");
            productInformation.append(appVersion.get("versionName")).append(")");
        }
        return productInformation.toString();
    }

    /*
     * Creates a generated orderId if the provided one is not valid
     */
    private static String calculateOrderId(String orderId) {
        if (StringUtils.isNotEmpty(orderId)) {
            return orderId;
        } else {
            //You must define your personal unique OrderId, this is just an example
            return ORDER_ID_EXAMPLE_PREFIX + UUID.randomUUID().toString();
        }
    }

    /*
     * Fill in the web service configuration using the server configuration data
     */
    private static Map<String, String> createWsConfiguration(Map<String, String> configuration) {
        Map<String, String> wsConfiguration = new HashMap<>();

        wsConfiguration.put("shopId", configuration.get("merchantSiteId"));
        wsConfiguration.put("shopKey", configuration.get("usedMerchantKey"));
        wsConfiguration.put("mode", configuration.get("mode"));
        wsConfiguration.put("endpointHost", configuration.get("paymentPlatformUrl").replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)",""));
        wsConfiguration.put("secureConnection", "true");
        wsConfiguration.put("connectionTimeout", configuration.get("webServiceConnectionTimeout"));
        wsConfiguration.put("requestTimeout", configuration.get("webServiceRequestTimeout"));

        return wsConfiguration;
    }
}
