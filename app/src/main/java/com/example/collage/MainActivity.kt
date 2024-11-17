package com.example.collage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.storage
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Date

private const val TAG = "MAIN_ACTIVITY"

class MainActivity : AppCompatActivity() {

    private lateinit var imageButton1: ImageButton
    private lateinit var uploadImageFab: FloatingActionButton
    private lateinit var uploadProgressBar: ProgressBar
    private lateinit var mainView: View

    private var newPhotoPath: String? = null
    private var visibleImagePath: String? = null
    private var imageFilename: String? = null
    private var photoUri: Uri? = null

    private val NEW_PHOTO_PATH_KEY = "new photo path key"
    private val VISIBLE_IMAGE_PATH_KEY = "visible image path key"

    private val storage = Firebase.storage

    // result launcher cameraActivityLauncher - used to launch the camera app and receives the result
    private val cameraActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result -> handleImage(result)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        newPhotoPath = savedInstanceState?.getString(NEW_PHOTO_PATH_KEY)
        visibleImagePath = savedInstanceState?.getString(VISIBLE_IMAGE_PATH_KEY)

        mainView = findViewById(R.id.content)

        uploadProgressBar = findViewById(R.id.upload_progress_bar)
        uploadImageFab = findViewById(R.id.upload_image_button)

        uploadImageFab.setOnClickListener {
            uploadImage()
        }
        imageButton1 = findViewById(R.id.imageButton1)
        imageButton1.setOnClickListener {
            takePicture()
        }

    }

    private fun uploadImage() {
        if (photoUri != null && imageFilename != null) {

            uploadProgressBar.visibility = View.VISIBLE
            val imageStorageRootReference = storage.reference
            val imageCollectionReference = imageStorageRootReference.child("images")
            val imageFileReference = imageCollectionReference.child(imageFilename!!)
            imageFileReference.putFile(photoUri!!).addOnCompleteListener {
                Snackbar.make(mainView, "Image uploaded!", Snackbar.LENGTH_LONG).show()
                uploadProgressBar.visibility = View.GONE
            }
                .addOnFailureListener { error ->
                    Snackbar.make(mainView, "Error uploading image", Snackbar.LENGTH_LONG).show()
                    Log.e(TAG, "Error uploading image $imageFilename", error)
                }
        } else {
            Snackbar.make(mainView, "Take a picture first!", Snackbar.LENGTH_LONG).show()
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(NEW_PHOTO_PATH_KEY, newPhotoPath)
        outState.putString(VISIBLE_IMAGE_PATH_KEY, visibleImagePath)
    }

    private fun takePicture() {  // before the intent is created and launched, setup a file to write the image to, find the uri for the file, provide uri as an extra as an intent
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val (photoFile, photoFilePath) = createImageFile()
        if (photoFile != null) {
            newPhotoPath = photoFilePath
            photoUri = FileProvider.getUriForFile(
                this, "com.example.collage.fileprovider", photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraActivityLauncher.launch(takePictureIntent)
        }
    }

    private fun createImageFile(): Pair<File?, String?> {
        try {
            val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            imageFilename = "COLLAGE_$dateTime"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file= File.createTempFile(imageFilename!!, ".jpg", storageDir)
            val filePath = file.absolutePath
            return file to filePath

        } catch (ex: IOException) {
            return null to null

        }
    }

    // defines function handleImage that takes ActivityResult parameter
    private fun handleImage(result: ActivityResult) {
        when (result.resultCode) {
            RESULT_OK -> {
                Log.d(TAG, "Result ok, user took picture image at $newPhotoPath")
                visibleImagePath = newPhotoPath
            }
            RESULT_CANCELED -> {
                Log.d(TAG, "Result cancelled, no picture taken")
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "on window focus changed $hasFocus visible image at $visibleImagePath")
        if (hasFocus) {
            visibleImagePath?.let { imagePath ->
                loadImage(imageButton1,imagePath) }
            }
        }

    private fun loadImage(imageButton: ImageButton, imagePath: String) {
        Picasso.get()
            .load(File(imagePath))
            .error(android.R.drawable.stat_notify_error)
            .fit()
            .centerCrop()
            .into(imageButton, object: Callback {
                override fun onSuccess() {
                    Log.d(TAG, "Loaded image $imagePath")
                }

                override fun onError(e: Exception?) {
                    Log.e(TAG, "Error loading image $imagePath", e)
                }
            })
    }
}