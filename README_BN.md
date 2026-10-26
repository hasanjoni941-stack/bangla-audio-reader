# বাংলা অডিও রিডার — Android প্রোটোটাইপ

এটি Kotlin + Jetpack Compose ভিত্তিক একটি প্রাথমিক Android প্রজেক্ট।

## বর্তমানে অন্তর্ভুক্ত
- PDF ফাইল পিকার
- PDF থেকে টেক্সট বের করার চেষ্টা (PDFBox-Android)
- ফোনের Android Text-to-Speech দিয়ে নির্বাচিত টেক্সট পড়ানো
- একাধিক ছবি থেকে PDF তৈরি
- আধুনিক ডার্ক-থিম প্রাথমিক UI

## এখনও সম্পূর্ণ নয়
- দীর্ঘ PDF-এর জন্য নির্ভরযোগ্য chunk-based reading ও অগ্রগতি
- স্ক্যান করা PDF-এর OCR
- ইংরেজি থেকে বাংলা অনুবাদ
- উচ্চমানের Neural বাংলা কণ্ঠ/অনলাইন TTS
- MP3 export, background playback, bookmarks, robust error handling
- 50 MB PDF-এর বিভিন্ন ডিভাইসে পরীক্ষা

## বিল্ডের প্রয়োজন
- Android Studio (ডেস্কটপ) অথবা Android Gradle Plugin 8.7.3 / Gradle 8.9 / JDK 17 সমর্থন করে এমন বিল্ড পরিবেশ
- Android SDK Platform 35
- প্রথম বিল্ডে ইন্টারনেট দরকার হবে Gradle dependency ডাউনলোডের জন্য

## প্রাথমিক সীমাবদ্ধতা
- TTS চালু আছে কি না এবং বাংলা voice সমর্থিত কি না, তা ফোনের TTS engine-এর ওপর নির্ভর করে।
- এই প্রোটোটাইপে একবারে সর্বোচ্চ 3000 অক্ষর পড়ানো হয়েছে।
- ছবি থেকে PDF বানানোর সময় ছবিগুলো 1000 পিক্সেল প্রস্থে সীমিত করা হয় যাতে RAM কম লাগে।
- স্ক্যান করা PDF থেকে টেক্সট বের করতে OCR নেই, তাই খালি ফল আসতে পারে।
- 50 MB PDF সমর্থন লক্ষ্য; সব PDF/ফোনে নিশ্চিত করা হয়নি।

## প্রজেক্টের মূল ফাইল
`app/src/main/java/com/example/banglaaudioreader/MainActivity.kt`
