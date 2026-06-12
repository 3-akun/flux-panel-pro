package com.admin.common.utils;

import cn.hutool.crypto.digest.BCrypt;

/**
 * 密码工具：新密码使用 BCrypt，兼容历史 MD5 并在登录时自动升级。
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        if (isBcrypt(storedHash)) {
            return BCrypt.checkpw(rawPassword, storedHash);
        }
        return storedHash.equals(Md5Util.md5(rawPassword));
    }

    public static boolean isBcrypt(String storedHash) {
        return storedHash != null && storedHash.startsWith("$2");
    }

    /**
     * 若仍为 MD5 且验证通过，返回 BCrypt 哈希供升级；否则返回 null。
     */
    public static String upgradeHashIfLegacy(String rawPassword, String storedHash) {
        if (isBcrypt(storedHash) || !matches(rawPassword, storedHash)) {
            return null;
        }
        return hash(rawPassword);
    }
}
