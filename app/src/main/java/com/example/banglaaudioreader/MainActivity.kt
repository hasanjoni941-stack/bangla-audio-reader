package com.example.banglaaudioreader

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        setContent { BanglaAudioApp() }
    }
}

private val Ink = Color(0xFF101426)
private val Purple = Color(0xFF7C4DFF)
private val Cyan = Color(0xFF42D9C8)

@Composable
fun BanglaAudioApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPdf by remember { mutableStateOf<Uri?>(null) }
    var pdfName by remember { mutableStateOf("এখনো কোনো PDF নির্বাচন করা হয়নি") }
    var extractedText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("শুরু করতে একটি PDF নির্বাচন করুন") }
    var busy by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var imageCount by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
            if (result == TextToSpeech.SUCCESS) {
                val languageResult = tts?.setLanguage(Locale("bn", "BD"))
                // TTS can still be usable if Bengali BD is not supported; user can install a voice in settings.
                status = if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                    languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    "ফোনে বাংলা TTS কণ্ঠ নাও থাকতে পারে। Settings থেকে বাংলা voice ইনস্টল করে দেখুন।"
                } else {
                    "প্রস্তুত — PDF নির্বাচন করুন"
                }
            }
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            tts = null
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedPdf = uri
            pdfName = uri.lastPathSegment?.substringAfterLast('/') ?: "নির্বাচিত PDF"
            extractedText = ""
            status = "PDF পাওয়া গেছে। লেখা পড়ার জন্য প্রস্তুত।"
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            busy = true
            imageCount = uris.size
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    createPdfFromImages(context.contentResolver, uris)
                }
                busy = false
                if (result != null) {
                    selectedPdf = result
                    pdfName = "ছবি থেকে তৈরি PDF (${uris.size}টি ছবি)"
                    extractedText = ""
                    status = "PDF তৈরি হয়েছে। এখন লেখা পড়ার চেষ্টা করতে পারেন।"
                } else {
                    status = "ছবি থেকে PDF তৈরি করা যায়নি। অন্য ছবি দিয়ে চেষ্টা করুন।"
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Purple,
            secondary = Cyan,
            background = Ink,
            surface = Color(0xFF1A2035),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF282052), Ink, Color(0xFF0C2531))))
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Spacer(Modifier.height(22.dp))
                        Text("শুনুন", color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("বাংলা অডিও রিডার", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("PDF পড়ুন নয়, শুনুন।", color = Color(0xFFCFD4E8), fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                    }
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2035)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("আপনার লাইব্রেরি", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(pdfName, color = Color(0xFFCFD4E8), fontSize = 13.sp)
                                Button(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("＋  PDF নির্বাচন করুন")
                                }
                                OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                                    Text("▧  ছবি থেকে PDF তৈরি")
                                }
                                Text("একসঙ্গে একাধিক ছবি বেছে নিয়ে একটি PDF তৈরি করা যাবে।", color = Color(0xFF9EA8C6), fontSize = 12.sp)
                            }
                        }
                    }
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2035))
                        ) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("অডিও প্লেয়ার", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Box(
                                    Modifier.fillMaxWidth().height(72.dp).background(
                                        Brush.horizontalGradient(listOf(Color(0xFF43308B), Color(0xFF176D78))),
                                        RoundedCornerShape(16.dp)
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("〰  ♪  〰  ♫  〰  ♪  〰", fontSize = 22.sp, color = Color.White)
                                }
                                Text(status, color = Color(0xFFCFD4E8), fontSize = 13.sp)
                                if (busy) CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Cyan)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = {
                                            if (extractedText.isNotBlank() && ttsReady) {
                                                tts?.speak(extractedText.take(3000), TextToSpeech.QUEUE_FLUSH, null, "pdf-audio")
                                                speaking = true
                                                status = "অডিও চলছে। এই প্রাথমিক সংস্করণে প্রথম ৩,০০০ অক্ষর পড়া হবে।"
                                            } else {
                                                status = "আগে PDF নির্বাচন করে ‘PDF-এর লেখা বের করুন’ চাপুন।"
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("▶  শুনুন") }
                                    OutlinedButton(onClick = {
                                        tts?.stop()
                                        speaking = false
                                        status = "অডিও থামানো হয়েছে"
                                    }) { Text("■ থামুন") }
                                }
                                OutlinedButton(
                                    onClick = {
                                        val uri = selectedPdf
                                        if (uri == null) {
                                            status = "আগে একটি PDF নির্বাচন করুন।"
                                        } else {
                                            busy = true
                                            status = "PDF থেকে লেখা বের করা হচ্ছে…"
                                            scope.launch {
                                                val text = withContext(Dispatchers.IO) {
                                                    extractPdfText(context.contentResolver, uri)
                                                }
                                                busy = false
                                                extractedText = text.orEmpty()
                                                status = when {
                                                    text == null -> "PDF পড়া যায়নি। ফাইলটি সুরক্ষিত বা ত্রুটিপূর্ণ হতে পারে।"
                                                    text.isBlank() -> "লেখা পাওয়া যায়নি। এটি স্ক্যান করা PDF হতে পারে; OCR পরে যোগ করব।"
                                                    else -> "লেখা পাওয়া গেছে (${text.length} অক্ষর)। শুনতে ‘শুনুন’ চাপুন।"
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("PDF-এর লেখা বের করুন") }
                            }
                        }
                    }
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2035))
                        ) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("পরবর্তী সংস্করণে", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                FeatureLine("স্বাভাবিক Neural বাংলা কণ্ঠ")
                                FeatureLine("৫০ MB পর্যন্ত PDF পরীক্ষা ও অপ্টিমাইজেশন")
                                FeatureLine("স্ক্যান PDF-এর OCR ও ইংরেজি→বাংলা অনুবাদ")
                                FeatureLine("অডিও সংরক্ষণ, বুকমার্ক ও ব্যাকগ্রাউন্ড প্লে")
                                Text("এগুলো এখনো সম্পূর্ণ করা হয়নি; ধাপে ধাপে যোগ হবে।", color = Color(0xFF9EA8C6), fontSize = 12.sp)
                            }
                        }
                    }
                    item {
                        Text("প্রোটোটাইপ 0.1 • বিনামূল্যের প্রাথমিক ভিত্তি", color = Color(0xFF8792B0), fontSize = 11.sp, modifier = Modifier.padding(bottom = 22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("✦", color = Cyan)
        Text(text, color = Color(0xFFCFD4E8), fontSize = 14.sp)
    }
}

private fun extractPdfText(resolver: ContentResolver, uri: Uri): String? {
    return try {
        resolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                PDFTextStripper().getText(document)
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun createPdfFromImages(resolver: ContentResolver, uris: List<Uri>): Uri? {
    val document = PdfDocument()
    return try {
        uris.forEachIndexed { index, uri ->
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: throw IOException("Could not decode image")
            val maxWidth = 1000
            val ratio = maxWidth.toFloat() / bitmap.width.coerceAtLeast(1)
            val width = if (bitmap.width > maxWidth) maxWidth else bitmap.width
            val height = (bitmap.height * (if (bitmap.width > maxWidth) ratio else 1f)).toInt().coerceAtLeast(1)
            val scaled = if (width != bitmap.width || height != bitmap.height) {
                android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)
            } else bitmap
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, index + 1).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawBitmap(scaled, 0f, 0f, null)
            document.finishPage(page)
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "BanglaAudio_images_${System.currentTimeMillis()}.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/BanglaAudioReader")
        }
        val outputUri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
            ?: throw IOException("Could not create output file")
        resolver.openOutputStream(outputUri)?.use { document.writeTo(it) }
            ?: throw IOException("Could not write output file")
        outputUri
    } catch (_: Exception) {
        null
    } finally {
        document.close()
    }
}
