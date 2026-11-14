# GitHub গাইড (বাংলা)

## ১. GitHub এ project তোলা (একবারই)
1. github.com এ লগইন → ডান উপরে **+ → New repository** → নাম `life-easy` → **Public** সিলেক্ট (Check for update ও সহজ download এর জন্য) → **Create repository**।
2. zip ফাইল (`LIFE-EASY-android-project.zip`) extract করো।
3. repo পেজে **uploading an existing file** লিংকে চাপো → extract করা ফোল্ডারের **সব ফাইল ও ফোল্ডার** (`.github` ও `ci` ফোল্ডারসহ) drag করে ছাড়ো → নিচে **Commit changes**।
   - `.github` ফোল্ডার না গেলে git দিয়ে দাও: `git init && git add . && git commit -m first && git branch -M main && git remote add origin https://github.com/USERNAME/life-easy.git && git push -u origin main`

## ২. একবার সেটিং (১ মিনিট)
**Settings → Actions → General → Workflow permissions → Read and write permissions → Save।**

## ৩. APK বানানো (২-৩ ক্লিক)
**Actions → Build APK and publish release → Run workflow → Run workflow।**
৫-১০ মিনিট পর সবুজ ✓ আসবে।

## ৪. APK download
Repo পেজের ডানে **Releases → সর্বশেষ release → LIFE-EASY.apk** চাপো।
সবসময়ের সরাসরি লিংক: `https://github.com/USERNAME/life-easy/releases/latest/download/LIFE-EASY.apk`
ফোনে install করার সময় "Install unknown apps" অনুমতি দাও।

## ৫. Update দেওয়া
কোড বদলে GitHub এ upload করলেই (বা আবার Run workflow দিলে) নতুন version নিজে নিজে তৈরি হয়। ফোনে নতুন APK install করলে পুরনোটার উপরেই update হয়। অ্যাপের ভিতরে **Persona ট্যাব → Check for update** চাপলেও নতুন version থাকলে download খুলবে।

## ৬. AI চালু করতে (প্রতিটি ইউজার নিজে)
অ্যাপে কোনো AI key দেওয়া নেই। ইউজার অ্যাপ খুলে **Persona ট্যাব → AI সেটিং** এ নিজের ফ্রি Gemini API key (aistudio.google.com/apikey) paste করে **সংরক্ষণ ও যাচাই** চাপবে। GitHub এ কোনো secret দিতে হবে না।

## ৬ক. "ক্ষতিকর অ্যাপ" / Play Protect সতর্কতা
GitHub থেকে নামানো APK ইনস্টলের সময় Google Play Protect সতর্ক করতে পারে ("অজানা ডেভেলপার" বা "অ্যাপ স্ক্যান করুন")। এটা ভাইরাস পাওয়া গেছে বলে নয়, শুধু অ্যাপটা Play Store থেকে আসেনি বলে। 
- ইউজারকে বলুন: **More details → Install anyway**, অথবা **Scan app** চাপুন।
- পুরোপুরি সতর্কতা ছাড়া ইনস্টল শুধু **Google Play Store** (Internal/Closed testing লিংকসহ) থেকেই হয়। ওটাই স্থায়ী সমাধান।
- সতর্কতা কমাতে: সবসময় একই signing key ব্যবহার করুন (key বদলালে আবার সতর্কতা আসে), অকারণ permission রাখবেন না (এই অ্যাপে শুধু Internet ও Microphone)।
- `ci/apk-signing.jks` পাবলিক টেস্ট key। বেশি মানুষকে দেওয়ার আগে নিজের গোপন key বানিয়ে (নিচের Play Store গাইড দেখুন) secrets এ দিন, নইলে অন্য কেউ একই key দিয়ে নকল APK বানাতে পারে। key বদলালে ইউজারকে একবার পুরনো অ্যাপ মুছে নতুন ইনস্টল করতে হবে।

## ৭. Play Store এর জন্য (পরে)
`GITHUB_AND_PLAY_STORE_GUIDE.md` এ আসল upload key বানানো ও secrets (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) দেওয়ার নিয়ম আছে। ওই secrets দিলে একই workflow `LIFE-EASY.aab` ও বানাবে।
**সতর্কতা:** ci ফোল্ডারের test key শুধু GitHub APK এর জন্য, Play Store এ ব্যবহার করো না। Play Store এর AAB এ "Check for update" বাটন থাকে না (Play এর নিয়মের জন্য)।
