import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.io.File;
public class Identity {
    public static void main(String[] a) throws Exception {
        DexPool.writeTo(a[1], DexFileFactory.loadDexFile(new File(a[0]), a.length > 2 ? null : Opcodes.forApi(30)));
    }
}
