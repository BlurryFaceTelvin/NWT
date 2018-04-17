import com.africastalking.*;
import com.africastalking.payment.recipient.Consumer;
import com.africastalking.payment.response.B2CResponse;
import com.africastalking.payment.response.CheckoutResponse;
import com.africastalking.voice.CallResponse;
import com.africastalking.voice.action.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;

import java.util.*;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static spark.Spark.port;
import static spark.Spark.post;

public class VoiceApp {
    private static Server server;
    private static int RPC_PORT=25565;
    private static int HTTP_PORT=30001;
    private static PaymentService paymentService;
    private static Gson gson;
    private static String enrollmentResponse;
    private static VoiceService voiceService;
    private static boolean authenticated = false;
    static Map<String,String> data;
    private static void log(String message){
        System.out.println(message);
    }
    private static void setUpAfricasTalking(){
        //AfricasTalking.initialize("sandbox","3951b35df84cb44c81ffeedad845413e7b63bc44068ac07f5d28137f3d049d2b");
        AfricasTalking.initialize("loans","956d6d203f8c06a94026bb57bcad45152d06585978b1d0b3b64bd71823bc281f");
        paymentService = AfricasTalking.getService(AfricasTalking.SERVICE_PAYMENT);
        voiceService = AfricasTalking.getService(AfricasTalking.SERVICE_VOICE);
        server = new Server();
        try {
            server.startInsecure(RPC_PORT);
        } catch (IOException e) {
            System.out.print(e.getMessage());
        }
        System.out.println("Connected");
    }
    public static void main(String[] args) throws UnknownHostException {
        gson = new Gson();
        //initialise voiceit
        //VoiceIt voiceIt = new VoiceIt("676d89d1f5d742f8a42f37a1a37db942");
        VoiceIt voiceIt = new VoiceIt("66f41f2df0564f2cbdfbdeaa668d773c");
        //phrase
        String phraseUrl = "https://voiceit.tech/voicePrint.wav";

        //initialise africastalking
        HashMap<String,String> states = new HashMap<>();
        Inet4Address host = (Inet4Address)Inet4Address.getLocalHost();
        log("\n");
        log(String.format("SDK Server: %s:%d", host.getHostAddress(), RPC_PORT));
        log(String.format("HTTP Server: %s:%d", host.getHostAddress(), HTTP_PORT));
        log("\n");
        String baseUrl = "http://e0a9bc05.ngrok.io";
        setUpAfricasTalking();
        port(HTTP_PORT);

        post("/pay",((request, response) -> {
            return "OK";
        }));
        post("/voice",((request, response) -> {
            //create user using their number
            //Parse post data
            String[] raw = URLDecoder.decode(request.body()).split("&");
            data = new HashMap<>();
            for (String item:raw){
                String[] kw = item.split("=");
                if(kw.length==2){
                    data.put(kw[0],kw[1]);
                }
            }
            //prep state
            boolean Active = data.get("isActive").contentEquals(String.valueOf(1));
            String sessionId = data.get("sessionId");
            String callerNumber = data.get("callerNumber");
            String dtmf = data.get("dtmfDigits");
            String recordingUrl = data.get("recordingUrl");
            //result for the enrollment
            String result = null;
            String authResult = null;
            String authResponseCode;
            ActionBuilder resp = new ActionBuilder();
            StringBuilder callerNo = new StringBuilder(callerNumber);
            callerNo.setCharAt(0,'A');
            //checks if user exists or not
            String[] userStatus = {null};
            if(recordingUrl!=null){
                if(states.get(sessionId).equals("enroll2")||states.get(sessionId).equals("enroll3")||states.get(sessionId).equals("success")) {
                    System.out.println(recordingUrl);
                    enrollmentResponse = voiceIt.createEnrollmentByWavURL(String.valueOf(callerNo), "abcde", recordingUrl);
                    Gson gson = new Gson();
                    VoiceItModel model = gson.fromJson(enrollmentResponse, VoiceItModel.class);
                    result = model.Result;

                    if (!result.equals("Success")) {
                        states.put(sessionId, "error");
                    }

                }else if(states.get(sessionId).equals("auth")){

                    //if user is authenticating
                    String authResponse = voiceIt.authenticationByWavURL(String.valueOf(callerNo),"abcde",recordingUrl);
                    Gson gson = new Gson();
                    VoiceItModel model = gson.fromJson(authResponse,VoiceItModel.class);
                    authResult = model.Result;
                    authResponseCode = model.ResponseCode;
                    System.out.println(authResponseCode);
                    System.out.println(authResult);
                    if(authResponseCode.equals("SUC")){

                        //change the status of the authentication to true
                        //redirect to ussd
                        authenticated=true;
                        states.put(sessionId,"authSuccess");
                    }
                    else {
                        states.put(sessionId,"autherror");
                    }

                }

            }

            int[] numberOfEnrollments = {0};
            //says our menu
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Gson enrollGson = new Gson();
                    try {
                        //check if user exists
                        String userExist = voiceIt.getUser(String.valueOf(callerNo),"abcde");
                        Gson userExistJson = new Gson();
                        VoiceItModel voiceItModel = userExistJson.fromJson(userExist,VoiceItModel.class);
                        userStatus[0] = voiceItModel.Result;
                        //System.out.println(userExist);
                        System.out.print(userStatus[0]);
                        if(userStatus[0].equals("Success")) {
                            String getEnrollmentResponse = voiceIt.getEnrollments(String.valueOf(callerNo), "abcde");
                            VoiceItEnrollModel enrollmentModel = enrollGson.fromJson(getEnrollmentResponse, VoiceItEnrollModel.class);
                            String[] getEnrollmentResults = enrollmentModel.Result;
                            System.out.println(getEnrollmentResults.length);
                            numberOfEnrollments[0] = getEnrollmentResults.length;
                        }else {
                            //if there is no created user we have 0 enrollments
                            numberOfEnrollments[0]=0;
                            //create user
                            voiceIt.createUser(String.valueOf(callerNo),"abcde");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.run();

            System.out.println(String.valueOf(Active));
            //check our state
            String state = Active? states.getOrDefault(sessionId,"menu"):"afs";
            System.out.println(state);

            switch (state){
                case "menu":
                    states.put(sessionId,"process");
                    System.out.println(numberOfEnrollments[0]+"menu");
                    if(numberOfEnrollments[0]>=3){
                        resp.say(new Say("Welcome dear user"))
                                .getDigits(new GetDigits(new Say("Press 2 to authenticate your voice "), 1, "#", null));
                    }else {
                        resp.say(new Say("Welcome dear user"))
                                .getDigits(new GetDigits(new Say("Press 1 to enroll your voice "), 1, "#", null));
                    }
                    break;
                case "process":
                    //get the users input
                    switch (dtmf){
                        //register with voice
                        case "1":
                            states.put(sessionId,"enroll2");
                            //play song from the song url
                            resp.say(new Say("Repeat this phrase after the beep and press 0 when done recording"));
                            resp.record(new Record(new Play(new URL(phraseUrl)), "0", 10, 5, true, true, null));
                            break;
                        //authenticate
                        case "2":
                            states.put(sessionId,"auth");
                            System.out.println(numberOfEnrollments[0]);
                            //check if user has registered
                            if(numberOfEnrollments[0]>=3) {
                                resp.record(new Record(new Say("Please say the phrase you registered with and press 0 when done recording"), "0", 10, 5, true, true, null));
                            }else {
                                resp.say(new Say("You have not registered yet"))
                                        .redirect(new Redirect(new URL(baseUrl+"/voice")));
                            }

                            break;
                    }
                    break;
                //second enrollment
                case "enroll2":
                    states.put(sessionId,"enroll3");
                    //play song from the song url
                    resp.say(new Say("Repeat this phrase after the beep and press 0 when done recording"));
                    resp.record(new Record(new Play(new URL(phraseUrl)), "0", 10, 5, true, true, null));
                    break;
                //third enrollment
                case "enroll3":
                    states.put(sessionId,"success");
                    //play song from the song url
                    resp.say(new Say("Repeat this phrase after the beep and press 0 when done recording"));
                    resp.record(new Record(new Play(new URL(phraseUrl)), "0", 10, 5, true, true, null));
                    break;
                //successful enrollments
                case "success":
                    states.put(sessionId,"menu");
                    resp.say(new Say("You have successfully registered your voice"));
                    break;
                //error handling for the enrollment
                case "error":
                    states.put(sessionId,"menu");
                    resp.say(new Say(result));
                    break;
                    //error for the authentication
                case "autherror":
                    states.put(sessionId,"menu");
                    resp.say(new Say(authResult));
                    break;
                case "authSuccess":
                    resp.say(new Say("Authentication was successful"));
                    break;

                    default:
                        resp.say(new Say("Something has gone wrong bye"))
                                .redirect(new Redirect(new URL(baseUrl+"/voice")));
                        break;
            }

            return resp.build();
        }));

        post("/ussd",(request, response) -> {
            //Parse post data
            String[] raw = URLDecoder.decode(request.body()).split("&");
            data = new HashMap<>();


            for (String item:raw){
                String[] kw = item.split("=");
                if(kw.length==2){
                    data.put(kw[0],kw[1]);
                }
            }

            String callerNumber = data.get("phoneNumber");
            String text = data.get("text");
            System.out.println(text);
            if(text==null)
                text = "";
            String[] datar = text.split("\\*");
            for (String values:datar){
                System.out.print(values+"yho");
            }
            if(text=="3*"){

            }
            switch (text){
                case "":
                    String data = "CON Hello "+callerNumber+ "\n"+
                            "1. Register \n"+
                            "2. Deposit \n"+
                            "3. Withdraw";
                    return data;
                case "1":
                    if(authenticated){
                        return "END Your are already Registered";
                    }else {
                        voiceService.call(callerNumber, "+254711082432", new Callback<CallResponse>() {
                            @Override
                            public void onSuccess(CallResponse callResponse) {
                                System.out.println("Success");
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                System.out.println("Failed");
                            }
                        });
                    }
                    break;
                case "2":
                    System.out.print("authentication state "+authenticated);
                    if(authenticated) {
                        authenticated = false;
                        return "CON Deposit money";
                    }
                    else {
                        voiceService.call(callerNumber, "+254711082432", new Callback<CallResponse>() {
                            @Override
                            public void onSuccess(CallResponse callResponse) {
                                System.out.println("Success");
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                System.out.println("Failed");
                            }
                        });
                    }
                    //c2b
                    break;
                case "3":
                    if(authenticated) {
                        authenticated = false;
                        return "CON Enter the amount you want to withdraw";
                    }else {
                        voiceService.call(callerNumber, "+254711082432", new Callback<CallResponse>() {
                            @Override
                            public void onSuccess(CallResponse callResponse) {
                                System.out.println("Success");
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                System.out.println("Failed");
                            }
                        });
                    }
                    /*
                    final String[] payStatus = new String[1];
                    Consumer consumer = new Consumer("Telvin",callerNumber,"KES 100",Consumer.REASON_BUSINESS);
                    List<Consumer> list = new ArrayList<>();
                    list.add(consumer);
                    paymentService.mobileB2C("Test", list, new Callback<B2CResponse>() {
                        @Override
                        public void onSuccess(B2CResponse b2CResponse) {
                            System.out.println(b2CResponse.entries.get(0));
                            payStatus[0] = "END SUCCESSFUL";
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            payStatus[0] = "END"+throwable;
                        }
                    });

                    return payStatus[0];
                    */
                    break;

                    default:
                        String option = "CON You have entered the wrong option";
                        return option;
            }
            return "END OK";
        });



    }

    static class VoiceItModel{
        private String ResponseCode,Result;
    }
    static class  VoiceItEnrollModel{
        private String[] Result;
    }
    static class AfricasTalkingVoiceModel{
        private String callerNo;
    }

}
