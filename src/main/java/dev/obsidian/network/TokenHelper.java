package dev.obsidian.network;

import dev.obsidian.client.util.LoggerHelper;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Enumeration;

/**
 * Ultra-Compact Binary Session Token Helper for Obsidian Messenger with 16-bit CRC Corruption Checksum.
 */
public class TokenHelper {

    public static class SessionTokenData {
        public final String publicIp;
        public final String lanIp;
        public final int port;
        public final long expiresAt;
        public final int maxPlayers;
        public final String hostPubKey;

        public SessionTokenData(String publicIp, String lanIp, int port, long expiresAt, int maxPlayers, String hostPubKey) {
            this.publicIp = publicIp;
            this.lanIp = lanIp;
            this.port = port;
            this.expiresAt = expiresAt;
            this.maxPlayers = maxPlayers;
            this.hostPubKey = hostPubKey;
        }
    }

    public static String generateToken(String publicIp, String lanIp, int port, int durationHours, int maxPlayers, String hostPubKeyBase64) {
        try {
            byte[] pubBytes = parseIpToBytes(publicIp);
            byte[] lanBytes = parseIpToBytes(lanIp);
            byte[] keyBytes = Base64.getDecoder().decode(hostPubKeyBase64);

            long expiresAt = System.currentTimeMillis() + ((long) durationHours * 3600 * 1000);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.write(pubBytes);
            dos.write(lanBytes);
            dos.writeShort(port);
            dos.writeLong(expiresAt);
            dos.writeByte(maxPlayers);
            dos.writeShort(keyBytes.length);
            dos.write(keyBytes);
            dos.flush();

            byte[] payload = baos.toByteArray();
            int crc = calculateCRC16(payload);

            ByteArrayOutputStream finalBaos = new ByteArrayOutputStream();
            DataOutputStream finalDos = new DataOutputStream(finalBaos);
            finalDos.writeShort(crc);
            finalDos.write(payload);
            finalDos.flush();

            String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(finalBaos.toByteArray());
            return "OM-" + b64;
        } catch (Exception e) {
            LoggerHelper.error("TokenHelper", "Token generation failed: " + e.getMessage());
            return "";
        }
    }

    public static SessionTokenData parseToken(String tokenStr) {
        if (tokenStr == null || !tokenStr.startsWith("OM-")) {
            throw new IllegalArgumentException("Invalid token prefix (must start with OM-)");
        }
        try {
            String b64 = tokenStr.substring(3);
            byte[] tokenBytes = Base64.getUrlDecoder().decode(b64);

            ByteArrayInputStream bais = new ByteArrayInputStream(tokenBytes);
            DataInputStream dis = new DataInputStream(bais);

            int expectedCrc = dis.readUnsignedShort();
            byte[] payload = new byte[tokenBytes.length - 2];
            dis.readFully(payload);

            int actualCrc = calculateCRC16(payload);
            if (expectedCrc != actualCrc) {
                throw new SecurityException("Token CRC corruption check failed! Expected=" + expectedCrc + ", Actual=" + actualCrc);
            }

            ByteArrayInputStream payloadBais = new ByteArrayInputStream(payload);
            DataInputStream payloadDis = new DataInputStream(payloadBais);

            byte[] pubBytes = new byte[4];
            payloadDis.readFully(pubBytes);
            String publicIp = InetAddress.getByAddress(pubBytes).getHostAddress();

            byte[] lanBytes = new byte[4];
            payloadDis.readFully(lanBytes);
            String lanIp = InetAddress.getByAddress(lanBytes).getHostAddress();

            int port = payloadDis.readUnsignedShort();
            long expiresAt = payloadDis.readLong();
            int maxPlayers = payloadDis.readUnsignedByte();

            int keyLen = payloadDis.readUnsignedShort();
            byte[] keyBytes = new byte[keyLen];
            payloadDis.readFully(keyBytes);
            String hostPubKey = Base64.getEncoder().encodeToString(keyBytes);

            if (System.currentTimeMillis() > expiresAt) {
                throw new IllegalStateException("Token expired");
            }

            return new SessionTokenData(publicIp, lanIp, port, expiresAt, maxPlayers, hostPubKey);
        } catch (IllegalArgumentException | SecurityException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed token: " + e.getMessage());
        }
    }

    public static int calculateCRC16(byte[] bytes) {
        int crc = 0xFFFF;
        for (byte b : bytes) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc = (crc >> 1);
                }
            }
        }
        return crc & 0xFFFF;
    }

    private static byte[] parseIpToBytes(String ip) throws UnknownHostException {
        return InetAddress.getByName(ip).getAddress();
    }

    public static String getLocalLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }
}
