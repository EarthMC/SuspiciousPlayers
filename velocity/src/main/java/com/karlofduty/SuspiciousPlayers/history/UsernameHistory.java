package com.karlofduty.SuspiciousPlayers.history;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.PlayerEntry;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class UsernameHistory {
    private static final Type type = new TypeToken<Map<String, String>>(){}.getType();
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(type, new NameMapDeserializer()).create();

    public static ConcurrentSkipListMap<String, String> deserialize(String string) {
        return gson.fromJson(string, type);
    }

    public static String serialize(Map<String, String> map) {
        return gson.toJson(map, type);
    }

    public static ConcurrentSkipListMap<String, String> newHistory(String oldName, String newName) {
        ConcurrentSkipListMap<String, String> map = new ConcurrentSkipListMap<>(Collections.reverseOrder());
        map.put("-", oldName);
        map.put(PlayerEntry.displayDateFormat.format(System.currentTimeMillis()), newName);
        return map;
    }

    /**
     * Deserializer for the name history package from the Mojang API.
     * The input map is actually an array of unnamed objects with a name and (usually) timestamp, this gets turned into a Map<Long, String> below.
     */
    public static class NameMapDeserializer implements JsonDeserializer<ConcurrentSkipListMap<String, String>>, JsonSerializer<ConcurrentSkipListMap<String, String>> {
        @Override
        public ConcurrentSkipListMap<String, String> deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            try {
                ConcurrentSkipListMap<String, String> map = new ConcurrentSkipListMap<>(Collections.reverseOrder());
                for (JsonElement jItem : json.getAsJsonArray()) {
                    JsonObject jObject = jItem.getAsJsonObject();

                    // Gets the player name, this is always included in each object
                    String name = jObject.get("name").getAsString();

                    // Timestamps are not included on the player's first name so it is set to "-" in that case
                    String timestamp = "-";
                    if (jObject.get("changedToAt") != null)
                        timestamp = PlayerEntry.displayDateFormat.format(jObject.get("changedToAt").getAsLong());

                    map.put(timestamp, name);
                }
                return map;
            } catch (Exception e) {
                SuspiciousPlayers.plugin().logger().error("Error occurred while parsing json package", e);
                return null;
            }
        }

        @Override
        public JsonElement serialize(ConcurrentSkipListMap<String, String> src, Type typeOfSrc, JsonSerializationContext context) {
            List<JsonObject> objects = new ArrayList<>();

            for (Map.Entry<String, String> entry : src.entrySet()) {
                JsonObject object = new JsonObject();
                object.addProperty("name", entry.getValue());

                if (entry.getKey() != null && !"-".equals(entry.getKey())) {
                    try {
                        object.addProperty("changedToAt", PlayerEntry.displayDateFormat.parse(entry.getKey()).getTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                objects.add(object);
            }

            Collections.reverse(objects);

            JsonArray array = new JsonArray();
            objects.forEach(array::add);

            return array;
        }
    }
}
