# Kushwaha Cafe - Hostinger VPS Deployment Guide (होस्टिंगर VPS डिप्लॉयमेंट गाइड)

यह गाइड आपको अपनी स्प्रिंग बूट कैफ़े वेबसाइट को **Hostinger VPS** पर डिप्लॉय (deploy) करने के लिए आसान चरण-दर-चरण निर्देश देती है।

---

## 🚀 1. Run & Build Locally (लोकल मशीन पर चलाएं और बिल्ड करें)

### Local Run (लोकल में रन करें):
प्रोजेक्ट डायरेक्टरी में टर्मिनल/कमांड प्रॉम्ट (CMD) खोलें और चलाएं:
```bash
mvnw.cmd spring-boot:run
```
यह पोर्ट `8080` पर शुरू होगा और आपके कंप्यूटर पर SQLite (`cafe.db`) का उपयोग करेगा।

### Pack to JAR (JAR फ़ाइल तैयार करें):
डिप्लॉय करने से पहले, आपको एक सिंगल निष्पादन योग्य (executable) JAR फ़ाइल बनानी होगी:
```bash
mvnw.cmd clean package -DskipTests
```
बिल्ड होने के बाद, आपकी फ़ाइल `target/cafe-0.0.1-SNAPSHOT.jar` नाम से सेव हो जाएगी। डिप्लॉयमेंट के लिए हमें इसी फ़ाइल की ज़रूरत होगी।

---

## ☁️ 2. Deploy on Hostinger VPS (होस्टिंगर VPS पर डिप्लॉयमेंट)

*स्प्रिंग बूट वेबसाइट चलाने के लिए Java की ज़रूरत होती है, इसलिए इसे बेसिक Shared Hosting (cPanel) पर सीधे नहीं चलाया जा सकता। इसके लिए आपको **Hostinger KVM VPS** का उपयोग करना होगा (Ubuntu 22.04 LTS OS अनुशंसित है)।*

### Step 1: Install Java 17 on Hostinger VPS (VPS पर Java इंस्टॉल करें)
1. अपने कंप्यूटर पर Terminal (Mac/Linux) या PuTTY/CMD (Windows) खोलें।
2. अपने Hostinger VPS से SSH के जरिए कनेक्ट करें (IP और पासवर्ड Hostinger Panel में मिल जाएगा):
   ```bash
   ssh root@your_vps_ip
   ```
3. VPS में Java 17 इंस्टॉल करने के लिए नीचे दिए गए कमांड्स चलाएं:
   ```bash
   sudo apt update
   sudo apt install openjdk-17-jre-headless -y
   ```
4. जांचें कि Java इंस्टॉल हो गया है:
   ```bash
   java -version
   ```

### Step 2: Upload JAR File to VPS (JAR फ़ाइल अपलोड करें)
1. FileZilla या WinSCP जैसे SFTP क्लाइंट को डाउनलोड करें और खोलें।
2. Hostinger VPS IP, Username (`root`), Port (`22`), और VPS Password डालकर कनेक्ट करें।
3. VPS में `/var/www/` डायरेक्टरी के अंदर `cafe` नाम का एक नया फोल्डर बनाएं (`/var/www/cafe/`)।
4. अपनी लोकल मशीन से `target/cafe-0.0.1-SNAPSHOT.jar` को ड्रैग करके VPS के `/var/www/cafe/` फोल्डर में अपलोड कर दें।

### Step 3: Run 24/7 in Background using Systemd (बैकग्राउंड सर्विस बनाएं)
वेबसाइट बिना बंद हुए 24 घंटे चलती रहे, इसके लिए एक सिस्टम सर्विस बनाएं:
1. SSH टर्मिनल में नीचे दिया गया कमांड चलाएं:
   ```bash
   sudo nano /etc/systemd/system/cafe.service
   ```
2. एडिटर में नीचे दिया गया कोड पेस्ट करें (ईमेल सेटिंग्स को अपने अनुसार बदलें):
   ```ini
   [Unit]
   Description=Kushwaha Cafe Spring Boot App
   After=network.target

   [Service]
   User=root
   WorkingDirectory=/var/www/cafe
   ExecStart=/usr/bin/java -jar cafe-0.0.1-SNAPSHOT.jar
   SuccessExitStatus=143
   Restart=always
   RestartSec=10
   # Environment variables
   Environment=PORT=8080
   Environment=SMTP_EMAIL=your_email@gmail.com
   Environment=SMTP_PASSWORD="your_gmail_app_password"

   [Install]
   WantedBy=multi-user.target
   ```
3. सेव करने के लिए: `Ctrl+O` दबाएं, फिर `Enter` करें। बाहर निकलने के लिए `Ctrl+X` दबाएं।
4. सर्विस को रीलोड और स्टार्ट करें:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl start cafe
   sudo systemctl enable cafe
   ```
5. स्टेटस चेक करें कि सर्विस रनिंग है या नहीं:
   ```bash
   sudo systemctl status cafe
   ```

### Step 4: Map Domain using Nginx Reverse Proxy (Nginx सेटअप करें)
अगर आप चाहते हैं कि वेबसाइट आईपी (जैसे `http://12.34.56.78:8080`) के बजाय सीधे आपके डोमेन (जैसे `kushwahacafe.com`) पर खुले, तो Nginx सेटअप करें:
1. Nginx इंस्टॉल करें:
   ```bash
   sudo apt install nginx -y
   ```
2. डिफ़ॉल्ट कॉन्फ़िगरेशन फ़ाइल खोलें:
   ```bash
   sudo nano /etc/nginx/sites-available/default
   ```
3. `location /` ब्लॉक को ढूंढें और उसे नीचे दिए गए कोड से बदल दें:
   ```nginx
   location / {
       proxy_pass http://127.0.0.1:8080;
       proxy_set_header Host $host;
       proxy_set_header X-Real-IP $remote_addr;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;
   }
   ```
4. कॉन्फ़िगरेशन टेस्ट करें और Nginx को रीस्टार्ट करें:
   ```bash
   sudo nginx -t
   sudo systemctl restart nginx
   ```

**बधाई हो!** अब आपकी Kushwaha Cafe वेबसाइट Hostinger VPS पर डिप्लॉय हो चुकी है और आपके डोमेन पर लाइव है। 🎉
