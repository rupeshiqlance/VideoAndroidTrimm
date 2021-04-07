package com.example.videoandroidtrimm


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.DexterError
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    var RECORD_VIDIEO_REQUEST_CODE=100
    var GALLER_VIDIEO_REQUEST_CODE=101
    var strVideoPath=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnPickVideo.setOnClickListener {
            getVideoPrompt()
        }
    }

    /*
    * This method called for open prompt for gallery and video
    * */
    private fun getVideoPrompt() {
        val items = arrayOf<CharSequence>(
            "Take Video", "Choose from Gallery",
            "Cancel"
        )
        val builder =
            AlertDialog.Builder(this@MainActivity)
        builder.setItems(
            items
        ) { dialog: DialogInterface, item: Int ->
            if (items[item] == "Take Video") {
                requestStoragePermission(true)
            } else if (items[item] == "Choose from Gallery") {
                requestStoragePermission(false)
            } else if (items[item] == "Cancel") {
                dialog.dismiss()
            }
        }
        builder.show()
    }
    /*
    * This method called for check a permission for Access Internal storage and camera
    *
    * */
    private fun requestStoragePermission(isCamera: Boolean) {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    // check if all permissions are granted
                    if (report.areAllPermissionsGranted()) {
                        if (isCamera) {
                            recordVideoIntent()
                            Log.e("camera","true");
                        } else {
                            videoGalleryIntent()
                            Log.e("Gallery","true");
                        }
                    }
                    // check for permanent denial of any permission
                    if (report.isAnyPermissionPermanentlyDenied) {
                        // show alert dialog navigating to Settings
                        showSettingsDialog()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest?>?,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            })
            .withErrorListener { error: DexterError? ->
                Toast.makeText(applicationContext, "Error occurred! ", Toast.LENGTH_SHORT)
                    .show()
            }
            .onSameThread()
            .check()

    }

    /*
    * This method called for pick a video from gallery
    * */
    private fun videoGalleryIntent() {
        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        intent.action = Intent.ACTION_GET_CONTENT;
       /* intent.addCategory(Intent.CATEGORY_OPENABLE);*/
        intent.type = "video/*"
        intent.action = Intent.ACTION_PICK
        startActivityForResult(Intent.createChooser(intent, "Select Video"),GALLER_VIDIEO_REQUEST_CODE)
    }

    /*This method called for record video from camera*/
    private fun recordVideoIntent() {

        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 120)
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        startActivityForResult(
            intent,RECORD_VIDIEO_REQUEST_CODE
        )
    }

    /**
     * Showing Alert Dialog with Settings option
     * Navigates user to app settings
     * NOTE: Keep proper title and message depending on your app
     */
    private fun showSettingsDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Need Permissions")
        builder.setMessage(
            "This app needs permission to use this feature. You can grant them in app settings."
        )
        builder.setPositiveButton("GOTO SETTINGS", { dialog, which ->
            dialog.cancel()
            openSettings()
        })
        builder.setNegativeButton("Cancel", { dialog, which -> dialog.cancel() })
        builder.show()
    }

    // navigating user to app settings
    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, 102)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == 102){
                getVideoPrompt()
            }else if(requestCode == RECORD_VIDIEO_REQUEST_CODE){
                strVideoPath = getPath(data!!.data!!,this)!!
                var mFile = File(strVideoPath)
                // Get length of file in bytes
                val fileSizeInBytes: Long = mFile.length()
                // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
                val fileSizeInKB: Long = fileSizeInBytes / 1024
                //  Convert the KB to MegaBytes (1 MB = 1024 KBytes)
                //  Convert the KB to MegaBytes (1 MB = 1024 KBytes)
                val fileSizeInMB = fileSizeInKB / 1024
                var mKBSize = mFile.length()
                var mMbSize = mKBSize/1024
                Log.i("Video side KB::",fileSizeInKB.toString())
                Log.i("Video side MB ::",fileSizeInMB.toString())
                Log.i("Video storage path ::",strVideoPath)
            }else if(requestCode == GALLER_VIDIEO_REQUEST_CODE){
                strVideoPath = getPath(data!!.data!!,this)!!
                var mFile = File(strVideoPath)
                val fileSizeInBytes: Long = mFile.length()
                val fileSizeInKB: Long = fileSizeInBytes / 1024
                val fileSizeInMB = fileSizeInKB / 1024
                Log.i("Video side KB::",fileSizeInKB.toString())
                Log.i("Video side MB ::",fileSizeInMB.toString())
                Log.i("Video storage path ::",strVideoPath)
                val mp: MediaPlayer = MediaPlayer.create(this, Uri.parse(strVideoPath))
                val durationVdeo = mp.duration
                /*val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this,Uri.parse(strVideoPath))
                val time: String? =retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val timeInMillisec = time!!.toLong()
                retriever.release()
                val duration = convertMillieToHMmSs(timeInMillisec)*/
                Log.e(
                    "VIdeoDuration::",
                    String.format("%d",TimeUnit.MILLISECONDS.toSeconds(durationVdeo.toLong()))
                )
                if (String.format("%d",TimeUnit.MILLISECONDS.toSeconds(durationVdeo.toLong())).toInt() > 60
                ) {
                    Toast.makeText(applicationContext, "Video should be max 2 minute", Toast.LENGTH_SHORT)
                        .show()
                }else{
                    ivThumbnail.setImageBitmap(retriveVideoFrameFromVideo(strVideoPath))
                    val outputDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .absolutePath
                    val destPath =
                        outputDir + File.separator + "VID_" + SimpleDateFormat(
                            "yyyyMMdd_HHmmss",
                            getLocale()
                        ).format(Date()) + ".mp4"
                    var strCompressedeVideoPath = compressVideo(strVideoPath,destPath);
                    var mFiles = File(strCompressedeVideoPath)
                    val filesSizeInBytes: Long = mFiles.length()
                    val filesSizeInKB: Long = filesSizeInBytes / 1024
                    val filesSizeInMB = filesSizeInKB / 1024
                    Log.e("Video side KB::",filesSizeInKB.toString())
                    Log.e("Video side MB ::",filesSizeInMB.toString())
                    Log.e("Compress VideoPath=>>",strCompressedeVideoPath)
                }
            }
        }
    }
    fun convertMillieToHMmSs(millie: Long): String? {
        val seconds = millie / 1000
        val second = seconds % 60
        val minute = seconds / 60 % 60
        val hour = seconds / (60 * 60) % 24
        val result = ""
        return if (hour > 0) {
            String.format("%02d:%02d:%02d", hour, minute, second)
        } else {
            String.format("%02d:%02d", minute, second)
        }
    }
    @SuppressLint("NewApi")
    fun getPath(uri: Uri,context:Context): String? {
        // check here to KITKAT or new version
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        var selection: String? = null
        var selectionArgs: Array<String>? = null
        // DocumentProvider
        if (isKitKat) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                val fullPath = getPathFromExtSD(split)
                return if (fullPath !== "") {
                    fullPath
                } else {
                    null
                }
            }


            // DownloadsProvider
            if (isDownloadsDocument(uri)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val id: String
                    var cursor: Cursor? = null
                    try {
                        cursor = context.getContentResolver().query(
                            uri,
                            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                            null,
                            null,
                            null
                        )
                        if (cursor != null && cursor.moveToFirst()) {
                            val fileName: String = cursor.getString(0)
                            val path =
                                Environment.getExternalStorageDirectory()
                                    .toString() + "/Download/" + fileName
                            if (!TextUtils.isEmpty(path)) {
                                return path
                            }
                        }
                    } finally {
                        if (cursor != null) cursor.close()
                    }
                    id = DocumentsContract.getDocumentId(uri)
                    if (!TextUtils.isEmpty(id)) {
                        if (id.startsWith("raw:")) {
                            return id.replaceFirst("raw:".toRegex(), "")
                        }
                        val contentUriPrefixesToTry =
                            arrayOf(
                                "content://downloads/public_downloads",
                                "content://downloads/my_downloads"
                            )
                        for (contentUriPrefix in contentUriPrefixesToTry) {
                            return try {
                                val contentUri = ContentUris.withAppendedId(
                                    Uri.parse(contentUriPrefix),
                                    java.lang.Long.valueOf(id)
                                )
                                getDataColumn(context, contentUri, null, null)
                            } catch (e: NumberFormatException) {
                                //In Android 8 and Android P the id is not a number
                                uri.path!!.replaceFirst("^/document/raw:".toRegex(), "")
                                    .replaceFirst("^raw:".toRegex(), "")
                            }
                        }
                    }
                } else {
                    var contentUri : Uri?= null
                    val id = DocumentsContract.getDocumentId(uri)
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:".toRegex(), "")
                    }
                    try {
                        contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"),
                            java.lang.Long.valueOf(id)
                        )
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                    if (contentUri != null) {
                        return getDataColumn(context, contentUri, null, null)
                    }
                }
            }


            // MediaProvider
            if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                selection = "_id=?"
                selectionArgs = arrayOf(split[1])
                return getDataColumn(
                    context, contentUri, selection,
                    selectionArgs
                )
            }
            if (isGoogleDriveUri(uri)) {
                return getDriveFilePath(uri,context)
            }
            if (isWhatsAppFile(uri)) {
                return getFilePathForWhatsApp(uri,context)
            }
            if ("content".equals(uri.scheme, ignoreCase = true)) {
                if (isGooglePhotosUri(uri)) {
                    return uri.lastPathSegment
                }
                if (isGoogleDriveUri(uri)) {
                    return getDriveFilePath(uri,context)
                }
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    // return getFilePathFromURI(context,uri);
                    copyFileToInternalStorage(uri, "userfiles",context)
                    // return getRealPathFromURI(context,uri);
                } else {
                    getDataColumn(context, uri, null, null)
                }
            }
            if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }
        } else {
            if (isWhatsAppFile(uri)) {
                return getFilePathForWhatsApp(uri,context)
            }
            if ("content".equals(uri.scheme, ignoreCase = true)) {
                val projection = arrayOf(
                    MediaStore.Images.Media.DATA
                )
                var cursor: Cursor? = null
                try {
                    cursor = context.getContentResolver()
                        .query(uri, projection, selection, selectionArgs, null)
                    val column_index: Int =
                        cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    if (cursor.moveToFirst()) {
                        return cursor.getString(column_index)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    private fun fileExists(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists()
    }

    private fun getPathFromExtSD(pathData: Array<String>): String {
        val type = pathData[0]
        val relativePath = "/" + pathData[1]
        var fullPath = ""

        // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
        // something like "71F8-2C0A", some kind of unique id per storage
        // don't know any API that can get the root path of that storage based on its id.
        //
        // so no "primary" type, but let the check here for other devices
        if ("primary".equals(type, ignoreCase = true)) {
            fullPath =
                Environment.getExternalStorageDirectory().toString() + relativePath
            if (fileExists(fullPath)) {
                return fullPath
            }
        }

        // Environment.isExternalStorageRemovable() is `true` for external and internal storage
        // so we cannot relay on it.
        //
        // instead, for each possible path, check if file exists
        // we'll start with secondary storage as this could be our (physically) removable sd card
        fullPath = System.getenv("SECONDARY_STORAGE") + relativePath
        if (fileExists(fullPath)) {
            return fullPath
        }
        fullPath = System.getenv("EXTERNAL_STORAGE") + relativePath
        return if (fileExists(fullPath)) {
            fullPath
        } else fullPath
    }

    private fun getDriveFilePath(uri: Uri,context:Context): String? {
        val returnCursor: Cursor? =
            context.getContentResolver().query(uri, null, null, null, null)
        /*
     * Get the column indexes of the data in the Cursor,
     *     * move to the first row in the Cursor, get the data,
     *     * and display it.
     * */
        val nameIndex: Int = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex: Int = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name: String = returnCursor.getString(nameIndex)
        val size = java.lang.Long.toString(returnCursor.getLong(sizeIndex))
        val file = File(context.getCacheDir(), name)
        try {
            val inputStream: InputStream? = context.getContentResolver().openInputStream(uri)
            val outputStream = FileOutputStream(file)
            var read = 0
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable: Int = inputStream!!.available()

            //int bufferSize = 1024;
            val bufferSize = Math.min(bytesAvailable, maxBufferSize)
            val buffers = ByteArray(bufferSize)
            while (inputStream.read(buffers).also({ read = it }) != -1) {
                outputStream.write(buffers, 0, read)
            }
            Log.e("File Size", "Size " + file.length())
            inputStream.close()
            outputStream.close()
            Log.e("File Path", "Path " + file.path)
            Log.e("File Size", "Size " + file.length())
        } catch (e: Exception) {
            Log.e("Exception", e.message!!)
        }
        return file.path
    }

    /***
     * Used for Android Q+
     * @param uri
     * @param newDirName if you want to create a directory, you can set this variable
     * @return
     */
    @SuppressLint("Recycle")
    private fun copyFileToInternalStorage(
        uri: Uri,
        newDirName: String,context: Context
    ): String? {
        val returnCursor: Cursor? = context.contentResolver.query(
            uri, arrayOf<String>(
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
            ), null, null, null
        )


        /*
     * Get the column indexes of the data in the Cursor,
     *     * move to the first row in the Cursor, get the data,
     *     * and display it.
     * */
        val nameIndex: Int = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex: Int = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name: String = returnCursor.getString(nameIndex)
        val size = java.lang.Long.toString(returnCursor.getLong(sizeIndex))
        val output: File
        if (newDirName != "") {
            val dir =
                File(context.getFilesDir().toString() + "/" + newDirName)
            if (!dir.exists()) {
                dir.mkdir()
            }
            output = File(context.getFilesDir().toString() + "/" + newDirName + "/" + name)
        } else {
            output = File(context.getFilesDir().toString() + "/" + name)
        }
        try {
            val inputStream: InputStream? = context.getContentResolver().openInputStream(uri)
            val outputStream = FileOutputStream(output)
            var read = 0
            val bufferSize = 1024
            val buffers = ByteArray(bufferSize)
            while (inputStream!!.read(buffers).also({ read = it }) != -1) {
                outputStream.write(buffers, 0, read)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            Log.e("Exception", e.message!!)
        }
        return output.path
    }

    private fun getFilePathForWhatsApp(uri: Uri,context: Context): String? {
        return copyFileToInternalStorage(uri, "whatsapp",context)
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(
                uri!!, projection,
                selection, selectionArgs, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val index: Int = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            if (cursor != null) cursor.close()
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    fun isWhatsAppFile(uri: Uri): Boolean {
        return "com.whatsapp.provider.media" == uri.authority
    }

    private fun isGoogleDriveUri(uri: Uri): Boolean {
        return "com.google.android.apps.docs.storage" == uri.authority || "com.google.android.apps.docs.storage.legacy" == uri.authority
    }

    /*This method called for video Thumbnail*/
    @Throws(Throwable::class)
    open fun retriveVideoFrameFromVideo(videoPath: String?): Bitmap? {
        val thumb = ThumbnailUtils.createVideoThumbnail(
            videoPath!!,
            MediaStore.Images.Thumbnails.MINI_KIND
        )
        return thumb
    }
    private fun getLocale(): Locale? {
        val config = resources.configuration
        var sysLocale: Locale? = null
        sysLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getSystemLocale(config)
        } else {
            getSystemLocaleLegacy(config)
        }
        return sysLocale
    }

    fun getSystemLocaleLegacy(config: Configuration): Locale? {
        return config.locale
    }

    @TargetApi(Build.VERSION_CODES.N)
    fun getSystemLocale(config: Configuration): Locale? {
        return config.locales[0]
    }
    open fun compressVideo(path:String,destinationPath:String):String{
        VideoCompressor.start(
            path,
            destinationPath,
            object : CompressionListener {
                override fun onProgress(percent: Float) {
                    // Update UI with progress value
                    runOnUiThread {
                        // update a text view
                        tvProgress.text = "${percent.toLong()+1}%"
                        // update a progress bar
                        //progressBar.progress = percent.toInt()
                    }
                }

                override fun onStart() {
                    // Compression start
                }

                override fun onSuccess() {
                    // On Compression success
                }

                override fun onFailure(failureMessage: String) {
                    // On Failure
                }

                override fun onCancelled() {
                    // On Cancelled
                }

            }, VideoQuality.HIGH, isMinBitRateEnabled = false, keepOriginalResolution = false)
        return  destinationPath
    }


}