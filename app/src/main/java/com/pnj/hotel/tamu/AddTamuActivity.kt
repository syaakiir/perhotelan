package com.pnj.hotel.tamu

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.pnj.hotel.MainActivity
import com.pnj.hotel.databinding.ActivityAddTamuBinding
import java.io.ByteArrayOutputStream
import java.util.Calendar

class AddTamuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTamuBinding
    private val firestoreDatabase = FirebaseFirestore.getInstance()

    private val REQ_CAM = 101
    private lateinit var imgUrl : Uri
    private var dataGambar: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddTamuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.TxtAddTglLahir.setOnClickListener{
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(this,
                DatePickerDialog.OnDateSetListener{ view, year, monthOfYear, dayOfMonth ->
                    binding.TxtAddTglLahir.setText(""+year+"-"+month+"-"+dayOfMonth)
                }, year, month, day)

            dpd.show()
        }

        binding.BtnAddTamu.setOnClickListener{
            addTamu()
        }

        binding.BtnImgTamu.setOnClickListener {
            openCamera()
        }
    }

    fun addTamu() {
        var nik : String = binding.TxtAddNIK.text.toString()
        var nama : String = binding.TxtAddNama.text.toString()
        var tgl_lahir : String = binding.TxtAddTglLahir.text.toString()

        var jk : String = ""
        if(binding.RdnEditJKL.isChecked){
            jk = "Laki - Laki"
        }
        else if(binding.RdnEditJKP.isChecked){
            jk = "Perempuan"
        }




        val tamu: MutableMap<String, Any> = HashMap()
        tamu["nik"] = nik
        tamu["nama"] = nama
        tamu["tgl_lahir"] = tgl_lahir
        tamu["jenis_kelamin"] = jk



        if(dataGambar != null){
            uploadPictFirebase(dataGambar!!,"${nik}_${nama}")

            firestoreDatabase.collection("tamu").add(tamu)
                .addOnSuccessListener {
                    val intentMain = Intent(this,MainActivity::class.java)
                    startActivity(intentMain)
                }
        }
    }

    private fun openCamera(){
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            this.packageManager?.let {
                intent?.resolveActivity(it).also {
                    startActivityForResult(intent,REQ_CAM)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAM && resultCode == RESULT_OK){
            dataGambar = data?.extras?.get("data") as Bitmap
            binding.BtnImgTamu.setImageBitmap(dataGambar)
        }
    }

    private fun uploadPictFirebase(img_bitmap: Bitmap, file_name: String){
        val baos = ByteArrayOutputStream()
        val ref = FirebaseStorage.getInstance().reference.child("img_tamu/${file_name}.jpg")
        img_bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos )

        val img = baos.toByteArray()
        ref.putBytes(img)
            .addOnCompleteListener() {
                if(it.isSuccessful){
                    ref.downloadUrl.addOnCompleteListener { Task ->
                        Task.result.let { Uri ->
                            imgUrl = Uri
                            binding.BtnImgTamu.setImageBitmap(img_bitmap)
                        }
                    }
                }
            }
    }
}

