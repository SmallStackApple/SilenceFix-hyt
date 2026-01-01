package net.minecraft.client.resources;

import com.google.common.collect.Sets;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class FolderResourcePack
extends AbstractResourcePack {
    public FolderResourcePack(File resourcePackFileIn) {
        super(resourcePackFileIn);
    }

    @Override
    protected InputStream getInputStreamByName(String name) throws IOException {
        return new BufferedInputStream(new FileInputStream(new File(this.resourcePackFile, name)));
    }

    @Override
    protected boolean hasResourceName(String name) {
        return new File(this.resourcePackFile, name).isFile();
    }

    @Override
    public Set<String> getResourceDomains() {
        HashSet set = Sets.newHashSet();
        File file1 = new File(this.resourcePackFile, "assets/");
        if (file1.isDirectory()) {
            for (File file2 : file1.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY)) {
                String s2 = FolderResourcePack.getRelativeName(file1, file2);
                if (!s2.equals(s2.toLowerCase())) {
                    this.logNameNotLowercase(s2);
                    continue;
                }
                set.add(s2.substring(0, s2.length() - 1));
            }
        }
        return set;
    }
}

