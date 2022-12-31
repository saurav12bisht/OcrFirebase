package com.example.ocrml

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.example.ocrml.databinding.ActivityMainBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var mFunctions: FirebaseFunctions
    lateinit var mAuth: FirebaseAuth
    private val STORAGE_PERMISSION_CODE = 101

    lateinit var photoUri: Uri
    private lateinit var functions: FirebaseFunctions
    lateinit var imageBitmap: Bitmap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mAuth = FirebaseAuth.getInstance()
        mFunctions = FirebaseFunctions.getInstance()
        functions = FirebaseFunctions.getInstance()

        //for gallery
        val getImage = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            ActivityResultCallback {
                takeImage(it)

            }
        )


        doLogin()

        //button gallery
        binding.btnGallery.setOnClickListener {
            getImage.launch("image/")
        }

        //button camera
        binding.btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
            } else {
                dispatchTakePictureIntent()
            }

        }


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Storage Permission Required for Text Extraction", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private fun dispatchTakePictureIntent() {
        // inside this method we are calling an implicit intent to capture an image.
        var photoFile: File? = null
        photoFile = createImageFile()
        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(this, "$packageName.provider", photoFile)
            getCameraImage.launch(photoUri)
        }
    }

    private val getCameraImage = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.image.setImageURI(photoUri)
            processImage(photoUri)
        }
    }

    private fun createImageFile(): File? {
        var mImageUri: Uri? = null
        val timeStamp = Calendar.getInstance().timeInMillis.toString()
        val imageFileName = "JPEG_" + timeStamp + "_"
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        var image: File? = null

        try {
            image = File.createTempFile(imageFileName, ".jpg", path)
            mImageUri = Uri.fromFile(image)
        } catch (e: IOException) {
            e.printStackTrace()
        }
//        Log.e("here", "Path:$mImageUri")
        return image
    }

    private fun takeImage(uri: Uri?) {
        binding.image.setImageURI(uri)
        try {
            val contentResolver = contentResolver
            var source: ImageDecoder.Source? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                source = ImageDecoder.createSource(contentResolver, uri!!)
                imageBitmap = ImageDecoder.decodeBitmap(source)
                processImage(uri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun processImage(uri: Uri) {
      // Scale down bitmap size

        var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        // Scale down bitmap size
        bitmap = scaleBitmapDown(bitmap, 640)
        // Convert bitmap to base64 encoded string
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
        val base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        // Create json request to cloud vision
        val request = JsonObject()
        // Add image to request
        val image = JsonObject()
        image.add("content", JsonPrimitive(base64encoded))
        request.add("image", image)
        //Add features to the request

        //Add features to the request
        val featureDocumentTextDetection = JsonObject()
        val featureLandmarkDetection = JsonObject()
        val featureFaceDetection = JsonObject()
        val featureObjectLocalised = JsonObject()
        val featureLogoDetection = JsonObject()
        val featureLabelDetection = JsonObject()
        val featureSafeSearchDetection = JsonObject()
        val featureImageProperties = JsonObject()
        val featureCropHints = JsonObject()


        //DOCUMENT_TEXT_DETECTION
        featureDocumentTextDetection.add("type", JsonPrimitive("DOCUMENT_TEXT_DETECTION"))


        //LANDMARK_DETECTION
        featureLandmarkDetection.add("type", JsonPrimitive("LANDMARK_DETECTION"))


        //FACE_DETECTION
        featureFaceDetection.add("type", JsonPrimitive("FACE_DETECTION"))


        //OBJECT_LOCALIZATION
        featureObjectLocalised.add("type", JsonPrimitive("OBJECT_LOCALIZATION"))


        //LOGO_DETECTION
        featureLogoDetection.add("type", JsonPrimitive("LOGO_DETECTION"))


        //LABEL_DETECTION
        featureLabelDetection.add("type", JsonPrimitive("LABEL_DETECTION"))


        //SAFE_SEARCH_DETECTION
        featureSafeSearchDetection.add("type", JsonPrimitive("SAFE_SEARCH_DETECTION"))


        //IMAGE_PROPERTIES
        featureImageProperties.add("type", JsonPrimitive("IMAGE_PROPERTIES"))


        //CROP_HINTS
        featureCropHints.add("type", JsonPrimitive("CROP_HINTS"))

        val features = JsonArray()
        features.add(featureDocumentTextDetection)
      /*  features.add(featureLandmarkDetection)
        features.add(featureFaceDetection)
        features.add(featureObjectLocalised)
        features.add(featureLogoDetection)
        features.add(featureLabelDetection)
        features.add(featureSafeSearchDetection)
        features.add(featureImageProperties)
        features.add(featureCropHints)*/
        request.add("features", features)

        val imageContext = JsonObject()
        val languageHints = JsonArray()
        languageHints.add("en")
        imageContext.add("languageHints", languageHints)
        request.add("imageContext", imageContext)

        annotateImage(request.toString())
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // Task failed with an exception
                    // ...
                    Log.e("TAG", "Error occurred")
                } else {
                    // Task completed successfully
                    // ...
                    val annotation = task.result!!.asJsonArray[0].asJsonObject["fullTextAnnotation"].asJsonObject
//                    System.out.format("%nComplete annotation:")
//                    System.out.format("%n%s", annotation["text"].asString)
                    binding.tvShow.text = annotation["text"].asString

                    Log.e("TAG", "text " + annotation["text"].asString)

                    getBlocks(annotation)
                }
            }
    }

    private fun getBlocks(annotation: JsonObject?) {

        for (page in annotation?.get("pages")?.asJsonArray!!) {
            var pageText = ""
            for (block in page.asJsonObject["blocks"].asJsonArray) {
                var blockText = ""
                for (para in block.asJsonObject["paragraphs"].asJsonArray) {
                    var paraText = ""
                    for (word in para.asJsonObject["words"].asJsonArray) {
                        var wordText = ""
                        for (symbol in word.asJsonObject["symbols"].asJsonArray) {

                            wordText += symbol.asJsonObject["text"].asString
//                            System.out.format("Symbol text: %s (confidence: %f)%n",
//                                symbol.asJsonObject["text"].asString, symbol.asJsonObject["confidence"].asFloat)
                        }
//                        System.out.format("Word text: %s (confidence: %f)%n%n", wordText,
//                            word.asJsonObject["confidence"].asFloat)
//                        System.out.format("Word bounding box: %s%n", word.asJsonObject["boundingBox"])
                        paraText = String.format("%s%s ", paraText, wordText)
                    }
//                    System.out.format("%nParagraph: %n%s%n", paraText)
//                    System.out.format("Paragraph bounding box: %s%n", para.asJsonObject["boundingBox"])
//                    System.out.format("Paragraph Confidence: %f%n", para.asJsonObject["confidence"].asFloat)
                    blockText += paraText
                }
                Log.e("blocktext", "" + blockText)
                pageText += blockText

            }
        }


    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth =
                (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight =
                (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }

    private fun doLogin() {
        mAuth.createUserWithEmailAndPassword("<USERNAME>", "<PASSWORD>")
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.e("TAG", "signupWithEmail:success");
                    val user = mAuth.currentUser
                } else {
                    // If sign in fails, display a message to the user.
                    Log.e("TAG", "signupWithEmail:failure", task.getException());
                    signInWithEmailAddress()
                }
            }
    }

    private fun annotateImage(requestJson: String): Task<JsonElement> {
        return functions
            .getHttpsCallable("annotateImage")
            .call(requestJson)
            .continueWith { task ->
                // This continuation runs on either success or failure, but if the task
                // has failed then result will throw an Exception which will be
                // propagated down.
                val result = task.result?.data
                JsonParser.parseString(Gson().toJson(result))
            }
    }

    private fun signInWithEmailAddress() {
        mAuth.signInWithEmailAndPassword("<USERNAME>", "PASSWORD")
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.e("TAG", "signInWithEmail:success");
                    val user = mAuth.currentUser
                } else {
                    // If sign in fails, display a message to the user.
                    Log.e("TAG", "signInWithEmail:failure", task.exception);
                }
            }
    }
}