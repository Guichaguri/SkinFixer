package guichaguri.skinfixer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.UnsupportedEncodingException;
import javax.xml.bind.DatatypeConverter;

/**
 * @author Guilherme Chaguri
 */
public class SkinConfig {

    private static final String DEFAULT_TEX_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%UUID%";

    public UrlObject profileUrl = new UrlObject(DEFAULT_TEX_URL, true);

    public UrlObject skinUrl = new UrlObject("", false);

    public UrlObject capeUrl = new UrlObject("", false);

    public UrlObject getFromType(UrlType type) {
        if(profileUrl == null || profileUrl.url.isEmpty()) {
            profileUrl = new UrlObject(DEFAULT_TEX_URL, true);
        }

        switch(type) {
            case SKIN:
                return skinUrl != null && !skinUrl.url.isEmpty() ? skinUrl : profileUrl;
            case CAPE:
                return capeUrl != null && !capeUrl.url.isEmpty() ? capeUrl : profileUrl;
            case TEXTURES:
            default:
                return profileUrl;
        }
    }

    public String getFromJson(JsonElement json, UrlType type) {
        JsonArray array = json.getAsJsonObject().get("properties").getAsJsonArray();

        for(int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            if(obj.get("name").getAsString().equals("textures")) {

                String texturesJson = obj.get("value").getAsString();
                try {
                    texturesJson = new String(DatatypeConverter.parseBase64Binary(texturesJson), "UTF-8");
                } catch(UnsupportedEncodingException e) {}
                JsonObject texturesObj = new JsonParser().parse(texturesJson).getAsJsonObject();

                String typeEntry;

                if(type == UrlType.SKIN) {
                    typeEntry = "SKIN";
                } else if(type == UrlType.CAPE) {
                    typeEntry = "CAPE";
                } else {
                    return null;
                }

                return texturesObj.getAsJsonObject("textures").getAsJsonObject(typeEntry).get("url").getAsString();
            }
        }

        return null;
    }

    public JsonElement toJson(String uuid, String username, String skinUrl, String capeUrl) {
        // Lets add all the information in case the game really needs them

        JsonObject tex = new JsonObject();

        if(skinUrl != null && !skinUrl.isEmpty()) {
            JsonObject skin = new JsonObject();
            skin.addProperty("url", skinUrl);
            tex.add("SKIN", skin);
        }

        if(capeUrl != null && !capeUrl.isEmpty()) {
            JsonObject cape = new JsonObject();
            cape.addProperty("url", capeUrl);
            tex.add("CAPE", cape);
        }

        JsonObject textures = new JsonObject();
        textures.addProperty("timestamp", (int)(System.currentTimeMillis() / 1000L));
        textures.addProperty("profileId", uuid);
        textures.addProperty("profileName", username);
        textures.add("textures", tex);

        JsonObject property = new JsonObject();
        property.addProperty("name", "textures");
        property.addProperty("value", DatatypeConverter.printBase64Binary(textures.toString().getBytes()));

        JsonArray properties = new JsonArray();
        properties.add(property);

        JsonObject obj = new JsonObject();
        obj.addProperty("id", uuid);
        obj.addProperty("name", username);
        obj.add("properties", properties);

        return obj;
    }

    public static class UrlObject {
        public String url;
        public boolean json;

        public UrlObject() {}

        public UrlObject(String url, boolean json) {
            this.url = url;
            this.json = json;
        }
    }

    enum UrlType {
        TEXTURES, SKIN, CAPE
    }

}
