package nl.codestix.mcdiscordregions;


import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class MojangAPI {

    public final static Pattern PLAYER_NAME_REGEX = Pattern.compile("[a-zA-Z0-9_]{3,16}");

    public static boolean isValidName(String playerName) {
        return PLAYER_NAME_REGEX.matcher(playerName).matches();
    }

    public static UUID playerNameToUUID(String playerName) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            con.setConnectTimeout(1000);
            con.setReadTimeout(1000);
            con.setRequestMethod("GET");

            if (con.getResponseCode() != 200)
                throw new Exception("Request could not be fulfilled: " + con.getResponseMessage());

            JSONObject obj = (JSONObject)new JSONParser().parse(new InputStreamReader(con.getInputStream(), "UTF-8"));
            con.disconnect();

            Object idObj = obj.get("id");
            if (idObj == null)
                throw new NullPointerException("idObj is null.");
            String idString = idObj.toString();
            if (idString.length() != 32)
                throw new NullPointerException("idString is invalid.");

            BigInteger bi1 = new BigInteger(idString.substring(0, 16), 16);
            BigInteger bi2 = new BigInteger(idString.substring(16, 32), 16);
            return new UUID(bi1.longValue(), bi2.longValue());
        }
        catch(Exception ex) {
            Bukkit.getLogger().warning("Could not request: " + ex);
            return null;
        }
    }
}
