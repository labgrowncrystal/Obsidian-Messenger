# 🛡️ Obsidian Messenger (OM)

**Zero-Server E2EE In-Game P2P Messenger GUI for Minecraft.**

Obsidian Messenger (OM) enables private, encrypted peer-to-peer chat and group messaging directly inside Minecraft via a custom In-Game GUI (Screen), with zero central web servers or server-side plugins required.

---

## 🔒 Security & Privacy Architecture

- 🛡️ **100% Zero-Server P2P Architecture** — Direct P2P TCP connections between players. No middleman or rendezvous server tracking online status or contacts.
- 🔐 **ECDH Key Agreement & AES-256-GCM** — Ephemeral Elliptic Curve Diffie-Hellman (`secp256r1`) key exchange over TCP, followed by AES-256-GCM authenticated encryption.
- 🗝️ **Master-Passphrase Encrypted Vault (`PBKDF2WithHmacSHA256`)** — Local chat history (`chat_history.enc`) and contact books (`contacts.enc`) are encrypted on disk via PBKDF2 (100,000 iterations).
- 🔑 **Host Public Key Pinning** — MitM attack protection via host key verification during handshake.
- 🙈 **Universal Regex IP & Token Masking** — Centralized privacy log redaction and log rotation.

---

## 🏗️ Building from Source

Build locally with Gradle:
```bash
./gradlew build
```
Compiled JAR artifacts will be located under `build/libs/`.

---

## 📄 License

Licensed under the [MIT License](LICENSE).
