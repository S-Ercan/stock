package com.selman.collect;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

public class Collector {
    public static void main(String[] args) {
        request(
                "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=MSFT&interval=15min&outputsize=compact&apikey=W6F61C7U07E7E8JX");
    }

    private static void request(String targetURL) {
        try {
            URL obj = new URL(targetURL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + targetURL);
            System.out.println("MetaData Code : " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            JsonObject jsonObject = new JsonParser().parse(response.toString()).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.get("Meta Data").getAsJsonObject().entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
            for (Map.Entry<String, JsonElement> entry : jsonObject.get("Time Series (Daily)").getAsJsonObject().entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }

            in.close();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
