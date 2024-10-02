package com.github.tr303.autosave;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AutoSaveData {
    private final Project project;
    private final Logger log = Logger.getInstance(AutoSaveData.class);

    public AutoSaveData(Project project) {
        this.project = project;
    }

    // 得到VERSIONS文件的内容
    String getVersionsFileContent() {
        String autosaveDirPath = project.getBasePath() + "/.autosave";
        VirtualFile autosaveDir = VfsUtil.findFileByIoFile(new File(autosaveDirPath), true);

        if (autosaveDir != null) {
            VirtualFile versionsFile = autosaveDir.findChild("VERSIONS");
            if (versionsFile != null) try {
                return VfsUtil.loadText(versionsFile);
            } catch (IOException e) {
                log.error(e);
            }
        }

        return null;
    }

    // 得到某个哈希值对应object的原始内容String
    String getObjectContentByHash(String hash) {
        String objectDir = project.getBasePath() + "/.autosave/objects/" + hash.substring(0, 2);
        String objectFileName = hash.substring(2);
        String objectFullPath = objectDir + '/' + objectFileName;

        VirtualFile objectFile = VfsUtil.findFileByIoFile(new File(objectFullPath), true);
        if (objectFile != null) try {
            return VfsUtil.loadText(objectFile);
        } catch (IOException e) {
            log.error(e);
        }

        return null;
    }

    void saveObjectWithHash(String content, String hash) {
        String objectDir = project.getBasePath() + "/.autosave/objects/" + hash.substring(0, 2);
        String objectFileName = hash.substring(2);

        try {
            VirtualFile objectDirFile = VfsUtil.createDirectoryIfMissing(objectDir);

            if (objectDirFile != null && objectDirFile.findChild(objectFileName) == null) {
                VirtualFile objectFile = objectDirFile.createChildData(this, objectFileName);
                objectFile.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            log.error(e);
        }
    }

    // 判断原始内容String描述的是目录还是文件
    Boolean isDirectory(String content) {
        if (content.startsWith("FIL\0")) return false;
        if (content.startsWith("DIR\0")) return true;
        return null;
    }

    // 添加FIL或DIR前缀
    String addPrefix(String content, boolean isDirectory) {
        if (isDirectory) return "DIR\0" + content;
        else return "FIL\0" + content;
    }

    // SHA256一个String
    String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(input.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString(); // 返回SHA-256哈希值
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
        }
        return null;
    }
}

class ReferenceCounter {

}