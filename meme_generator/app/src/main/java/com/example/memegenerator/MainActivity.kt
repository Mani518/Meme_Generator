package com.example.memegenerator

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider


import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.io.FileOutputStream
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemeGeneratorApp()
        }
    }
}

@Composable
fun MemeGeneratorApp() {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var memeText by remember { mutableStateOf("When Kotlin hits different!") }
    var textSize by remember { mutableStateOf(24f) }
    var textColor by remember { mutableStateOf(ComposeColor.White) }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var textPositionX by remember { mutableStateOf(50f) }
    var textPositionY by remember { mutableStateOf(50f) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spokenText.isNullOrEmpty()) memeText = spokenText
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(ComposeColor.Gray)
        ) {
            val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            textPositionX = (textPositionX + dragAmount.x).coerceIn(0f, maxWidthPx - 50f)
                            textPositionY = (textPositionY + dragAmount.y).coerceIn(0f, maxHeightPx - 50f)
                        }
                    }
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        imageUri ?: Uri.parse("https://via.placeholder.com/300x300.png?text=Default+Meme")
                    ),
                    contentDescription = "Meme Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Text(
                    text = memeText,
                    fontSize = textSize.sp,
                    color = textColor,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None,
                    modifier = Modifier.offset { IntOffset(textPositionX.toInt(), textPositionY.toInt()) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = memeText,
            onValueChange = { memeText = it },
            label = { Text("Enter Meme Text") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = textSize,
                onValueChange = { textSize = it },
                valueRange = 12f..72f,
                modifier = Modifier.weight(1f)
            )
            Text("${textSize.toInt()}sp", modifier = Modifier.align(Alignment.CenterVertically))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(
                ComposeColor.White, ComposeColor.Black,
                ComposeColor.Red, ComposeColor.Yellow,
                ComposeColor.Blue, ComposeColor.Green, ComposeColor.Magenta
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color)
                        .clickable { textColor = color }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { isBold = !isBold }) { Text("Bold") }
            Button(onClick = { isItalic = !isItalic }) { Text("Italic") }
            Button(onClick = { isUnderline = !isUnderline }) { Text("Underline") }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { imagePickerLauncher.launch("image/*") }) { Text("Pick Image") }
            Button(onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                speechRecognizerLauncher.launch(intent)
            }) {
                Text("Speech-to-Text")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                val bgBitmap = imageUri?.let {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } ?: Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)

                val bitmap = bgBitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(bitmap)
                val paint = Paint().apply {
                    color = when (textColor) {
                        ComposeColor.White -> Color.WHITE
                        ComposeColor.Black -> Color.BLACK
                        ComposeColor.Red -> Color.RED
                        ComposeColor.Yellow -> Color.YELLOW
                        ComposeColor.Blue -> Color.BLUE
                        ComposeColor.Green -> Color.GREEN
                        ComposeColor.Magenta -> Color.MAGENTA
                        else -> Color.WHITE
                    }
                    textSize = this@apply.textSize * 2
                    isFakeBoldText = isBold
                    isUnderlineText = isUnderline
                    typeface = if (isItalic) Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) else Typeface.DEFAULT
                }
                canvas.drawText(memeText, textPositionX, textPositionY + 50, paint)
                generatedBitmap = bitmap

                val file = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "meme_${System.currentTimeMillis()}.png"
                )
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                Toast.makeText(context, "Meme Saved!", Toast.LENGTH_SHORT).show()
            }) { Text("Save Meme") }

            Button(onClick = {
                generatedBitmap?.let { bitmap ->
                    val file = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        "meme_${System.currentTimeMillis()}.png"
                    )
                    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Meme"))
                } ?: Toast.makeText(context, "Save the meme first!", Toast.LENGTH_SHORT).show()
            }) { Text("Share") }
        }
    }
}
