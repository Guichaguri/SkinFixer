package guichaguri.skinfixer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import guichaguri.skinfixer.SkinConfig.UrlObject;
import guichaguri.skinfixer.SkinConfig.UrlType;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Guilherme Chaguri
 */
public class SkinWorker implements Runnable {

    private static final Pattern AWS_SKIN_REGEX = Pattern.compile("https?:\\/\\/s3\\.amazonaws\\.com\\/MinecraftSkins\\/(.*)\\.png");
    private static final Pattern AWS_CAPE_REGEX = Pattern.compile("https?:\\/\\/s3\\.amazonaws\\.com\\/MinecraftCloaks\\/(.*)\\.png");
    private static final Pattern MC_SKIN_REGEX = Pattern.compile("https?:\\/\\/skins\\.minecraft\\.net\\/MinecraftSkins\\/(.*)\\.png");
    private static final Pattern MC_CAPE_REGEX = Pattern.compile("https?:\\/\\/skins\\.minecraft\\.net\\/MinecraftCloaks\\/(.*)\\.png");
    private static final Pattern PROFILE_REGEX = Pattern.compile("https?:\\/\\/sessionserver\\.mojang\\.com\\/session\\/minecraft\\/profile\\/(.*)");

    private static final String USERNAME = "%USERNAME%";
    private static final String UUID = "%UUID%";

    private static SkinConfig CONFIG;
    private static SkinWorker WORKER;

    /**
     * The hook that should be fired from the classes that create the skin/cape/profile URLs
     */
    public static String getURL(String oldUrl) {
        if(CONFIG == null) loadConfig();

        Matcher m;

        m = PROFILE_REGEX.matcher(oldUrl);
        if(m.matches()) {
            return replaceUrl(UrlType.TEXTURES, m.group(1), null);
        }

        m = AWS_SKIN_REGEX.matcher(oldUrl);
        if(m.matches()) {
            return replaceUrl(UrlType.SKIN, null, m.group(1));
        }

        m = AWS_CAPE_REGEX.matcher(oldUrl);
        if(m.matches()) {
            return replaceUrl(UrlType.CAPE, null, m.group(1));
        }

        m = MC_SKIN_REGEX.matcher(oldUrl);
        if(m.matches()) {
            return replaceUrl(UrlType.SKIN, null, m.group(1));
        }

        m = MC_CAPE_REGEX.matcher(oldUrl);
        if(m.matches()) {
            return replaceUrl(UrlType.CAPE, null, m.group(1));
        }

        return oldUrl;
    }

