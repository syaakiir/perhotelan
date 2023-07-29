package com.pnj.hotel.tamu

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.pnj.hotel.MainActivity
import com.pnj.hotel.databinding.ActivityEditTamuBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class EditTamuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditTamuBinding
    private val db = FirebaseFirestore.getInstance()

    private val REQ_CAM = 101
    private lateinit var imgUrl : Uri
    private var dataGambar: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditTamuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val (year, month, day, curr_tamu) = setDefaultValue()

        binding.TxtEditTglLahir.setOnClickListener {
            val dpd = DatePickerDialog(this,
                DatePickerDialog.OnDateSetListener{view, year, monthOfYear, dayOfMonth ->
                    binding.TxtEditTglLahir.setText(
                        ""+year+"-"+(monthOfYear+1)+"-"+dayOfMonth)
                }, year.toString().toInt(), month.toString().toInt(), day.toString().toInt()
            )
            dpd.show()
        }

        binding.BtnEditTamu.setOnClickListener {
            val new_data_tamu = newTamu()
            updateTamu(curr_tamu as Tamu, new_data_tamu)

            val intentMain = Intent(this,MainActivity::class.java)
            startActivity(intentMain)
            finish()
        }

        showFoto()

        binding.BtnImgTamu.setOnClickListener {
            openCamera()
        }
    }

    fun setDefaultValue(): Array<Any>{
        val intent = intent
        val nik = intent.getStringExtra("nik").toString()
        val nama = intent.getStringExtra("nama").toString()
        val tgl_lahir = intent.getStringExtra("tgl_lahir").toString()
        val jenis_kelamin = intent.getStringExtra("jenis_kelamin").toString()


        binding.TxtEditNIK.setText(nik)
        binding.TxtEditNama.setText(nama)
        binding.TxtEditTglLahir.setText(tgl_lahir)

        val tgl_split = intent.getStringExtra("tgl_lahir")
            .toString().split("-").toTypedArray()
        val year = tgl_split[0].toInt()
        val month = tgl_split[1].toInt() - 1
        val day = tgl_split[2].toInt()
        if (jenis_kelamin == "Laki - Laki"){
            binding.RdnEditJKL.isChecked = true
        } else if (jenis_kelamin == "Perempuan") {
            binding.RdnEditJKP.isChecked = true
        }
        val curr_tamu = Tamu(nik, nama, tgl_lahir, jenis_kelamin,)
        return arrayOf(year, month, day, curr_tamu)
    }

    fun newTamu():Map<String, Any>{
        var nik : String = binding.TxtEditNIK.text.toString()
        var nama : String = binding.TxtEditNama.text.toString()
        var tgl_lahir : String = binding.TxtEditTglLahir.text.toString()

        var jk : String = ""
        if(binding.RdnEditJKL.isChecked){
            jk= "Laki - Laki"
        }else if(binding.RdnEditJKP.isChecked) {
            jk = "Perempuan"
        }

        if(dataGambar != null){
            uploadPictFirebase(dataGambar!!,"${nik}_${nama}")
        }


        val tamu = mutableMapOf<String, Any>()
        tamu["nik"]=nik
        tamu["nama"]=nama
        tamu["tgl_lahir"]=tgl_lahir
        tamu["jenis_kelamin"]=jk


        return tamu
    }

    private fun updateTamu(tamu: Tamu, newTamuMap: Map<String, Any>) =
        CoroutineScope(Dispatchers.IO).launch {
            val personQuery = db.collection("tamu")
                .whereEqualTo("nik",tamu.nik)
                .whereEqualTo("nama",tamu.nama)
                .whereEqualTo("jenis_kelamin",tamu.jenis_kelamin)
                .whereEqualTo("tgl_lahir",tamu.tgl_lahir)
                .get()
                .await()
            if (personQuery.documents.isNotEmpty()){
                for (document in personQuery){
                    try {
                        db.collection("tamu").document(document.id).set(
                            newTamuMap,
                            SetOptions.merge()
                        )
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@EditTamuActivity,
                                e.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditTamuActivity,
                        "No persons matched the query.",Toast.LENGTH_LONG).show()
                }
            }
        }

    fun showFoto(){
        val intent = intent
        val nik = intent.getStringExtra("nik").toString()
        val nama = intent.getStringExtra("nama").toString()

        val storageRef = FirebaseStorage.getInstance().reference.child("img_tamu/${nik}_${nama}.jpg")
        val localfile = File.createTempFile("tempImage","jpg")
        storageRef.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding.BtnImgTamu.setImageBitmap(bitmap)
        }.addOnFailureListener {
            Log.e("foto ?","gagal")
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

