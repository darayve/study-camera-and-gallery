package com.darayve.usingcamera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.darayve.usingcamera.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    val REQUEST_IMAGE_CAPTURE = 100
    val REQUEST_GALLERY_DOCUMENT = 200
    val REQUEST_READ_AND_WRITE = 300

    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher =
        registerForActivityResult( // Abrir dialog pedindo permissão
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) dispatchTakePictureIntent()
        }

    private val requestPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                binding.button.text = it.key
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.button.setOnClickListener {
            showChooseDialog(this)
        }
    }

    private fun hasReadPermission() =
        ActivityCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasWritePermission() =
        ActivityCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        var permissionsToRequest = mutableListOf<String>()

        if (!hasReadPermission()) permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!hasWritePermission()) permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permissionsToRequest.isNotEmpty()) {
            Log.d("tag", permissionsToRequest.toTypedArray().toString())
            explainPermissionDialog(this@MainActivity, "Agora nós solicitaremos acesso à sua galeria.") {
                requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
            }
        } else {
            dispatchGalleryIntent()
        }
    }

    private fun requestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                dispatchTakePictureIntent()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> {
                // Additional rationale should be displayed
                explainPermissionDialog(
                    this@MainActivity,
                    "Agora nós pediremos permissão para acessar sua câmera."
                ) { requestPermissionLauncher.launch(Manifest.permission.CAMERA) }
            }

            else -> {
                // Permission has not been asked yet
                explainPermissionDialog(
                    this@MainActivity,
                    "Agora nós pediremos permissão para acessar sua câmera."
                ) { requestPermissionLauncher.launch(Manifest.permission.CAMERA) }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when {
                requestCode == REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    binding.imageView.setImageBitmap(imageBitmap)
                }

                requestCode == REQUEST_GALLERY_DOCUMENT -> {
                    val selectedImage = data?.data
                    binding.imageView.setImageURI(selectedImage)
                }

            }
        }
    }

    private fun showChooseDialog(context: Context) {
        val options = arrayOf("Tirar uma foto", "Escolher uma foto")
        var itemIndex = 0
        val dialog = AlertDialog.Builder(context).apply {
            setTitle("Selecionar")
            setSingleChoiceItems(options, 0) { _, i ->
                itemIndex = i
            }
            setPositiveButton("OK") { _, _ ->
                if (itemIndex == 0) requestPermission()
                else if (itemIndex == 1) requestPermissions()
            }
            setNegativeButton("VOLTAR") { _, _ ->

            }
            setCancelable(false)
        }
        dialog.show()
    }

    private fun explainPermissionDialog(
        context: Context,
        message: String,
        launcher: () -> Unit
    ) {
        val dialog = AlertDialog.Builder(context).apply {
            setMessage(message)
            setPositiveButton("OK") { _, _ ->
                launcher()
            }
            setCancelable(false)
        }
        dialog.show()
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun dispatchGalleryIntent() {
        Intent(
            Intent.ACTION_OPEN_DOCUMENT,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ).also { galleryIntent ->
            galleryIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(galleryIntent, REQUEST_GALLERY_DOCUMENT)
            }
        }
    }
}