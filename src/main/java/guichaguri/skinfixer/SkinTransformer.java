package guichaguri.skinfixer;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Guilherme Chaguri
 */
public class SkinTransformer implements IClassTransformer {

    // Not including protocols or anything that could change between versions.
    // Only what should match the possible URLs
    private final String AMAZONAWS_BASE = "s3.amazonaws.com/Minecraft";
    private final String MINECRAFT_BASE = "skins.minecraft.net/Minecraft";
    private final String PROFILE_BASE = "sessionserver.mojang.com/session/minecraft/profile";

    private final byte[] AMAZONAWS_BYTES = AMAZONAWS_BASE.getBytes();
    private final byte[] MINECRAFT_BYTES = MINECRAFT_BASE.getBytes();
    private final byte[] PROFILE_BYTES = PROFILE_BASE.getBytes();

    public byte[] transform(String name, String transformedName, byte[] bytes) {

        if(contains(bytes, AMAZONAWS_BYTES) || contains(bytes, MINECRAFT_BYTES) || contains(bytes, PROFILE_BYTES)) {
            return patch(bytes);
        }

        return bytes;
    }

    private boolean contains(byte[] bytes, byte[] needle) {
        int i = 0;
        for(byte b : bytes) {
            if(b == needle[i]) {
                i++;
                if(i >= needle.length) {
                    return true;
                }
            }
        }
        return false;
    }

    private byte[] patch(byte[] bytes) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, ClassReader.SKIP_FRAMES);

        boolean patch = false;

        for(MethodNode method : classNode.methods) {

            if(patchNodes(method)) patch = true;

        }

        if(!patch) return bytes;

        System.out.println("Patched a skin fix in " + classNode.name);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private boolean patchNodes(MethodNode method) {
        InsnList list = method.instructions;
        boolean patched = false;
        boolean foundUrl = false;

        for(int i = 0; i < list.size(); i++) {
            AbstractInsnNode node = list.get(i);

            if(node instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode)node;

                if(ldc.cst instanceof String && isUrl((String)ldc.cst)) {
                    foundUrl = true;
                }

            } else if(foundUrl && (node.getOpcode() == Opcodes.PUTFIELD || node.getOpcode() == Opcodes.ASTORE)) {

                list.insertBefore(node, new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "guichaguri/skinfixer/SkinWorker", "getURL", "(Ljava/lang/String;)Ljava/lang/String;", false));
                foundUrl = false;
                patched = true;

            }

        }
        return patched;
    }

    private boolean isUrl(String url) {
        return url.contains(AMAZONAWS_BASE) || url.contains(MINECRAFT_BASE) || url.contains(PROFILE_BASE);
    }

}
