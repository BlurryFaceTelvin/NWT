import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class VoiceIt {


    String developerId;
    String platformId = "3";
    public VoiceIt(String developerId) {
        this.developerId = developerId;
    }

    private String GetSHA256(String data) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(data.getBytes());
            byte[] hash = sha.digest();

            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String readInputStream(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            int result = inputStream.read();
            while (result != -1) {
                outputStream.write((byte) result);
                result = inputStream.read();
            }
            return outputStream.toString();
        }
    }

    public String getUser(String userId, String password) throws IOException {
        URL url = new URL("https://siv.voiceprintportal.com/sivservice/api/users");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.addRequestProperty("UserId", userId);
            connection.addRequestProperty("VsitPassword", GetSHA256(password));
            connection.addRequestProperty("VsitDeveloperId", developerId);
            connection.addRequestProperty("PlatformID", platformId);
            // read response
            try (InputStream inputStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream()) {
                return readInputStream(inputStream);
            }
        }
        finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    public String createUser(String userId, String password) throws IOException {
        URL url = new URL("https://siv.voiceprintportal.com/sivservice/api/users");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.addRequestProperty("UserId", userId);
            connection.addRequestProperty("VsitPassword", GetSHA256(password));
            connection.addRequestProperty("VsitDeveloperId", developerId);
            connection.addRequestProperty("PlatformID", platformId);

            // read response
            try (InputStream inputStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream()) {
                return readInputStream(inputStream);
            }
        }
        finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    public String deleteUser(String userId, String password) throws IOException {
        URL url = new URL("https://siv.voiceprintportal.com/sivservice/api/users");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("DELETE");
            connection.addRequestProperty("UserId", userId);
            connection.addRequestProperty("VsitPassword", GetSHA256(password));
            connection.addRequestProperty("VsitDeveloperId", developerId);
            connection.addRequestProperty("PlatformID", platformId);

            // read response
            try (InputStream inputStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream()) {
                return readInputStream(inputStream);
            }
        }
        finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    public String createEnrollment(String userId, String password,String pathToEnrollmentWav,String contentLanguage) throws IOException {
        URL url = new URL("https://siv.voiceprintportal.com/sivservice/api/enrollments");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        byte [] myData = Files.readAllBytes(Paths.get(pathToEnrollmentWav));
        try {
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.addRequestProperty("PlatformID", platformId);
            connection.addRequestProperty("UserId", userId);
            connection.addRequestProperty("VsitPassword", GetSHA256(password));
            connection.addRequestProperty("VsitDeveloperId", developerId);
            connection.addRequestProperty("ContentLanguage", contentLanguage);
            connection.setRequestProperty("Content-Type","audio/wav");
            DataOutputStream request = new DataOutputStream(connection.getOutputStream());
            request.write(myData);
            request.flush();
            request.close();
            // read response
            try (InputStream inputStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream()) {
                return readInputStream(inputStream);
            }
        }
        finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    public String createEnrollment(String userId, String password,String pathToEnrollmentWav)throws IOException{
        try{
            return createEnrollment(userId,password,pathToEnrollmentWav,"");
        }
        catch(IOException e){
            return "Failed: IOException";
        }
    }

    public String createEnrollmentByWavURL(String userId, String password,String urlToEnrollmentWav,String contentLanguage) throws IOException {
        URL url = new URL("https://siv.voiceprintportal.com/sivservice/api/enrollments/bywavurl");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.addRequestProperty("UserId", userId);
            connection.addRequestProperty("VsitPassword", GetSHA256(password));
            connection.addRequestProperty("VsitDeveloperId", developerId);
            connection.addRequestProperty("VsitwavURL", urlToEnrollmentWav);
            connection.addRequestProperty("ContentLanguage", contentLanguage);
            connection.addRequestProperty("PlatformID", platformId);
            connection.setRequestProperty("Content-Type","audio/wav");

            // read response
            try (InputStream inputStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream()) {
                return readInputStream(inputStream);
            }
        }
        finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    public String createEnrollmentByWavURL(String userId, String password,String urlToEnrollmentWav)throws IOException{
        try{
            return createEnrollmentByWavURL(userId,password,urlToEnrollmentWav,"");
        }
        catch(IOException e){
            return "Failed: IOException";
        }
    }

    public String deleteEnrollment(String userId, String password,String enrollmentId) throws IOException {
        URL url = new URL("https://siv.voiceprintportal.com/sivservice/api/enrollments"+"/"+enrollmentId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("DELETE");
            connection.addRequestProperty("UserId", userId);
            connection.addRequestProperty("VsitPassword", GetSHA256(password));
            connection.addRequestProperty("VsitDeveloperId", developerId);
            connection.addRequestProperty("PlatformID", platformId);

            // read response
            try (InputStream inputStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream()) {
                return readInputStream(inputStream);
            }
        }
        finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    public String getEnrollments(String userId, String password) throws IOException {
        URL url = new URL("https://siv.voiceprintportal.com/sivservice/api/enrollments");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.addRequestProperty("UserId", userId);
            connection.addRequestProperty("VsitPassword", GetSHA256(password));
            connection.addRequestProperty("VsitDeveloperId", developerId);
            connection.addRequestProperty("PlatformID", platformId);

            // read response
            try (InputStream inputStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream()) {
                return readInputStream(inputStream);
            }
        }
        finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    public String authentication(String userId, String password,String pathToAuthenticationWav, String contentLanguage) throws IOException {
        URL url = new URL("https://siv.voiceprintportal.com/sivservice/api/authentications");
        byte [] myData = Files.readAllBytes(Paths.get(pathToAuthenticationWav));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.addRequestProperty("UserId", userId);
            connection.addRequestProperty("VsitPassword", GetSHA256(password));
            connection.addRequestProperty("VsitDeveloperId", developerId);
            connection.addRequestProperty("ContentLanguage", contentLanguage);
            connection.addRequestProperty("PlatformID", platformId);
            connection.setRequestProperty("Content-Type","audio/wav");

            DataOutputStream request = new DataOutputStream(connection.getOutputStream());
            request.write(myData);
            request.flush();
            request.close();

            // read response
            try (InputStream inputStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream()) {
                return readInputStream(inputStream);
            }
        }
        finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    public String authentication(String userId, String password, String pathToAuthenticationWav)throws IOException{
        try{
            return authentication(userId, password, pathToAuthenticationWav,"");
        }
        catch(IOException e){
            return "Failed: IOException";
        }
    }

    public String authenticationByWavURL(String userId, String password, String urlToAuthenticationWav, String contentLanguage) throws IOException {
        URL url = new URL("https://siv.voiceprintportal.com/sivservice/api/authentications/bywavurl");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.addRequestProperty("UserId", userId);
            connection.addRequestProperty("VsitPassword", GetSHA256(password));
            connection.addRequestProperty("VsitDeveloperId", developerId);
            connection.addRequestProperty("VsitwavURL", urlToAuthenticationWav);
            connection.addRequestProperty("ContentLanguage", contentLanguage);
            connection.addRequestProperty("PlatformID", platformId);
            connection.setRequestProperty("Content-Type","audio/wav");

            // read response
            try (InputStream inputStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream()) {
                return readInputStream(inputStream);
            }
        }
        finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    public String authenticationByWavURL(String userId, String password, String urlToAuthenticationWav)throws IOException{
        try{
            return authenticationByWavURL(userId, password, urlToAuthenticationWav,"");
        }
        catch(IOException e){
            return "Failed: IOException";
        }
    }


}
