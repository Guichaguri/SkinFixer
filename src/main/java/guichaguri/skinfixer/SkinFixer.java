package guichaguri.skinfixer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * @author Guilherme Chaguri
 */
public class SkinFixer implements ITweaker {
    private List<String> args;

    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.args = new ArrayList<String>(args);
        this.args.add("--version");
        this.args.add(profile);
        if(assetsDir != null) {
            this.args.add("--assetsDir");
            this.args.add(assetsDir.getAbsolutePath());
        }
        if(gameDir != null) {
            this.args.add("--gameDir");
            this.args.add(gameDir.getAbsolutePath());
        }
    }

    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        classLoader.registerTransformer("guichaguri.skinfixer.SkinTransformer");
    }

    public String getLaunchTarget() {
        // A little hack to make it work with older versions
        if(getClass().getResource("net/minecraft/client/main/Main.class") != null) {
            return "net.minecraft.client.main.Main";
        }
        return "net.minecraft.client.Minecraft";
    }

    public String[] getLaunchArguments() {
        ArrayList args = (ArrayList)Launch.blackboard.get("ArgumentList");
        if(args.isEmpty()) args.addAll(this.args);

        this.args = null;

        return new String[0];
    }
}