    private static void startProxyServer() {
        try {
            WORKER = new SkinWorker();
            new Thread(WORKER).start();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void loadConfig() {
        Gson gson = new Gson();
        File config = new File("config", "skinfixer.json");

        try {
            CONFIG = gson.fromJson(new FileReader(config), SkinConfig.class);
        } catch(IOException ex) {
            CONFIG = new SkinConfig();
        }

        try {
            FileWriter writer = new FileWriter(config);
            writer.write(gson.toJson(CONFIG));
            writer.close();
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    private static String replaceUrl(UrlType type, String uuid, String username) {
        UrlObject urlObject = CONFIG.getFromType(type);
        String url = urlObject.url;

        if((uuid == null && url.contains(UUID)) || (username == null && url.contains(USERNAME))) {
            // Use the proxy server if a variable is missing
            return replaceProxyUrl(type, uuid, username);
        }

        if((type == UrlType.TEXTURES && !urlObject.json) || (type != UrlType.TEXTURES && urlObject.json)) {
            // Use the proxy server if the URL returns what the game does not expect
            // E.g. the game expects a json and the URL returns an image or vice-versa
            return replaceProxyUrl(type, uuid, username);
        }

        // The URL returns what the game expects and all needed variables are provided.
        // No need of a proxy server :D
        return replaceUrlObjects(url, uuid, username);
    }

    private static String replaceUrlObjects(String skinUrl, String uuid, String username) {
        // Replaces the variables
        return skinUrl.replaceAll(UUID, uuid).replaceAll(USERNAME, username);
    }

    private static String replaceProxyUrl(UrlType type, String uuid, String username) {
        // Starts the proxy server if not already
        if(WORKER == null) startProxyServer();

        String valueType, value;
        if(uuid == null) {
            valueType = "name";
            value = username;
        } else if(username == null) {
            valueType = "uuid";
            value = uuid;
        } else {
            valueType = "user";
            value = String.format("%s/%s", uuid, username);
        }

        // Generate an URL leading to the proxy server with the right data
        return String.format("http://127.0.0.1:%d/%s/%s/%s", WORKER.server.getLocalPort(), type.name(), valueType, value);
    }

    private static String queryUUID(String username) throws IOException {
        // Retrieves the UUID from an username
        // Username -> UUID
        URL url = new URL(String.format("https://api.mojang.com/users/profiles/minecraft/%s", username));
        URLConnection con = url.openConnection();

        try {
            JsonElement elem = new JsonParser().parse(new InputStreamReader(con.getInputStream()));
            return elem.getAsJsonObject().get("id").getAsString();
        } catch(Exception ex) {
            // Json validation error
            return null;
        }
    }

    private static String queryUsername(String uuid) throws IOException {
        // Retrieves the username from an UUID
        // UUID -> Username
        URL url = new URL(String.format("https://api.mojang.com/user/profiles/%s/names", uuid));
        URLConnection con = url.openConnection();

        try {
            JsonElement elem = new JsonParser().parse(new InputStreamReader(con.getInputStream()));
            JsonArray array = elem.getAsJsonArray();
            return array.get(array.size() - 1).getAsJsonObject().get("name").getAsString();
        } catch(Exception ex) {
            // Json validation error
            return null;
        }
    }

    private static void downloadAndPipe(String url, UrlType urlType, String uuid, String username, OutputStream out) throws IOException {
        url = replaceUrlObjects(url, uuid, username);
        URL urlObj = new URL(url);
        HttpURLConnection con = (HttpURLConnection)urlObj.openConnection();

        String type = con.getContentType();
        InputStream in = con.getInputStream();

        if(urlType != UrlType.TEXTURES && (type.equals("application/json") || type.startsWith("text"))) {

            // The game expects an image, not a json
            url = CONFIG.getFromJson(new JsonParser().parse(new InputStreamReader(in)), urlType);
            downloadAndPipe(url, urlType, uuid, username, out);
            return;

        }

        // This is exactly what the game expects. Let's give it what it wants :)
        serverPipe(in, type, con.getContentLengthLong(), out);
    }

    private static void serverPipe(InputStream in, String type, long length, OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(out, true);
        writer.println("HTTP/1.0 200");
        writer.println(String.format("Content-type: %s", type));
        writer.println(String.format("Content-length: %d", length));
        writer.println("");

        int n;
        byte[] buffer = new byte[1024];
        while((n = in.read(buffer)) > -1) {
            out.write(buffer, 0, n);
        }
        out.flush();
        out.close();
    }

    private final Pattern url = Pattern.compile("GET /(.*) HTTP");
    protected ServerSocket server;

    private SkinWorker() throws IOException {
        this.server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
    }

    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            Socket socket;
            try {
                socket = server.accept();
            } catch(IOException ex) {
                ex.printStackTrace();
                continue;
            }
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String inputLine;
                Matcher matcher = null;

                while((inputLine = reader.readLine()) != null) {
                    matcher = url.matcher(inputLine);
                    if(matcher.find()) break;
                    matcher = null;
                }

                if(matcher == null) continue;

                String[] data = matcher.group(1).split("/");

                String uuid, username;
                UrlType type = UrlType.valueOf(data[0]);

                if(data[1].equals("name")) {
                    username = data[2];
                    uuid = queryUUID(data[2]);
                } else if(data[1].equals("uuid")) {
                    uuid = data[2];
                    username = queryUsername(data[2]);
                } else if(data[1].equals("user")) {
                    uuid = data[2];
                    username = data[3];
                } else {
                    continue;
                }

                OutputStream out = socket.getOutputStream();

                UrlObject urlObject = CONFIG.getFromType(type);

                if(type == UrlType.TEXTURES) {
                    UrlObject urlSkin = CONFIG.getFromType(UrlType.SKIN);
                    UrlObject urlCape = CONFIG.getFromType(UrlType.CAPE);

                    if(!urlObject.json || !urlObject.url.equals(urlSkin.url) || !urlObject.url.equals(urlCape.url)) {
                        // The game either expects a json and the URL returns an image, or there's custom skin/cape URLs

                        String skinUrl = urlSkin.json ? replaceProxyUrl(UrlType.SKIN, uuid, username) : urlSkin.url;
                        String capeUrl = urlCape.json ? replaceProxyUrl(UrlType.CAPE, uuid, username) : urlCape.url;

                        byte[] json = CONFIG.toJson(uuid, username, skinUrl, capeUrl).toString().getBytes();

                        ByteArrayInputStream in = new ByteArrayInputStream(json);
                        serverPipe(in, "application/json", json.length, out);
                        socket.close();
                        continue;
                    }
                }

                downloadAndPipe(urlObject.url, type, uuid, username, out);
                socket.close();

            } catch(Exception ex) {
                try {
                    // If an exception is thrown, we will just return a 403 error.
                    // Most MC versions should understand it and load the default skin/cape
                    if(socket != null) {
                        socket.getOutputStream().write("HTTP/1.0 403".getBytes());
                        socket.close();
                    }
                } catch(Exception ignored) {}
            }
        }
    }
}
